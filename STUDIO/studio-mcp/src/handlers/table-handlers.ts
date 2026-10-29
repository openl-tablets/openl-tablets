/**
 * Rules/tables tool handlers — list/get tables and update/append/create them.
 * Owns the structured-payload argument validation used by the editing tools;
 * the post-edit tail they share lives in `table-edit.ts`.
 */
import { ProtocolError, ProtocolErrorCode } from "@modelcontextprotocol/server";
import type { ZodError } from "zod";

import type { OpenLClient } from "../client.js";
import * as schemas from "../schemas.js";
import type * as Types from "../types.js";
import { paginateCollection, type Truncation } from "../formatters.js";
import { isPlainObject, sanitizeError } from "../utils.js";
import { formatResult, formatSchemaValidationError, registerTool, type ToolResponse } from "./common.js";
import { finishTableEdit } from "./table-edit.js";

/** Validate that an optional table id in a replacement body matches the path id. */
function validateTableIdMatch(tableId: string, viewId: string | undefined): void {
  if (viewId !== undefined && viewId !== tableId) {
    throw new ProtocolError(
      ProtocolErrorCode.InvalidParams,
      `Table ID mismatch: tableId parameter is "${tableId}" but view.id is "${viewId}". ` +
      `These must match. Use the same ID from get_table() for both parameters.`
    );
  }
}

/**
 * Refuse a whole-table replacement that would remove rows. A sliced RawSource
 * view remains dangerous after a caller drops its `totalRows` marker, so the
 * submitted height must be checked against a bounded read of the live table.
 */
async function validateRawSourceReplacementRows(
  client: OpenLClient,
  projectId: string,
  tableId: string,
  source: Types.RawTableCell[][],
): Promise<void> {
  const current = await client.getTable(projectId, tableId, { startRow: 0, maxRows: 1 });

  const currentRows = Math.max(current.source.length, current.totalRows ?? 0);
  if (source.length < currentRows) {
    throw new ProtocolError(
      ProtocolErrorCode.InvalidParams,
      `Cannot replace table '${tableId}' with ${source.length} row(s): the current table has ${currentRows} row(s). ` +
        "openl_update_table cannot remove rows because a partial RawSource view without totalRows is " +
        "indistinguishable from an intentional deletion. Use the delete_rows action of openl_edit_table with an " +
        "explicit position and count to remove rows, or another of its actions for isolated edits. Nothing was written.",
    );
  }
}

/**
 * LLM clients frequently send the nested payload (view/table) as a
 * JSON *string* instead of an object; the client would then POST a bare JSON string
 * and the backend rejects it with an opaque 400. Parse such a string back into a
 * value so validation (and the handler) see the real object. A string that LOOKS
 * like JSON but fails to parse is reported precisely; any other string is left
 * for the schema to reject ("expected object, received string").
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
        `(${sanitizeError(err)}). Pass '${payloadArg}' as a JSON object, not a string.`,
    );
  }
}

/** Build an actionable message from a Zod validation failure, with the note on a typed table DTO. */
function formatValidationError(payloadArg: string, toolName: string, error: ZodError): string {
  let message = formatSchemaValidationError(toolName, error);
  const tableTypeIssue = error.issues.some(
    (issue) => issue.path[0] === payloadArg && issue.path[1] === "tableType",
  );
  if (tableTypeIssue) {
    message +=
      `\n\n${payloadArg}.tableType must be exactly "RawSource". Typed table DTOs are intentionally ` +
      `unsupported because they are incomplete and cannot safely round-trip workbook content. ` +
      `Call openl_get_table() and edit its raw source matrix.`;
  }
  return message;
}

/**
 * The name and the validation hooks of a tool whose `payloadArg` argument holds a nested object payload, such as
 * "view". The preprocessor only coerces JSON strings; the shared executor performs the single schema parse and
 * calls the formatter below if validation fails.
 */
function structuredPayloadTool(toolName: string, payloadArg: string): {
  name: string;
  validateArgs: (args: unknown) => unknown;
  formatValidationError: (error: ZodError) => string;
} {
  return {
    name: toolName,
    validateArgs: (args: unknown): unknown => {
      if (!isPlainObject(args)) return args;
      const original = args[payloadArg];
      const coerced = coercePayloadJson(original, payloadArg, toolName);
      return coerced === original ? args : { ...args, [payloadArg]: coerced };
    },
    formatValidationError: (error: ZodError): string => formatValidationError(payloadArg, toolName, error),
  };
}

export function registerTableHandlers(): void {
  registerTool({
    name: "list_tables",
    compiled: true,
    title: "List Project Tables",
    description: "List tables/rules in a project with optional filters for kind, name, and properties. Results are paginated (default 50, maximum 200): when a complete inventory is required, follow pagination.has_more and call again with pagination.next_offset until has_more is false. Returns table metadata including 'tableId' (the 'id' field) which is required for calling openl_get_table(), openl_update_table(), openl_edit_table(), or openl_run_table(). Use the 'tableId' field from the response to reference specific tables in other API calls. A table keeps its id while it is edited or moved; a rename gives it a new id, which the edit tools return as 'tableId'.",
    schema: schemas.listTablesSchema,
    annotations: {
      readOnlyHint: true,
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const { response_format: format, projectId, ...filters } = args;
      const tablesPage = await client.listTablesPage(projectId, filters);
      // Every table of the page belongs to the project the caller named, which Studio repeats in each of them
      const tables = tablesPage.items.map(({ project, projectId: _, ...table }) => table);
      const paginated = paginateCollection({ ...tablesPage, items: tables }, filters.offset);

      return formatResult(paginated.data, format, {
        pagination: paginated.pagination,
        dataType: "tables",
      });
    },
  });

  registerTool({
    name: "get_table",
    compiled: true,
    title: "Get Table Structure & Data",
    description:
      "Get a table as its authoritative RawSource 2D cell matrix. A cell carries no address of its own: the cell in row r and column c of 'source' lies window.startRow + r rows below and c columns right of the first cell of 'pos', which names the whole table also for a slice; compilation messages name cells by those addresses. Cell values are scalar string/number/boolean/null values or Studio's one-dimensional multi-value arrays; representable values can be submitted unchanged through every raw write tool. Typed/parsed table views are intentionally unsupported because they are incomplete and cannot safely round-trip workbook content. Every answer states its rows in 'window' next to the data: startRow, endRow, rows, totalRows, complete (whether it holds every row of the table) and nextStartRow while rows remain. startRow/maxRows read a large table in row slices of exactly those rows (a windowed response carries totalRows; a merged cell cut at the edge of a slice keeps its value and names its whole area in mergedArea), and styles=true adds each cell's Excel style. A window cannot replace the whole table: openl_update_table rejects totalRows and independently refuses a source with fewer rows than the live table, so removing the marker cannot erase omitted rows. For a large table whose complete response becomes a preview, use openl_edit_table instead. A table keeps its id while it is edited or moved; Studio still serves an id from before a rename, and answers 410 for a deleted table. The answer carries the 'version' of the table: pass it to the edit tools, so that an edit over a table changed since — in Excel, on disk or through another client — is refused instead of overwriting that change.",
    schema: schemas.getTableSchema,
    annotations: {
      readOnlyHint: true,
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const table = await client.getTable(args.projectId, args.tableId, args);
      const startRow = args.startRow ?? 0;
      // Studio names the rows of the table only when the window leaves some out
      const totalRows = table.totalRows ?? startRow + table.source.length;

      return formatResult(withoutAddresses(table), "json", { truncation: rowTruncation(table, startRow, totalRows) });
    },
  });

  registerTool({
    name: "delete_table",
    compiled: true,
    title: "Delete Table",
    description:
      "Delete an ENTIRE table from a project. The whole table area is cleared from the sheet regardless of table type, so the table no longer exists once the project is recompiled. To remove only a row or column WITHIN a table, use openl_edit_table instead. The studio does not auto-compile after the delete — run openl_project_status afterward to confirm the project still compiles (a dangling reference to the deleted table surfaces there). 'version' from openl_get_table is required: Studio deletes nothing when the table changed since.",
    schema: schemas.tableOnlySchema,
    annotations: {
      destructiveHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const projectId = args.projectId;
      const requestedId = args.tableId;
      await client.deleteTable(projectId, requestedId, args.version);

      const result = {
        success: true,
        message: `Successfully deleted table ${requestedId}`,
        note: "The table area is cleared regardless of type; the table no longer exists once the project recompiles. Run openl_project_status to confirm the project still compiles.",
      };

      return formatResult(result);
    },
  });

  registerTool({
    ...structuredPayloadTool("update_table", "view"),
    compiled: true,
    title: "Replace Entire Table",
    description:
      "Replace the ENTIRE table RawSource matrix with a modified version; a cell sent with the value it holds is left as it is. Typed table DTOs are intentionally unsupported. Use for modifying, reordering, or structural changes that preserve or add rows; row removal is rejected, so use the delete_rows action of openl_edit_table with an explicit position/count. Prefer openl_edit_table for isolated edits and additions. Required workflow: call get_table() without styles=true, preserve the complete matrix including covered cells/spans and scalar or one-dimensional multi-value cell values, modify it, then pass the full RawSource object here. Arrays must be non-empty and contain only scalar values; [null], nested arrays, objects, and string elements surrounded by Studio-trimmed whitespace or control characters are rejected. Before writing, the tool reads one live row to verify the submitted source is not shorter than the table; this prevents a sliced view from deleting unseen rows even if totalRows was removed. Studio table write APIs cannot change formatting, so cell style is read-only and rejected. The 'version' openl_get_table answered stays in the view and is required: Studio refuses the write when the table changed since — in Excel, on disk or through another client — and writes nothing, so read the table again and repeat the change. The response returns the CURRENT tableId, a new one when the edit renamed the table, and the table's new 'version'. The tool reads the table back to trigger recompilation, so openl_project_status reflects the change.",
    schema: schemas.updateTableSchema,
    annotations: {
      destructiveHint: true,
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const { projectId, tableId, view } = args;
      // Preserve the client contract: an id mismatch is an input error and
      // must fail before the row-count guard performs any Studio request.
      validateTableIdMatch(tableId, view.id);
      await validateRawSourceReplacementRows(client, projectId, tableId, view.source);

      // Studio answers the new id of a table the update renamed.
      const reportedId = await client.updateTable(projectId, tableId, view);
      return finishTableEdit(client, args, reportedId, `Successfully updated table ${tableId}`, "update");
    },
  });

  registerTool({
    ...structuredPayloadTool("create_project_table", "table"),
    compiled: true,
    title: "Create New Table",
    description:
      "Create a table from its complete RawSource 2D cell matrix. Typed table creation DTOs are intentionally unsupported because they omit workbook features and do not round-trip reliably. Requires moduleName plus table { tableType: \"RawSource\", name, source }. By default moduleName identifies an existing module; pass modulePath ending in .xlsx to create a new module. Build the exact OpenL grid from the guides (openl_get_guides) or copy an existing raw source, including covered cells/spans where needed. Cell values may be scalars or Studio's one-dimensional multi-value arrays; arrays must be non-empty and contain only scalars, while [null], nested arrays, objects, and string elements surrounded by Studio-trimmed whitespace or control characters are rejected. Studio table write APIs cannot set cell formatting, so style is rejected. The response is metadata, not a compilation result; call openl_project_status afterward.",
    schema: schemas.createProjectTableSchema,
    annotations: {
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const createdTable = await client.createProjectTable(args.projectId, {
        moduleName: args.moduleName,
        modulePath: args.modulePath,
        sheetName: args.sheetName,
        table: args.table,
      });

      const result = {
        success: true,
        tableId: createdTable.id,
        tableName: createdTable.name,
        tableType: createdTable.tableType,
        file: createdTable.file,
        message: `Successfully created ${createdTable.tableType} table '${createdTable.name}' in module '${args.moduleName}'`,
      };

      return formatResult(result);
    },
  });
}

/**
 * The rows a table read answers: the first and the last of them, how many rows the table has, whether the answer
 * holds every one of them, and the row to read next while rows remain.
 */
function rowWindow(startRow: number, rows: number, totalRows: number): Record<string, unknown> {
  return {
    startRow,
    ...(rows > 0 ? { endRow: startRow + rows - 1 } : {}),
    rows,
    totalRows,
    complete: startRow === 0 && rows === totalRows,
    ...(startRow + rows < totalRows ? { nextStartRow: startRow + rows } : {}),
  };
}

/**
 * The cut of a table read: its leading rows, with the total number of rows of the table and the row to read next.
 * A cut view carries `totalRows`, so it cannot be written back as the whole table.
 */
function rowTruncation(table: Types.RawTableView, startRow: number, totalRows: number): Truncation {
  return {
    items: table.source.length,
    unit: "rows",
    cut: (count) => withoutAddresses({ ...table, source: leadingRows(table.source, count), totalRows }),
    next: (count) => `continue with startRow=${startRow + count}`,
    metadata: (count) => ({ window: rowWindow(startRow, count, totalRows) }),
  };
}

/**
 * A table read without the A1 address of each cell, which its position in the source and the `pos` of the table
 * give: the addresses take about 40% of a read.
 */
function withoutAddresses(table: Types.RawTableView): Types.RawTableView {
  return { ...table, source: table.source.map((row) => row.map(({ cell, ...rest }) => rest)) };
}

/**
 * The first `count` rows of a table. A merged cell reaching past the last of them is cut there as a window of Studio
 * cuts it: it spans the rows left, and names its whole area in `mergedArea`.
 */
function leadingRows(source: Types.RawTableCell[][], count: number): Types.RawTableCell[][] {
  return source.slice(0, count).map((row, index) => row.map((cell) => {
    const rowspan = cell.rowspan ?? 1;
    if (index + rowspan <= count || cell.cell === undefined) {
      return cell;
    }
    const left = count - index;
    return {
      ...cell,
      rowspan: left > 1 ? left : undefined,
      mergedArea: cell.mergedArea ?? areaOf(cell.cell, rowspan, cell.colspan ?? 1),
    };
  }));
}

/** The A1 area of a merged cell from its first cell, such as `B4:C299` for `B4` spanning 296 rows and 2 columns. */
function areaOf(first: string, rowspan: number, colspan: number): string {
  const [, letters = "", digits = "0"] = /^([A-Z]+)(\d+)$/.exec(first) ?? [];
  let column = [...letters].reduce((index, letter) => index * 26 + letter.charCodeAt(0) - 64, 0) + colspan - 1;
  let last = "";
  while (column > 0) {
    last = String.fromCharCode(65 + (column - 1) % 26) + last;
    column = Math.floor((column - 1) / 26);
  }
  return `${first}:${last}${Number(digits) + rowspan - 1}`;
}
