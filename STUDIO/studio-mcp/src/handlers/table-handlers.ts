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
import { tableLabel } from "../text.js";
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
  "A table is 'rows', one value per column: a scalar, an array of scalars, or null for an empty cell or one a merge covers; 'merges' lists [row, column, rowspan, colspan], 'formulas' Excel formulas as [row, column, '=B2*C2']. Positions count from 0, the header row and the label column included.";

/**
 * The tables of a page by name, a line per kind: `Rules: AgeFactor, AreaFactor (QFVRFV3YHVHG7S4E, state=CA)`. The table
 * tools take a name; its id follows a name several tables of the page share, with the dimension properties Studio
 * tells of the versions of a table, those the versions of the page differ in, and the project of a table of a
 * dependency, which scope 'all' finds. A Markdown row repeated the id and the kind of every table.
 */
function tableNames(page: Types.TableMetadata[], ofDependency: (table: Types.TableMetadata) => boolean): (tables: Types.TableMetadata[]) => string {
  const byName = Map.groupBy(page, (table) => table.name);
  const label = (table: Types.TableMetadata): string => {
    const others = (byName.get(table.name) ?? []).filter((other) => other !== table);
    if (others.length === 0 && table.dimensions === undefined && !ofDependency(table)) {
      return tableLabel(table.name ?? table.id);
    }
    // Only one version on the page has nothing to differ from: all its properties tell it apart
    const differs = (key: string, value: unknown): boolean =>
      others.length === 0 || others.some((other) => JSON.stringify(other.dimensions?.[key]) !== JSON.stringify(value));
    const dimensions = Object.entries(table.dimensions ?? {}).filter(([key, value]) => differs(key, value))
      .map(([key, value]) => `${key}=${cellText(value)}`).join("; ");
    return tableLabel(table.name ?? table.id, table.id, dimensions, ofDependency(table) ? `project ${table.project} ${table.projectId}` : undefined);
  };
  const kindLine = ([kind, tables]: [string | undefined, Types.TableMetadata[]]): string =>
    `${kind ? `${kind}: ` : ""}${tables.map(label).join(", ")}`;
  return (tables) => Array.from(Map.groupBy(tables, (table) => table.kind), kindLine).join("\n");
}

export function registerTableHandlers(): void {
  registerTool({
    name: "list_tables",
    compiled: true,
    title: "List Project Tables",
    description:
      "List the tables of a project by name, a line per module and kind; the first line names the next page's offset. A name several tables share, such as the versions of a table, is followed by the id of each and the properties they differ in: the table tools take that id instead of the name. A rename gives a table a new id, which the edit tools answer.",
    schema: schemas.listTablesSchema,
    // The filter was named 'properties', which agents read as the properties to show
    aliases: { search: "name", properties: "propertyFilter" },
    annotations: {
      readOnlyHint: true,
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const { projectId, propertyFilter, ...filters } = args;
      const tablesPage = await client.listTablesPage(projectId, { ...filters, properties: propertyFilter });
      // A table of the project the caller named does not repeat it; one of a dependency, which scope 'all' finds, does
      const ofDependency = (table: Types.TableMetadata): boolean => table.projectId !== undefined && table.projectId !== projectId;
      return textResult(formatStudioPage(tablesPage, filters.offset, {
        noun: "tables",
        // The module of a table, which many tables share, is written once over them. Studio leaves it out of the
        // tables of the project when they are filtered by it
        groupBy: {
          header: "module",
          value: (table) => table.module ?? (ofDependency(table) ? undefined : filters.module),
        },
        render: tableNames(tablesPage.items, ofDependency),
      }));
    },
  });

  registerTool({
    name: "get_table",
    compiled: true,
    title: "Get Table Structure & Data",
    description:
      `Read a table as JSON, in the shape openl_update_table and openl_create_project_table take. ${TABLE_SHAPE} 'notes' holds the Excel notes, 'styles' the cell styles; writes keep both. 'pos' is the A1 address of the first cell, for the cells compilation messages name. Positions are those of the whole table, also in a slice. Answers the 'version' the edit tools take.`,
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
      "Delete a whole table, of any type, from its sheet; openl_edit_table removes rows or columns. The project is not compiled after it: check with openl_project_status that nothing references the table.",
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
      `Replace a whole table with the rows, merges and formulas openl_get_table answered, changed; to change a few cells or remove rows use openl_edit_table. A row may end before the last column; notes and styles are kept. Example: {"tableId":"rates","version":"…","rows":[["Data Rate rates"],["code","rate"],["Code","Rate"],["A",1.5]]}.`,
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
      `Create a table in a module, or in a new .xlsx module at modulePath, its rows, merges and formulas as openl_get_table answers a table like it. Example: {"module":"Rules","name":"rates","rows":[["Data Rate rates",null],["code","rate"],["Code","Rate"],["A",1.5]],"merges":[[0,0,1,2]]}.`,
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
