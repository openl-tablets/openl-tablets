/**
 * Rules/tables tool handlers — list/get tables and update/append/create them.
 * Owns the structured-payload argument validation used by the editing tools;
 * the post-edit table-id tracking they share lives in `table-id-tracking.ts`.
 */
import { ProtocolError, ProtocolErrorCode } from "@modelcontextprotocol/server";
import type { ZodError } from "zod";

import { validateTableIdMatch, type OpenLClient } from "../client.js";
import * as schemas from "../schemas.js";
import type * as Types from "../types.js";
import { formatResponse, paginateCollection } from "../formatters.js";
import { isNotFoundError, isPlainObject } from "../utils.js";
import { registerTool, STALE_TABLE_ID_HINT, type ToolResponse } from "./common.js";
import {
  finalizeTableEdit,
  listTablesByExactName,
  resolveCurrentTableId,
  resolveTableIdAlias,
  withStaleIdRetry,
  type TableIdentity,
} from "./table-id-tracking.js";

/**
 * EPBDS-16085: reject RawSource append rows whose width does not match the
 * table, BEFORE anything is posted. The backend accepts short rows and silently
 * pads the missing cells with blanks — corrupt data with success:true.
 *
 * The table width comes from the raw source matrix (which includes covered
 * placeholder cells, so every row has one entry per column). A submitted row is
 * accepted when either its entry count matches the width (placeholder style,
 * mirroring openl_get_table raw output) or the columns covered via colspan add
 * up to the width (no-placeholder style). If the matrix is ragged the width
 * cannot be trusted and validation is skipped rather than blocking valid
 * appends.
 */
function validateRawSourceAppendRows(
  rows: Types.RawTableCell[][],
  source: Types.RawTableCell[][],
  tableLabel: string,
): void {
  if (!Array.isArray(source) || source.length === 0 || rows.length === 0) {
    return;
  }
  const widths = new Set(source.map((row) => (Array.isArray(row) ? row.length : -1)));
  if (widths.size !== 1 || widths.has(-1)) {
    return;
  }
  const width = source[0].length;
  if (width === 0) {
    return;
  }

  const problems: string[] = [];
  rows.forEach((row, index) => {
    const entryCount = row.length;
    let coveredColumns = 0;
    for (const cell of row) {
      if (cell && cell.covered === true) {
        coveredColumns += 1;
        continue;
      }
      const colspan = Number(cell?.colspan);
      coveredColumns += Number.isFinite(colspan) && colspan >= 2 ? colspan : 1;
    }
    if (entryCount !== width && coveredColumns !== width) {
      problems.push(
        `row ${index + 1} has ${entryCount} cell(s)` +
          (coveredColumns !== entryCount ? ` covering ${coveredColumns} column(s)` : ""),
      );
    }
  });

  if (problems.length > 0) {
    throw new ProtocolError(
      ProtocolErrorCode.InvalidParams,
      `Cannot append to table '${tableLabel}': the table is ${width} column(s) wide, but ${problems.join("; ")}. ` +
        `Each appended RawSource row must cover all ${width} column(s) — provide one cell object per column ` +
        `(use { "value": null } for intentionally blank cells). Nothing was appended. ` +
        `Call openl_get_table() to inspect the table's exact column layout.`,
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
  const readFirstRow = (id: string): Promise<Types.RawTableView> =>
    client.getTable(projectId, id, { startRow: 0, maxRows: 1 });

  let current: Types.RawTableView;
  try {
    current = await readFirstRow(tableId);
  } catch (error) {
    const aliased = isNotFoundError(error) ? resolveTableIdAlias(projectId, tableId) : undefined;
    if (aliased === undefined) {
      throw error;
    }
    current = await readFirstRow(aliased);
  }

  const reportedTotal = current.totalRows;
  const currentRows = typeof reportedTotal === "number" && Number.isFinite(reportedTotal)
    ? Math.max(current.source.length, reportedTotal)
    : current.source.length;
  if (source.length < currentRows) {
    throw new ProtocolError(
      ProtocolErrorCode.InvalidParams,
      `Cannot replace table '${tableId}' with ${source.length} row(s): the current table has ${currentRows} row(s). ` +
        "openl_update_table cannot remove rows because a partial RawSource view without totalRows is " +
        "indistinguishable from an intentional deletion. Use openl_delete_table_rows with an explicit position " +
        "and count to remove rows, or another narrow raw table action for isolated edits. Nothing was written.",
    );
  }
}

interface ToolValidationSpec {
  /** Top-level argument holding the nested object payload (e.g. "appendData"). */
  payloadArg: string;
}

interface StructuredPayloadValidation {
  validateArgs: (args: unknown) => unknown;
  formatValidationError: (error: ZodError) => string;
}

/**
 * LLM clients frequently send the nested payload (appendData/view/table) as a
 * JSON *string* instead of an object; axios would then POST a bare JSON string
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
        `(${err instanceof Error ? err.message : String(err)}). Pass '${payloadArg}' as a JSON object, not a string.`,
    );
  }
}

/** Build an actionable message from a Zod validation failure. */
function formatValidationError(spec: ToolValidationSpec, toolName: string, error: ZodError): string {
  let message = `Invalid arguments for ${toolName}:\n${schemas.z.prettifyError(error)}`;
  const tableTypeIssue = error.issues.some(
    (issue) => issue.path[0] === spec.payloadArg && issue.path[1] === "tableType",
  );
  if (tableTypeIssue) {
    message +=
      `\n\n${spec.payloadArg}.tableType must be exactly "RawSource". Typed table DTOs are intentionally ` +
      `unsupported because they are incomplete and cannot safely round-trip workbook content. ` +
      `Call openl_get_table() and edit its raw source matrix.`;
  }
  return message;
}

/**
 * Prepare the validation hooks for a structured-payload tool. The preprocessor
 * only coerces JSON strings; the shared executor performs the single schema
 * parse and calls the formatter below if validation fails.
 */
function structuredPayloadValidation(
  toolName: string,
  spec: ToolValidationSpec,
): StructuredPayloadValidation {
  return {
    validateArgs: (args: unknown): unknown => {
      if (!isPlainObject(args)) return args;
      const original = args[spec.payloadArg];
      const coerced = coercePayloadJson(original, spec.payloadArg, toolName);
      return coerced === original ? args : { ...args, [spec.payloadArg]: coerced };
    },
    formatValidationError: (error: ZodError): string =>
      formatValidationError(spec, toolName, error),
  };
}

export function registerTableHandlers(): void {
  registerTool({
    name: "list_tables",
    category: "Rules & Tables",
    title: "List Project Tables",
    description: "List tables/rules in a project with optional filters for kind, name, and properties. Results are paginated (default 50, maximum 200): when a complete inventory is required, follow pagination.has_more and call again with pagination.next_offset until has_more is false. Returns table metadata including 'tableId' (the 'id' field) which is required for calling get_table(), update_table(), append_table(), or run_project_tests(). Use the 'tableId' field from the response to reference specific tables in other API calls. IMPORTANT: a table id is derived from its location and changes when an edit relocates the table (it had no room to grow in place). After openl_update_table/openl_append_table, use the 'tableId' those tools return (or re-run openl_list_tables); an id from a listing taken before such an edit is stale.",
    schema: schemas.listTablesSchema,
    annotations: {
      readOnlyHint: true,
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = args;

      const format = typedArgs.response_format;
      const { limit = 50, offset = 0 } = typedArgs;

      const filters: Types.TableFilters = {};
      if (typedArgs.kind && typedArgs.kind.length > 0) {
        filters.kind = typedArgs.kind;
      }
      if (typedArgs.name) filters.name = typedArgs.name;
      if (typedArgs.properties) filters.properties = typedArgs.properties;

      filters.offset = offset;
      filters.limit = limit;

      const tablesPage = await client.listTablesPage(typedArgs.projectId, filters);

      // If API already paginated, use its pagination metadata
      // Otherwise apply client-side pagination
      const paginated = paginateCollection(tablesPage, limit, offset);

      const formattedResult = formatResponse(paginated.data, format, {
        pagination: paginated.pagination,
        dataType: "tables",
      });

      return {
        content: [{ type: "text", text: formattedResult }],
      };
    },
  });

  registerTool({
    name: "get_table",
    category: "Rules & Tables",
    title: "Get Table Structure & Data",
    description:
      "Get a table as its authoritative RawSource 2D cell matrix. Cell values are scalar string/number/boolean/null values or Studio's one-dimensional multi-value arrays; representable values can be submitted unchanged through every raw write tool. Typed/parsed table views are intentionally unsupported because they are incomplete and cannot safely round-trip workbook content. startRow/maxRows read a large table in row slices (a windowed response carries totalRows), and styles=true adds each cell's Excel style. A window cannot replace the whole table: openl_update_table rejects totalRows and independently refuses a source with fewer rows than the live table, so removing the marker cannot erase omitted rows. For a large table whose complete response becomes a preview, use the narrow raw table action tools instead. A table id changes when an edit relocates the table; stale ids produced by this server are resolved automatically, otherwise refresh ids with openl_list_tables().",
    schema: schemas.getTableSchema,
    annotations: {
      readOnlyHint: true,
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = args;

      const format = typedArgs.response_format;

      const fetchTable = (id: string): Promise<Types.RawTableView> =>
        client.getTable(typedArgs.projectId, id, {
          startRow: typedArgs.startRow,
          maxRows: typedArgs.maxRows,
          styles: typedArgs.styles,
        });

      // EPBDS-16084: a table's id changes after every edit. If this id went
      // stale through an edit made via this server, resolve it transparently.
      let table: Types.RawTableView;
      let staleIdNote: string | undefined;
      try {
        table = await fetchTable(typedArgs.tableId);
      } catch (error) {
        const aliased = isNotFoundError(error)
          ? resolveTableIdAlias(typedArgs.projectId, typedArgs.tableId)
          : undefined;
        if (aliased === undefined) {
          throw error;
        }
        table = await fetchTable(aliased);
        staleIdNote =
          `Note: the provided tableId '${typedArgs.tableId}' is stale — the table was edited after that id was issued. ` +
          `It was automatically resolved to the current id '${aliased}'. ${STALE_TABLE_ID_HINT}`;
      }

      const formattedResult = formatResponse(table, format);
      const content: ToolResponse["content"] = staleIdNote
        ? [
            { type: "text", text: staleIdNote },
            { type: "text", text: formattedResult },
          ]
        : [{ type: "text", text: formattedResult }];

      return { content };
    },
  });

  registerTool({
    name: "delete_table",
    category: "Rules & Tables",
    title: "Delete Table",
    description:
      "Delete an ENTIRE table from a project. The whole table area is cleared from the sheet regardless of table type, so the table no longer exists once the project is recompiled. To remove only a row or column WITHIN a table, use openl_delete_table_rows / openl_delete_table_columns instead. If the given id went stale through an edit made via this server, it is resolved to the current id automatically. The studio does not auto-compile after the delete — run openl_project_status afterward to confirm the project still compiles (a dangling reference to the deleted table surfaces there).",
    schema: schemas.deleteTableSchema,
    annotations: {
      destructiveHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = args;

      const format = typedArgs.response_format;
      const projectId = typedArgs.projectId;
      const requestedId = typedArgs.tableId;
      const notes: string[] = [
        "The table area is cleared regardless of type; the table no longer exists once the project recompiles. Run openl_project_status to confirm the project still compiles.",
      ];

      // A 404 deletes nothing, so retrying with a known rename is safe.
      const { staleNote } = await withStaleIdRetry(projectId, requestedId, (id) =>
        client.deleteTable(projectId, id),
      );
      if (staleNote) {
        notes.push(staleNote);
      }

      const result = {
        success: true,
        message: `Successfully deleted table ${requestedId}`,
        note: notes.join(" "),
      };

      return {
        content: [{ type: "text", text: formatResponse(result, format) }],
      };
    },
  });

  registerTool({
    name: "update_table",
    ...structuredPayloadValidation("update_table", {
      payloadArg: "view",
    }),
    category: "Rules & Tables",
    title: "Replace Entire Table",
    description:
      "Replace the ENTIRE table RawSource matrix with a modified version. Typed table DTOs are intentionally unsupported. Use for modifying, reordering, or structural changes that preserve or add rows; row removal is rejected, so use openl_delete_table_rows with an explicit position/count. Prefer the narrow raw action tools for isolated edits and append_table for additions. Required workflow: call get_table() without styles=true, preserve the complete matrix including covered cells/spans and scalar or one-dimensional multi-value cell values, modify it, then pass the full RawSource object here. Arrays must be non-empty and contain only scalar values; [null], nested arrays, objects, and string elements surrounded by Studio-trimmed whitespace or control characters are rejected. Before writing, the tool reads one live row to verify the submitted source is not shorter than the table; this prevents a sliced view from deleting unseen rows even if totalRows was removed. Studio table write APIs cannot change formatting, so cell style is read-only and rejected. The response returns the CURRENT tableId after relocation. The tool reads the table back to trigger recompilation, so openl_project_status reflects the change.",
    schema: schemas.updateTableSchema,
    annotations: {
      destructiveHint: true,
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = args;

      const format = typedArgs.response_format;

      const view = typedArgs.view;

      const projectId = typedArgs.projectId;
      const requestedId = typedArgs.tableId;
      const notes: string[] = [];

      // Preserve the client contract: an id mismatch is an input error and
      // must fail before the row-count guard performs any Studio request.
      validateTableIdMatch(requestedId, view.id);
      await validateRawSourceReplacementRows(client, projectId, requestedId, view.source);

      // EPBDS-16084/16086: an edit that relocates the table changes its
      // location-derived id. Studio PR #1778 reports the new id directly (the
      // updateTable call returns it); for older studios that don't, fall back to
      // re-resolving by identity — so capture the table's identity (and which
      // same-name ids exist) before the edit for that fallback.
      const identity: TableIdentity | undefined =
        typeof view.name === "string" && view.name
          ? { name: view.name, kind: view.kind }
          : undefined;
      let idsBeforeEdit: Set<string> | undefined;
      if (identity) {
        const before = await listTablesByExactName(client, projectId, identity.name);
        if (before) {
          idsBeforeEdit = new Set(before.flatMap((t) => typeof t.id === "string" ? [t.id] : []));
        }
      }

      // A 404 writes nothing, so retrying with a known rename is safe. The first
      // attempt sends the view as-is (keeping client.updateTable's id-mismatch
      // guard); only the retry forces the resolved id into the view.
      const { value: reportedNewId, tableId, staleNote } = await withStaleIdRetry(
        projectId,
        requestedId,
        (id) => client.updateTable(projectId, id, id === requestedId ? view : { ...view, id }),
      );
      if (staleNote) {
        notes.push(staleNote);
      }

      // Determine the table's current id after the write. Prefer the id the
      // studio reported (authoritative); otherwise fall back to re-resolving by
      // identity (older studios that answer 204 even when the id changed).
      let currentId = tableId;
      if (reportedNewId) {
        currentId = reportedNewId;
      } else if (identity) {
        const resolved = await resolveCurrentTableId(client, projectId, tableId, identity, idsBeforeEdit);
        if (resolved) {
          currentId = resolved;
        }
      }

      const { result } = await finalizeTableEdit(
        client, projectId, requestedId, tableId, currentId,
        `Successfully updated table ${requestedId}`, "update", notes,
      );

      return {
        content: [{ type: "text", text: formatResponse(result, format) }],
      };
    },
  });

  registerTool({
    name: "append_table",
    ...structuredPayloadValidation("append_table", {
      payloadArg: "appendData",
    }),
    category: "Rules & Tables",
    title: "Append Raw Source Rows",
    description:
      "Append RawSource rows to an existing table. Typed append DTOs are intentionally unsupported. Every row must cover ALL columns of the table; wrong-width rows are rejected before anything is written. Cell values may be scalars or Studio's one-dimensional multi-value arrays; arrays must be non-empty and contain only scalars, while [null], nested arrays, objects, and string elements surrounded by Studio-trimmed whitespace or control characters are rejected. Use { value: null } for a blank cell and preserve covered placeholders for merged regions. For modifying or reordering use a narrow raw action tool or update_table; remove rows/columns only with the corresponding delete action. The response returns the CURRENT tableId after relocation, and the read-back triggers recompilation.",
    schema: schemas.appendTableSchema,
    annotations: {
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = args;

      const format = typedArgs.response_format;

      const appendData: Types.RawTableAppend = typedArgs.appendData;

      const projectId = typedArgs.projectId;
      const requestedId = typedArgs.tableId;
      const notes: string[] = [];

      // Probe the table before editing. This (a) fails fast on a stale id —
      // resolving it transparently when the rename is known (EPBDS-16084),
      // (b) captures the identity needed to find the table's new id after the
      // edit, and (c) for RawSource provides the source matrix used to validate
      // row width before anything is written (EPBDS-16085).
      let tableId = requestedId;
      let probedView: Types.RawTableView | undefined;
      try {
        probedView = await client.getTable(projectId, tableId);
      } catch (error) {
        if (isNotFoundError(error)) {
          const aliased = resolveTableIdAlias(projectId, tableId);
          if (aliased === undefined) {
            throw error;
          }
          probedView = await client.getTable(projectId, aliased);
          notes.push(
            `The provided tableId '${requestedId}' was stale (the table was edited after that id was issued) and was automatically resolved to '${aliased}'.`,
          );
          tableId = aliased;
        } else {
          throw error;
        }
      }

      if (probedView && Array.isArray(probedView.source)) {
        validateRawSourceAppendRows(
          appendData.rows,
          probedView.source,
          probedView.name || tableId,
        );
      }

      // EPBDS-16084/16086 fallback snapshot: which same-name ids exist before the
      // edit, so the table's new id can be re-resolved on older studios that don't
      // report it. Current studios (PR #1778) report the new id directly below.
      const identity: TableIdentity | undefined = probedView?.name
        ? { name: probedView.name, kind: probedView.kind }
        : undefined;
      let idsBeforeEdit: Set<string> | undefined;
      if (identity) {
        const before = await listTablesByExactName(client, projectId, identity.name);
        if (before) {
          idsBeforeEdit = new Set(before.flatMap((t) => typeof t.id === "string" ? [t.id] : []));
        }
      }

      const reportedNewId = await client.appendProjectTable(projectId, tableId, appendData);

      // Determine the table's current id after the append. Prefer the id the
      // studio reported (authoritative); otherwise fall back to re-resolving by
      // identity (older studios that answer 204 even when the id changed).
      let currentId = tableId;
      if (reportedNewId) {
        currentId = reportedNewId;
      } else if (identity) {
        const resolved = await resolveCurrentTableId(client, projectId, tableId, identity, idsBeforeEdit);
        if (resolved) {
          currentId = resolved;
        }
      }
      const { result } = await finalizeTableEdit(
        client, projectId, requestedId, tableId, currentId,
        `Successfully appended ${appendData.rows.length} raw source row(s) to table ${requestedId}`, "append", notes,
      );

      return {
        content: [{ type: "text", text: formatResponse(result, format) }],
      };
    },
  });

  registerTool({
    name: "create_project_table",
    ...structuredPayloadValidation("create_project_table", {
      payloadArg: "table",
    }),
    category: "Rules & Tables",
    title: "Create New Table",
    description:
      "Create a table from its complete RawSource 2D cell matrix. Typed table creation DTOs are intentionally unsupported because they omit workbook features and do not round-trip reliably. Requires moduleName plus table { tableType: \"RawSource\", name, source }. By default moduleName identifies an existing module; pass modulePath ending in .xlsx to create a new module. Build the exact OpenL grid from the bundled guides or copy an existing raw source, including covered cells/spans where needed. Cell values may be scalars or Studio's one-dimensional multi-value arrays; arrays must be non-empty and contain only scalars, while [null], nested arrays, objects, and string elements surrounded by Studio-trimmed whitespace or control characters are rejected. Studio table write APIs cannot set cell formatting, so style is rejected. The response is metadata, not a compilation result; call openl_project_status afterward.",
    schema: schemas.createProjectTableSchema,
    annotations: {
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = args;

      const format = typedArgs.response_format;

      const createdTable = await client.createProjectTable(typedArgs.projectId, {
        moduleName: typedArgs.moduleName,
        modulePath: typedArgs.modulePath,
        sheetName: typedArgs.sheetName,
        table: typedArgs.table,
      });

      const result = {
        success: true,
        tableId: createdTable.id,
        tableName: createdTable.name,
        tableType: createdTable.tableType,
        file: createdTable.file,
        message: `Successfully created ${createdTable.tableType} table '${createdTable.name}' in module '${typedArgs.moduleName}'`,
      };

      const formattedResult = formatResponse(result, format);

      return {
        content: [{ type: "text", text: formattedResult }],
      };
    },
  });
}
