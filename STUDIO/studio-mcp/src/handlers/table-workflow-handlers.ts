/**
 * Higher-level table workflows backed by the Studio BETA APIs: regular table
 * execution, dependency discovery, module/sheet/property discovery, and
 * server-side table copying.
 */
import { ProtocolError, ProtocolErrorCode } from "@modelcontextprotocol/server";
import * as schemas from "../schemas.js";
import { formatResponse } from "../formatters.js";
import { isAxiosError, isResultNotReady } from "../utils.js";
import { registerTool, type ToolHandlerExtra, type ToolResponse } from "./common.js";
import type { OpenLClient } from "../client.js";
import type * as Types from "../types.js";

const RUN_DEFAULT_TIMEOUT_MS = 120_000;
const RUN_POLL_INITIAL_INTERVAL_MS = 250;
const RUN_POLL_MAX_INTERVAL_MS = 2_000;
const RUN_RESULT_FIELDS =
  "tableId,tableName,executionTimeMs,result,errors(id,severity,summary)," +
  "parameters(name,description,lazy,parameterId,value)," +
  "contextParameters(name,description,lazy,parameterId,value)";
// Studio stores one table run per HTTP session and its result endpoint is keyed
// only by project. Overlap could replace or cancel another tool call's run.
const activeTableRunClients = new WeakSet<OpenLClient>();

function isLegacyResultNotReady(error: unknown): boolean {
  if (!isAxiosError(error) || error.response?.status !== 409) {
    return false;
  }
  const data = error.response.data;
  const code = typeof data === "object" && data !== null && "code" in data
    ? (data as { code?: unknown }).code
    : undefined;
  return typeof code === "string" && (
    code.endsWith(".not.completed") || code.endsWith(".not-completed")
  );
}

function isRequestTimeout(error: unknown): boolean {
  return isAxiosError(error) && (error.code === "ECONNABORTED" || error.code === "ETIMEDOUT");
}

function makeAbortError(): Error {
  const error = new Error("run_table aborted");
  error.name = "AbortError";
  return error;
}

function makeRunTimeoutError(timeoutMs: number): ProtocolError {
  return new ProtocolError(
    ProtocolErrorCode.InvalidRequest,
    `Table execution did not finish within ${timeoutMs} ms. The pending Studio run was cancelled; retry with a larger timeoutMs if the table is expected to run longer.`,
  );
}

function remainingRunTime(deadline: number, timeoutMs: number): number {
  const remainingMs = deadline - Date.now();
  if (remainingMs <= 0) {
    throw makeRunTimeoutError(timeoutMs);
  }
  return remainingMs;
}

/** Abortable delay between result reads while Studio executes the table. */
function delay(ms: number, signal?: AbortSignal): Promise<void> {
  return new Promise((resolve, reject) => {
    if (signal?.aborted) {
      reject(makeAbortError());
      return;
    }
    const onAbort = (): void => {
      clearTimeout(timer);
      reject(makeAbortError());
    };
    const timer = setTimeout(() => {
      signal?.removeEventListener("abort", onAbort);
      resolve();
    }, ms);
    if (typeof (timer as { unref?: () => void }).unref === "function") {
      (timer as unknown as { unref: () => void }).unref();
    }
    signal?.addEventListener("abort", onAbort, { once: true });
  });
}

async function waitForRunResult(
  client: OpenLClient,
  projectId: string,
  withSchema: boolean,
  startedAt: number,
  deadline: number,
  timeoutMs: number,
  extra?: ToolHandlerExtra,
): Promise<Types.RunExecutionResult> {
  const progressToken = extra?._meta?.progressToken;
  let pollIntervalMs = RUN_POLL_INITIAL_INTERVAL_MS;

  for (;;) {
    if (extra?.signal.aborted) {
      throw makeAbortError();
    }
    if (Date.now() >= deadline) {
      throw makeRunTimeoutError(timeoutMs);
    }
    try {
      const result = await client.getTableRunResult(projectId, {
        fields: withSchema ? undefined : RUN_RESULT_FIELDS,
        signal: extra?.signal,
        timeoutMs: remainingRunTime(deadline, timeoutMs),
      });
      if (!isResultNotReady(result)) {
        return result;
      }
    } catch (error) {
      if (Date.now() >= deadline || isRequestTimeout(error)) {
        throw makeRunTimeoutError(timeoutMs);
      }
      if (!isLegacyResultNotReady(error)) {
        throw error;
      }
    }

    if (progressToken !== undefined && extra?.sendNotification) {
      void extra.sendNotification({
        method: "notifications/progress",
        params: {
          progressToken,
          progress: Math.round((Date.now() - startedAt) / 1000),
          message: "Table execution is still running…",
        },
      }).catch(() => { /* progress reporting is best-effort */ });
    }
    const remainingMs = deadline - Date.now();
    if (remainingMs <= 0) {
      continue;
    }
    await delay(Math.min(pollIntervalMs, remainingMs), extra?.signal);
    pollIntervalMs = Math.min(pollIntervalMs * 2, RUN_POLL_MAX_INTERVAL_MS);
  }
}

export function registerTableWorkflowHandlers(): void {
  registerTool({
    name: "run_table",
    category: "Rules & Tables",
    title: "Run Table",
    description:
      "Execute a regular (non-Test) table with caller-provided JSON and return its result. This is a single high-level operation: it starts Studio's asynchronous run and waits inside the tool call until the result is ready, emitting progress notifications instead of requiring agent-side polling. Pass inputJson as an object keyed by method parameter name or as { params: { parameterName: value }, runtimeContext? }; { params: [...] } is rejected because Studio silently runs it with null arguments. A top-level array is passed as the value of a single array-valued parameter, not as positional arguments. Set withSchema only when the result/parameter JSON Schemas are needed because they can be large. Studio permits one table run per HTTP session, so wait for an active call to finish before starting another. Use openl_start_project_tests and the test-result tools for Test tables. Cancelling the MCP call also cancels and clears the Studio run.",
    schema: schemas.runTableSchema,
    annotations: {
      openWorldHint: true,
    },
    handler: async (args, client, extra): Promise<ToolResponse> => {
      const format = args.response_format;
      if (extra?.signal.aborted) {
        throw makeAbortError();
      }
      if (activeTableRunClients.has(client)) {
        throw new ProtocolError(
          ProtocolErrorCode.InvalidRequest,
          "Another openl_run_table call is already active in this Studio session. Wait for it to finish or cancel it before starting another table run.",
        );
      }
      activeTableRunClients.add(client);
      const timeoutMs = args.timeoutMs ?? RUN_DEFAULT_TIMEOUT_MS;
      const startedAt = Date.now();
      const deadline = startedAt + timeoutMs;
      try {
        await client.startTableRun(args.projectId, args.tableId, args.inputJson, {
          fromModule: args.fromModule,
          // Do not abort an unsettled start request: Studio may register its
          // session-scoped task after an early cleanup DELETE. Wait for the POST
          // to settle, then observe cancellation and clear the registered run.
          timeoutMs: remainingRunTime(deadline, timeoutMs),
        });
        if (extra?.signal.aborted) {
          throw makeAbortError();
        }
        const result = await waitForRunResult(
          client,
          args.projectId,
          args.withSchema === true,
          startedAt,
          deadline,
          timeoutMs,
          extra,
        );
        return { content: [{ type: "text", text: formatResponse(result, format) }] };
      } catch (error) {
        // Studio may schedule the run before the start response is observed;
        // every failed workflow must clear it so a later run cannot inherit stale state.
        await client.cancelTableRun(args.projectId).catch(() => { /* cancellation cleanup is best-effort */ });
        throw isRequestTimeout(error) ? makeRunTimeoutError(timeoutMs) : error;
      } finally {
        activeTableRunClients.delete(client);
      }
    },
  });

  registerTool({
    name: "get_table_dependencies",
    category: "Rules & Tables",
    title: "Get Table Dependencies",
    description:
      "Get the table dependency graph as an adjacency list. Omit tableId for the whole project graph, optionally restricted by module and layer (executable, datatype, or all); provide tableId for its dependency/dependent neighborhood with optional direction and depth. Executable nodes include signatures, return types, and dimension properties; datatype nodes include inheritance and declared field references; vocabulary nodes include their value type, total value count, and a bounded first/last values preview. Dispatchers represent versioned executable tables, self-loops represent recursion or self-reference, and cycles are derived from dependency edges. JSON preserves the graph nodes directly. Markdown renders executable calls as a Mermaid flowchart and the data model, including declared fields, vocabulary value previews, and reference cardinalities, as a Mermaid ER diagram; vocabulary headers use Name<Type>, preview rows leave the redundant type column visually empty, and a + N more marker identifies a truncated middle. Inheritance is shown separately when present. Detailed Markdown adds per-node metadata, while concise Markdown stays textual.",
    schema: schemas.getTableDependenciesSchema,
    annotations: {
      readOnlyHint: true,
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const format = args.response_format;
      const graph = await client.getTableDependencies(args.projectId, {
        tableId: args.tableId,
        module: args.module,
        layer: args.layer,
        direction: args.direction,
        depth: args.depth,
      });
      return {
        content: [{
          type: "text",
          text: formatResponse(graph, format, {
            dataType: "table_dependencies",
            markdownContext: {
              scope: args.tableId ? "table neighborhood" : "whole project",
              ...(args.tableId ? { tableId: args.tableId } : {}),
              ...(args.module ? { module: args.module } : {}),
              ...(args.layer ? { layer: args.layer } : {}),
              ...(args.direction ? { direction: args.direction } : {}),
              ...(args.depth !== undefined ? { depth: args.depth } : {}),
            },
          }),
        }],
      };
    },
  });

  registerTool({
    name: "list_project_modules",
    category: "Project",
    title: "List Project Modules",
    description:
      "List the modules declared by a project, including module names, rules-root paths/patterns, and modules matched by a pattern. Use the returned name when creating/copying a table, selecting a module graph, or listing worksheets.",
    schema: schemas.listProjectModulesSchema,
    annotations: {
      readOnlyHint: true,
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const format = args.response_format;
      const modules = await client.listProjectModules(args.projectId);
      return { content: [{ type: "text", text: formatResponse(modules, format) }] };
    },
  });

  registerTool({
    name: "list_module_sheets",
    category: "Project",
    title: "List Module Worksheets",
    description:
      "List worksheet names in a project module. First call openl_list_project_modules to get the exact moduleName. Use a returned sheet name as the destination sheet when creating or copying a table.",
    schema: schemas.listModuleSheetsSchema,
    annotations: {
      readOnlyHint: true,
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const format = args.response_format;
      const sheets = await client.listModuleSheets(args.projectId, args.moduleName);
      return { content: [{ type: "text", text: formatResponse(sheets, format) }] };
    },
  });

  registerTool({
    name: "list_table_property_definitions",
    category: "Rules & Tables",
    title: "List Allowed Table Properties",
    description:
      "List the properties Studio allows in the requested table context, including value type, whether multiple values are accepted, and allowed enum values. Omit tableType for entries inside a Properties table; provide a public table kind for properties that may be declared on that kind.",
    schema: schemas.listTablePropertyDefinitionsSchema,
    annotations: {
      readOnlyHint: true,
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const format = args.response_format;
      const properties = await client.listTablePropertyDefinitions(args.projectId, args.tableType);
      return { content: [{ type: "text", text: formatResponse(properties, format) }] };
    },
  });

  registerTool({
    name: "copy_table",
    category: "Rules & Tables",
    title: "Copy Table",
    description:
      "Copy a table inside the same project using Studio's server-side copy operation, preserving formatting, merged cells, comments, and complete table structure. Provide the destination module and new table name; optionally choose a sheet, create a new .xlsx module with modulePath, or replace the source properties. The copy remains in the working copy—use openl_save_project for Git-backed projects.",
    schema: schemas.copyTableSchema,
    annotations: {
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const format = args.response_format;
      const copied = await client.copyTable(args.projectId, args.tableId, {
        moduleName: args.moduleName,
        modulePath: args.modulePath,
        name: args.name,
        properties: args.properties,
        sheetName: args.sheetName,
      });
      return {
        content: [{
          type: "text",
          text: formatResponse({ ...copied, success: true, tableId: copied.id }, format),
        }],
      };
    },
  });
}
