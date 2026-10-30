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
import { finishTableEdit } from "./table-edit.js";
import type { OpenLClient } from "../client.js";

// The result of a run, without the input the caller sent, which Studio echoes as parameters
const RUN_RESULT_FIELDS = "executionTimeMs,result,errors(id,severity,summary)";
// Studio stores one table run per client and its result endpoint is keyed
// only by project. Overlap could replace or cancel another tool call's run.
const activeTableRunClients = new WeakSet<OpenLClient>();

function runTimeoutMessage(timeoutMs: number): string {
  return `Table execution did not finish within ${timeoutMs} ms. The pending Studio run was cancelled; retry with a larger timeoutMs if the table is expected to run longer.`;
}

export function registerTableWorkflowHandlers(): void {
  registerTool({
    name: "run_table",
    compiled: true,
    title: "Run Table",
    description:
      "Execute a regular (non-Test) table with caller-provided JSON and return its result. The tool waits for the result inside the call. Pass inputJson as an object keyed by method parameter name, as { params, runtimeContext? } with params keyed by name or listed in order, or as an array of the argument values in order, each also as a JSON string; for a method whose only parameter is an array, a top-level array is that parameter's value. To build an input of datatypes, call it first with withSchema: true, and inputJson {} when no input is known yet: the answer gives the JSON Schema of each parameter, with its fields and the values each vocabulary allows. Studio keeps one table run per client, so wait for an active call to finish before starting another. Use openl_start_project_tests and the test-result tools for Test tables. Cancelling the MCP call also cancels and clears the Studio run.",
    schema: schemas.runTableSchema,
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
        return formatResult(result);
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
      "Get the table dependency graph as an adjacency list: without tableId the whole project, optionally by module and layer; with tableId the neighborhood of the table, by direction and depth. An executable node carries its signature, return type and dimension properties; a datatype its parent and field references; a vocabulary its value type, value count and first and last values. Dispatchers stand for versioned tables, self-loops for recursion, and cycles are listed. Markdown draws the calls as a Mermaid flowchart and the data model as a Mermaid ER diagram, where '+ N more' marks the values left out; detailed Markdown adds the metadata of every node.",
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
      return formatResult(properties);
    },
  });

  registerTool({
    name: "copy_table",
    compiled: true,
    title: "Copy Table",
    description:
      "Copy a table inside the same project using Studio's server-side copy operation, preserving formatting, merged cells, comments, and complete table structure. Provide the destination module and new table name; optionally choose a sheet, create a new .xlsx module with modulePath, or replace the source properties. The copy remains in the working copy—use openl_save_project for Git-backed projects. The answer gives the 'tableId' and 'version' of the copy, and 'compiles: false' with its errors when it does not compile.",
    schema: schemas.copyTableSchema,
    annotations: {
      destructiveHint: false,
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
      return finishTableEdit(client, { projectId: args.projectId, tableId: copied.id }, undefined, "copy");
    },
  });
}
