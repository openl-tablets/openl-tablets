/**
 * Higher-level table workflows backed by the Studio BETA APIs: regular table
 * execution, dependency discovery, table property discovery, and server-side
 * table copying.
 */
import { ProtocolError, ProtocolErrorCode } from "@modelcontextprotocol/server";
import * as schemas from "../schemas.js";

import { isRequestTimeout } from "../http.js";
import { registerTool, formatResult, textResult, type ToolResponse, TABLE_ID_ALIAS } from "./common.js";
import { cellText, fitLines, formatWholeList } from "../markdown-list.js";
import { formatCount, tableLabel } from "../text.js";
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

/** A field of a datatype: `name Type`, `name Type[]` for a collection. */
function fieldText({ name, type, collection }: NonNullable<Types.TableNodeView["fields"]>[number]): string {
  return `${name ?? ""} ${type ?? ""}${collection ? "[]" : ""}`.trim();
}

/** The values of a vocabulary: `values String, 3: CalendarYear, BenefitYear, Lifetime`, a preview marked as cut. */
function vocabularyText({ valueType, valueCount, valuesPreview, truncated }: NonNullable<Types.TableNodeView["vocabulary"]>): string {
  return `values ${[valueType, valueCount].filter((part) => part !== undefined).join(", ")}: ${cellText(valuesPreview)}${truncated ? ", …" : ""}`;
}

/**
 * A dependency graph as text, a table a line: `AreaFactor (Rules, QFVRFV3YHVHG7S4E) called by: Calc1, Calc2`, then
 * what a version, a datatype or a vocabulary adds. A table names its id once; the tables it calls and that call it are
 * named by their names, which the table tools take, and by their ids where the graph holds several of a name. JSON
 * spent two thirds of a whole-project graph on keys and on the ids of the tables named.
 */
function graphText(nodes: Types.TableNodeView[]): string {
  const byId = new Map(nodes.map((node) => [node.id, node]));
  const named = Map.groupBy(nodes, (node) => node.name);
  const nameOf = (id: string): string => {
    const name = byId.get(id)?.name;
    return name !== undefined && named.get(name)?.length === 1 ? name : id;
  };
  const lines = nodes.map((node) => {
    const parts = [
      node.dependencies?.length && `calls: ${node.dependencies.map(nameOf).join(", ")}`,
      node.dependents?.length && `called by: ${node.dependents.map(nameOf).join(", ")}`,
      node.dimensions && `dimensions: ${JSON.stringify(node.dimensions)}`,
      node.extends && `extends: ${nameOf(node.extends)}`,
      node.fields?.length && `fields: ${node.fields.map(fieldText).join(", ")}`,
      node.vocabulary && vocabularyText(node.vocabulary),
    ].filter(Boolean);
    return `${tableLabel(node.name ?? node.id, node.kind, node.id)}${parts.length ? ` ${parts.join("; ")}` : ""}`;
  });
  return fitLines({
    head: [`${formatCount(nodes.length, "table")}.`],
    lines,
    more: (left) => `… ${formatCount(left, "more table")}: narrow the graph by tableId, module or layer to read them.`,
  });
}

export function registerTableWorkflowHandlers(): void {
  registerTool({
    name: "run_table",
    compiled: true,
    title: "Run Table",
    description:
      "Run a table with a JSON input and answer its result; test tables run with openl_run_project_tests. For an input of datatypes, call it first with withSchema: true and inputJson {}: the schema of each parameter names its fields and the values of each vocabulary. Example: {\"tableId\":\"calculatePremium\",\"inputJson\":{\"age\":30,\"state\":\"CA\"}}.",
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
      "Get the dependency graph of a project, or around the table tableId, a table a line: name, kind and id, then the tables it calls and that call it, by name, or by id where several share it. A table version adds its dimension properties, a datatype its parent and fields, a vocabulary its value type, count and first and last values. A dispatcher stands for versioned tables.",
    schema: schemas.getTableDependenciesSchema,
    aliases: TABLE_ID_ALIAS,
    annotations: {
      readOnlyHint: true,
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const { projectId, ...query } = args;
      return textResult(graphText(await client.getTableDependencies(projectId, query)));
    },
  });

  registerTool({
    name: "list_table_property_definitions",
    title: "List Allowed Table Properties",
    description:
      "List the properties a table of the kind tableType may declare: their type, whether they take several values or are dimensional, their default and their allowed values.",
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
      "Copy a table within its project, with its formatting, merges and comments, under a new name into a module, or into a new module at modulePath. Example: {\"tableId\":\"AgeFactor\",\"module\":\"Rules\",\"name\":\"AgeFactorNY\",\"properties\":{\"state\":\"NY\"}}. Answers the 'tableId' and 'version' of the copy.",
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
