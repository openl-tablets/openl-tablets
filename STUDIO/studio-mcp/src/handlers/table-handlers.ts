/**
 * Rules/tables tool handlers — list/get tables and update/append/create them.
 * Owns the shape a table is read and written in (`table-grid.ts`) for these tools;
 * the post-edit tail they share lives in `table-edit.ts`.
 */
import { ProtocolError, ProtocolErrorCode } from "@modelcontextprotocol/server";
import type { OpenLClient } from "../client.js";
import * as schemas from "../schemas.js";
import { findRows, gridOf, leadingRows, sourceOf, widthOf } from "../table-grid.js";
import { isPlainObject, sanitizeError } from "../utils.js";
import { formatResult, registerTool, textResult, type ToolResponse, TABLE_ID_ALIAS } from "./common.js";
import { cellText, formatStudioPage } from "../markdown-list.js";
import type * as Types from "../types.js";
import { finishTableEdit } from "./table-edit.js";

/**
 * Refuse a whole-table replacement that would remove rows: a table read in slices and written back from one of them
 * would lose every row the slice left out. Answers the number of columns of the table, which the rows sent may leave
 * out at their end.
 */
async function checkWholeTable(
  client: OpenLClient,
  projectId: string,
  tableId: string,
  rows: number,
): Promise<number> {
  const current = await client.getTable(projectId, tableId, { offset: 0, limit: 1 });
  const currentRows = Math.max(current.source.length, current.totalRows ?? 0);
  if (rows < currentRows) {
    throw new ProtocolError(
      ProtocolErrorCode.InvalidParams,
      `Cannot replace table '${tableId}' with ${rows} row(s): the current table has ${currentRows} row(s). ` +
        "openl_update_table writes the whole table, so a slice would remove the rows it leaves out. Use the " +
        "delete_rows action of openl_edit_table to remove rows, or another of its actions for isolated edits. " +
        "Nothing was written.",
    );
  }
  return widthOf(current.pos);
}

/**
 * LLM clients frequently send a nested array (the rows of a table) as a JSON *string*; parse such a string back into a
 * value so validation sees the real array. A string that LOOKS like JSON but fails to parse is reported precisely;
 * any other string is left for the schema to reject.
 */
function coercePayloadJson(value: unknown, payloadArg: string, toolName: string): unknown {
  if (typeof value !== "string") return value;
  const trimmed = value.trim();
  if (!/^[[{]/.test(trimmed)) return value;
  try {
    return JSON.parse(trimmed);
  } catch (err) {
    throw new ProtocolError(
      ProtocolErrorCode.InvalidParams,
      `${toolName}: '${payloadArg}' was provided as a string that is not valid JSON ` +
        `(${sanitizeError(err)}). Pass '${payloadArg}' as a JSON array, not a string.`,
    );
  }
}

/**
 * A merged cell named as the merge action of openl_edit_table names it, which agents carry over, as the [row, column,
 * rowspan, colspan] a table takes, a span left out being 1; any other value as it is.
 */
function mergeTuple(merge: unknown): unknown {
  if (!isPlainObject(merge) || !("row" in merge && "column" in merge)) return merge;
  const { row, column, rowspan = 1, colspan = 1, ...other } = merge;
  return Object.keys(other).length === 0 ? [row, column, rowspan, colspan] : merge;
}

/**
 * The name and the validation hook of a tool that takes the rows of a table, which a client may send as a string, and
 * its merged cells, which it may name.
 */
function tableRowsTool(toolName: string): { name: string; validateArgs: (args: unknown) => unknown } {
  return {
    name: toolName,
    validateArgs: (args: unknown): unknown => {
      if (!isPlainObject(args)) return args;
      return {
        ...args,
        rows: coercePayloadJson(args.rows, "rows", toolName),
        ...(Array.isArray(args.merges) ? { merges: args.merges.map(mergeTuple) } : {}),
      };
    },
  };
}

/** The description of the shape a table is read and written in, which every table tool shares. */
const TABLE_SHAPE =
  "A table is 'rows', one value per column: a scalar, an array of scalars, or null for an empty cell or one a merge " +
  "covers; 'merges' lists merged cells as [row, column, rowspan, colspan], 'formulas' Excel formulas as [row, " +
  "column, '=B2*C2'] beside the values they compute. Positions count from 0 in the whole table, the header row and " +
  "the label column included.";

export function registerTableHandlers(): void {
  registerTool({
    name: "list_tables",
    compiled: true,
    title: "List Project Tables",
    description:
      "List the tables of a project by module, a table of id, name and kind, with the dimension properties that tell versions of one table apart; 50 a page, 200 at most, the first line naming the offset of the next page. Filter by kind, name, module, properties, header and text. The 'id' is the tableId the table tools take; it stays while the table is edited or moved, and a rename gives a new one, which the edit tools answer.",
    schema: schemas.listTablesSchema,
    aliases: { search: "name" },
    annotations: {
      readOnlyHint: true,
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const { projectId, ...filters } = args;
      const tablesPage = await client.listTablesPage(projectId, filters);
      // A table of the project the caller named does not repeat it; one of a dependency, which scope 'all' finds, does
      const ofDependency = (table: Types.TableMetadata): boolean => table.projectId !== undefined && table.projectId !== projectId;
      const ofDependencies = tablesPage.items.some(ofDependency);
      // Studio tells the dimension properties of a table only when it has other versions, which share its name
      const versioned = tablesPage.items.some((table) => table.dimensions !== undefined);
      return textResult(formatStudioPage(tablesPage, filters.offset, {
        noun: "tables",
        // The module of a table, which many tables share, is written once over them. Studio leaves it out of the
        // tables of the project when they are filtered by it
        groupBy: {
          header: "module",
          value: (table) => table.module ?? (ofDependency(table) ? undefined : filters.module),
        },
        columns: [
          { header: "id", value: (table) => table.id },
          { header: "name", value: (table) => table.name },
          { header: "kind", value: (table) => table.kind },
          ...(versioned ? [{
            header: "version",
            value: (table: Types.TableMetadata) =>
              Object.entries(table.dimensions ?? {}).map(([name, value]) => `${name}=${cellText(value)}`).join("; "),
          }] : []),
          ...(ofDependencies ? [{
            header: "project",
            value: (table: Types.TableMetadata) => (table.projectId === projectId ? "" : `${table.project} (${table.projectId})`),
          }] : []),
        ],
      }));
    },
  });

  registerTool({
    name: "get_table",
    compiled: true,
    title: "Get Table Structure & Data",
    description:
      `Read a table as JSON, in the shape openl_update_table and openl_create_project_table take. ${TABLE_SHAPE} 'notes' holds the Excel notes and, with styles=true, 'styles' the cell styles; a write keeps both. Compilation messages name cells by A1 address, 'pos' giving the first cell of the table. For a large table, find='900' reads only the rows holding a text, with the header rows, 'rows' then keyed by the position of each; offset/limit read a slice ('window' tells offset, totalRows and next_offset), and a read without a limit answers the first 50 rows of a table of more than 100, the others read on by offset or find, its positions those of the whole table, for reading and for openl_edit_table. The answer carries the 'version' the edit tools take. An id from before a rename still reads the table; a deleted table answers 410.`,
    schema: schemas.getTableSchema,
    aliases: TABLE_ID_ALIAS,
    annotations: {
      readOnlyHint: true,
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      // A read without a limit answers a small table whole and the first rows of a large one, which the agent reads
      // on by slices or by find: a lookup of 900 rows read for its layout would fill its context
      const unbounded = args.find === undefined && args.limit === undefined;
      const table = await client.getTable(args.projectId, args.tableId, unbounded ? { ...args, limit: WHOLE_TABLE_ROWS + 1 } : args);
      const offset = args.offset ?? 0;
      const { source, tableType: _, totalRows: total, ...about } = table;
      // Studio names the rows of the table only when the window leaves some out
      const totalRows = total ?? offset + source.length;
      const read = gridOf(source, offset, table.pos);
      const grid = unbounded && read.rows.length > WHOLE_TABLE_ROWS ? leadingRows(read, offset, FIRST_ROWS) : read;
      const text = args.find;
      if (text !== undefined) {
        // The header and the row of the titles of the columns tell what the rows found hold
        const headerRows = (table.headerHeight ?? 1) + 1;
        const found = findRows(grid, text, headerRows);
        const rows = Object.keys(found.rows).length;
        return formatResult({ ...about, ...found }, {
          responseMetadata: { found: { rows, totalRows } },
          // Too many rows found are cut to the first of them whole, as a slice is
          truncation: {
            items: rows,
            unit: "rows found",
            cut: (count) => ({ ...about, ...findRows(grid, text, headerRows, count) }),
            next: () => "narrow find to read the others",
          },
        });
      }

      return formatResult({ ...about, ...grid }, {
        truncation: {
          items: grid.rows.length,
          unit: "rows",
          cut: (count) => ({ ...about, ...leadingRows(grid, offset, count) }),
          next: (count) => `continue with offset=${offset + count}`,
          metadata: (count) => ({ window: rowWindow(offset, count, totalRows) }),
        },
      });
    },
  });

  registerTool({
    name: "delete_table",
    compiled: true,
    title: "Delete Table",
    description:
      "Delete an ENTIRE table from a project. The whole table area is cleared from the sheet regardless of table type, so the table no longer exists once the project is recompiled. To remove only a row or column WITHIN a table, use openl_edit_table instead. The studio does not auto-compile after the delete — run openl_project_status afterward to confirm the project still compiles (a dangling reference to the deleted table surfaces there). 'version' from openl_get_table is required: Studio deletes nothing when the table changed since.",
    schema: schemas.tableOnlySchema,
    aliases: TABLE_ID_ALIAS,
    annotations: {
      destructiveHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const projectId = args.projectId;
      const requestedId = args.tableId;
      await client.deleteTable(projectId, requestedId, args.version);

      return formatResult({ deleted: requestedId });
    },
  });

  registerTool({
    ...tableRowsTool("update_table"),
    compiled: true,
    title: "Replace Entire Table",
    description:
      `Replace a whole table with the rows, merges and formulas openl_get_table answered, changed, and its 'version'. ${TABLE_SHAPE} Rows are added, never removed: fewer rows than the table has are refused; remove rows with the delete_rows action of openl_edit_table, which also suits an isolated edit. A row may end before the last column; notes and styles are kept. The answer gives the current 'tableId' and the new 'version'.`,
    schema: schemas.updateTableSchema,
    aliases: TABLE_ID_ALIAS,
    annotations: {
      destructiveHint: true,
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const { projectId, tableId, version } = args;
      const width = await checkWholeTable(client, projectId, tableId, args.rows.length);

      // Studio answers the new id of a table the update renamed.
      const reportedId = await client.updateTable(projectId, tableId, { tableType: "RawSource", source: sourceOf(args, width) }, version);
      return finishTableEdit(client, args, reportedId, "update");
    },
  });

  registerTool({
    ...tableRowsTool("create_project_table"),
    compiled: true,
    title: "Create New Table",
    description:
      `Create a table in the module 'module', or in a new module at the .xlsx 'modulePath', from its rows, merges and formulas. ${TABLE_SHAPE} Build the rows as the OpenL guides (openl_get_guides) describe the table type, or as openl_get_table answers a table like it; the header row names the table as 'name' does. The answer gives the 'tableId' and 'version' of the table, and 'compiles: false' with its errors when it does not compile.`,
    schema: schemas.createProjectTableSchema,
    annotations: {
      destructiveHint: false,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const createdTable = await client.createProjectTable(args.projectId, {
        moduleName: args.module,
        modulePath: args.modulePath,
        sheetName: args.sheetName,
        table: { tableType: "RawSource", name: args.name, source: sourceOf(args) },
      });

      return finishTableEdit(client, { projectId: args.projectId, tableId: createdTable.id }, undefined, "creation");
    },
  });
}

/** The most rows a read without a limit answers of a table whole, and the rows it answers of a larger one. */
const WHOLE_TABLE_ROWS = 100;
const FIRST_ROWS = 50;

/**
 * The rows a table read answers: the first of them and how many, how many rows the table has, whether the answer
 * holds every one of them, and the row to read next while rows remain.
 */
function rowWindow(offset: number, rows: number, totalRows: number): Record<string, unknown> {
  return {
    offset,
    rows,
    totalRows,
    complete: offset === 0 && rows === totalRows,
    ...(offset + rows < totalRows ? { next_offset: offset + rows } : {}),
  };
}
