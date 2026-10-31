/**
 * Higher-level table workflows backed by the Studio BETA APIs: regular table
 * execution, dependency discovery, table property discovery, and server-side
 * table copying.
 */
import { ProtocolError, ProtocolErrorCode } from "@modelcontextprotocol/server";
import * as schemas from "../schemas.js";

import { isRequestTimeout } from "../http.js";
import { registerTool, formatResult, textResult, type ToolResponse, TABLE_ID_ALIAS } from "./common.js";
import { formatWholeList } from "../markdown-list.js";
import { waitForResult } from "./wait.js";
import { finishTableEdit } from "./table-edit.js";
import type { OpenLClient } from "../client.js";
import type * as Types from "../types.js";

// The result of a run, without the input the caller sent, which Studio echoes as parameters
const RUN_RESULT_FIELDS = "executionTimeMs,result,errors(id,severity,summary)";
// With the schema of each parameter, to build an input from; the schema of the result is left out, the result is there
const RUN_SCHEMA_FIELDS = `${RUN_RESULT_FIELDS},parameters(name,schema)`;
// Studio stores one table run per client and its result endpoint is keyed
// only by project. Overlap could replace or cancel another tool call's run.
const activeTableRunClients = new WeakSet<OpenLClient>();

function runTimeoutMessage(timeoutMs: number): string {
  return `Table execution did not finish within ${timeoutMs} ms. The pending Studio run was cancelled; retry with a larger timeoutMs if the table is expected to run longer.`;
}

/** The run result with the parameter schemas without their `$schema` dialect, which tells an agent nothing. */
function withoutSchemaDialects(result: Types.RunExecutionResult): Types.RunExecutionResult {
  return {
    ...result,
    parameters: result.parameters?.map(({ schema, ...parameter }) => {
      const { $schema, ...rest } = (schema ?? {}) as Record<string, unknown>;
      return schema ? { ...parameter, schema: rest } : parameter;
    }),
  };
}

export function registerTableWorkflowHandlers(): void {
  registerTool({
    name: "run_table",
    compiled: true,
    title: "Run Table",
    description:
      "Run a regular (non-Test) table with a JSON input and answer its result, waiting in this call. inputJson is an object keyed by parameter name; { params, runtimeContext? } with params by name or in order; or an array of the arguments in order; any of them also as a JSON string. For a method whose only parameter is an array, a top-level array is that parameter's value. To build an input of datatypes, call it first with withSchema: true and inputJson {}: the answer gives the JSON Schema of each parameter, with its fields and the values each vocabulary allows. Studio runs one table per client at a time; Test tables run with openl_start_project_tests. Cancelling the call cancels the run.",
    schema: schemas.runTableSchema,
    aliases: TABLE_ID_ALIAS,
    annotations: {
      destructiveHint: false,
      openWorldHint: true,
    },
    handler: async (args, client, extra): Promise<ToolResponse> => {
      if (extra?.signal.aborted) {
        throw new DOMException("run_table aborted", "AbortError");
      }
      if (activeTableRunClients.has(client)) {
        throw new ProtocolError(
          ProtocolErrorCode.InvalidRequest,
          "Another openl_run_table call is already active in this Studio session. Wait for it to finish or cancel it before starting another table run.",
        );
      }
      activeTableRunClients.add(client);
      const timeoutMs = args.timeoutMs;
      const deadline = Date.now() + timeoutMs;
      try {
        await client.startTableRun(args.projectId, args.tableId, args.inputJson, {
          module: args.module,
          // Do not abort an unsettled start request: Studio may register its
          // session-scoped task after an early cleanup DELETE. Wait for the POST
          // to settle, then observe cancellation and clear the registered run.
          timeoutMs,
        });
        if (extra?.signal.aborted) {
          throw new DOMException("run_table aborted", "AbortError");
        }
        const result = await waitForResult(
          (request) => client.getTableRunResult(args.projectId, {
            fields: args.withSchema === true ? RUN_SCHEMA_FIELDS : RUN_RESULT_FIELDS,
            ...request,
          }),
          {
            timeoutMs: Math.max(deadline - Date.now(), 0),
            progressMessage: "Table execution is still running…",
            timeoutMessage: runTimeoutMessage(timeoutMs),
          },
          extra,
        );
        return formatResult(args.withSchema === true ? withoutSchemaDialects(result) : result);
      } catch (error) {
        // Studio may schedule the run before the start response is observed;
        // every failed workflow must clear it so a later run cannot inherit stale state.
        await client.cancelTableRun(args.projectId).catch(() => { /* cancellation cleanup is best-effort */ });
        throw isRequestTimeout(error)
          ? new ProtocolError(ProtocolErrorCode.InvalidRequest, runTimeoutMessage(timeoutMs))
          : error;
      } finally {
        activeTableRunClients.delete(client);
      }
    },
  });

  registerTool({
    name: "get_table_dependencies",
    compiled: true,
    title: "Get Table Dependencies",
    description:
      "Get the table dependency graph as an adjacency list of nodes: id, name, kind, and the ids of the tables it calls (dependencies) and that call it (dependents). Without tableId the whole project, optionally by module and layer; with tableId the neighborhood of the table, by direction and depth. A version of a table names its dimension properties; a datatype its parent and fields; a vocabulary its value type, value count and first and last values. Dispatchers stand for versioned tables, self-loops for recursion. Read a table itself with openl_get_table.",
    schema: schemas.getTableDependenciesSchema,
    aliases: TABLE_ID_ALIAS,
    annotations: {
      readOnlyHint: true,
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const { projectId, ...query } = args;
      return formatResult(await client.getTableDependencies(projectId, query));
    },
  });

  registerTool({
    name: "list_table_property_definitions",
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
      const properties = await client.listTablePropertyDefinitions(args.projectId, args.tableType);
      return textResult(formatWholeList(properties, {
        noun: "properties", one: "property",
        columns: [
          { header: "name", value: (property) => property.name },
          { header: "type", value: (property) => property.type },
          { header: "multiple", value: (property) => property.multiple },
          { header: "dimensional", value: (property) => property.dimensional },
          { header: "default", value: (property) => property.defaultValue },
          // An enum value is named by its code: the label of a code tells nothing more, unless it differs
          { header: "values", value: (property) => (property.values ?? []).map(({ code, value }) =>
            (value === undefined || value === code ? code : `${code} (${value})`)) },
        ],
      }));
    },
  });

  registerTool({
    name: "copy_table",
    compiled: true,
    title: "Copy Table",
    description:
      "Copy a table inside the same project, keeping its formatting, merged cells and comments, into the destination module under the new name; optionally on a sheet, into a new .xlsx module by modulePath, or with new 'properties'. The copy stays in the working copy until openl_save_project. The answer gives the 'tableId' and 'version' of the copy, and 'compiles: false' with its errors when it does not compile.",
    schema: schemas.copyTableSchema,
    aliases: TABLE_ID_ALIAS,
    annotations: {
      destructiveHint: false,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const copied = await client.copyTable(args.projectId, args.tableId, {
        moduleName: args.module,
        modulePath: args.modulePath,
        name: args.name,
        properties: args.properties && Object.entries(args.properties).map(([name, value]) => ({ name, value })),
        sheetName: args.sheetName,
      });
      return finishTableEdit(client, { projectId: args.projectId, tableId: copied.id }, undefined, "copy");
    },
  });
}
