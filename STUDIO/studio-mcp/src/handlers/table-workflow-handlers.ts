/**
 * Higher-level table workflows backed by the Studio BETA APIs: regular table
 * execution, dependency discovery, table property discovery, and server-side
 * table copying.
 */
import { ProtocolError, ProtocolErrorCode } from "@modelcontextprotocol/server";
import * as schemas from "../schemas.js";

import { isRequestTimeout } from "../http.js";
import { registerTool, formatResult, type ToolResponse } from "./common.js";
import { waitForResult } from "./wait.js";
import type { OpenLClient } from "../client.js";

const RUN_RESULT_FIELDS =
  "tableId,tableName,executionTimeMs,result,errors(id,severity,summary)," +
  "parameters(name,description,lazy,parameterId,value)," +
  "contextParameters(name,description,lazy,parameterId,value)";
// Studio stores one table run per client and its result endpoint is keyed
// only by project. Overlap could replace or cancel another tool call's run.
const activeTableRunClients = new WeakSet<OpenLClient>();

function runTimeoutMessage(timeoutMs: number): string {
  return `Table execution did not finish within ${timeoutMs} ms. The pending Studio run was cancelled; retry with a larger timeoutMs if the table is expected to run longer.`;
}

export function registerTableWorkflowHandlers(): void {
  registerTool({
    name: "run_table",
    title: "Run Table",
    description:
      "Execute a regular (non-Test) table with caller-provided JSON and return its result. This is a single high-level operation: it starts Studio's asynchronous run and waits inside the tool call until the result is ready, emitting progress notifications instead of requiring agent-side polling. Pass inputJson as an object keyed by method parameter name, as { params, runtimeContext? } with params keyed by name or listed in order, or as an array of the argument values in order; for a method whose only parameter is an array, a top-level array is that parameter's value. Set withSchema only when the result/parameter JSON Schemas are needed because they can be large. Studio keeps one table run per client, so wait for an active call to finish before starting another. Use openl_start_project_tests and the test-result tools for Test tables. Cancelling the MCP call also cancels and clears the Studio run.",
    schema: schemas.runTableSchema,
    annotations: {
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
          fromModule: args.fromModule,
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
            fields: args.withSchema === true ? undefined : RUN_RESULT_FIELDS,
            ...request,
          }),
          {
            timeoutMs: Math.max(deadline - Date.now(), 0),
            progressMessage: "Table execution is still running…",
            timeoutMessage: runTimeoutMessage(timeoutMs),
          },
          extra,
        );
        return formatResult(result, args.response_format);
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
      const { response_format, projectId, ...query } = args;
      const graph = await client.getTableDependencies(projectId, query);
      return formatResult(graph, response_format, {
        dataType: "table_dependencies",
        // The query line leaves out the arguments not given.
        markdownContext: { scope: query.tableId ? "table neighborhood" : "whole project", ...query },
      });
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
      return formatResult(properties, args.response_format);
    },
  });

  registerTool({
    name: "copy_table",
    title: "Copy Table",
    description:
      "Copy a table inside the same project using Studio's server-side copy operation, preserving formatting, merged cells, comments, and complete table structure. Provide the destination module and new table name; optionally choose a sheet, create a new .xlsx module with modulePath, or replace the source properties. The copy remains in the working copy—use openl_save_project for Git-backed projects.",
    schema: schemas.copyTableSchema,
    annotations: {
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const copied = await client.copyTable(args.projectId, args.tableId, {
        moduleName: args.moduleName,
        modulePath: args.modulePath,
        name: args.name,
        properties: args.properties,
        sheetName: args.sheetName,
      });
      return formatResult({ ...copied, success: true, tableId: copied.id }, args.response_format);
    },
  });
}
