/**
 * Raw table-source action tools (POST /projects/{id}/tables/{tableId}/actions).
 *
 * One tool per operation×orientation — append/insert/delete one OR more rows or
 * columns (the studio takes a single `rows`/`columns` block target; one row is a
 * one-element block), update a row/column/cell/range, and merge/unmerge cells.
 * Each edits the table's RAW source regardless of table type, unlike
 * openl_update_table / openl_append_table which take complete raw structures
 * structure. All share one runner that handles the stale-id retry, the post-edit
 * id change, and the recompile-on-read — the same machinery the full
 * update/append tools use (see `table-id-tracking.ts`).
 */

import type { ZodType } from "zod";

import * as schemas from "../schemas.js";
import type * as Types from "../types.js";
import { formatResponse } from "../formatters.js";
import { registerTool, type ToolResponse } from "./common.js";
import { finalizeTableEdit, withStaleIdRetry } from "./table-id-tracking.js";
import type { OpenLClient } from "../client.js";

/**
 * Apply one raw-source edit and report the table's current id.
 *
 * On a stale tableId the edit is retried once against the known new id (a 404
 * writes nothing, so the retry is safe). After the edit the table's id may have
 * changed (it was relocated); the new id is recorded as an alias and returned so
 * the caller never reuses the stale one. Finally the table is read back to force
 * the studio to recompile so a later openl_project_status reflects the change.
 */
async function runTableSourceAction(
  client: OpenLClient,
  projectId: string,
  requestedId: string,
  action: Types.RawTableSourceAction,
  pastTenseEdit: string,
  format: Parameters<typeof formatResponse>[1],
): Promise<ToolResponse> {
  // The studio reports the new id directly (200 + { id }) when the edit relocated
  // the table; an in-place edit answers 204 and the id is unchanged. So unlike
  // update/append there is no identity-snapshot fallback to re-resolve the id.
  const { value: reportedNewId, tableId, staleNote } = await withStaleIdRetry(
    projectId,
    requestedId,
    (id) => client.editTableSource(projectId, id, action),
  );

  const notes = staleNote ? [staleNote] : [];
  const { result } = await finalizeTableEdit(
    client,
    projectId,
    requestedId,
    tableId,
    reportedNewId ?? tableId,
    `Successfully ${pastTenseEdit} table ${requestedId}`,
    "edit",
    notes,
  );

  return { content: [{ type: "text", text: formatResponse(result, format) }] };
}

/** Common tail appended to every action tool's description. */
const ACTION_SUFFIX =
  " Operates on the table's RAW source, so it works for any table type. Positions are 0-based " +
  "(row 0 is the header row, column 0 carries the leading labels). An edit that relocates the table " +
  "(it had no room to grow in place) CHANGES its location-derived id; the response always returns the " +
  "table's CURRENT id as 'tableId' (plus previousTableId when it changed) — use it for subsequent calls. " +
  "When the tool writes cell values, it accepts scalar string/number/boolean/null values and Studio's " +
  "one-dimensional multi-value arrays. Arrays must be non-empty and contain only scalars; [null], nested " +
  "arrays, objects, and string elements surrounded by Studio-trimmed whitespace or control characters are rejected. " +
  "Note: the studio does not auto-compile after an edit; this tool reads the table back to trigger the " +
  "recompile, so a subsequent openl_project_status reflects the change.";

/** Arguments shared by every action tool after schema validation. */
interface BaseActionArgs {
  projectId: string;
  tableId: string;
  response_format?: "json" | "markdown";
}

interface ActionToolSpec {
  name: string;
  title: string;
  schema: ZodType;
  description: string;
  /** Past-tense fragment for the success message, e.g. "inserted a row into". */
  pastTenseEdit: string;
  annotations: NonNullable<Parameters<typeof registerTool>[0]["annotations"]>;
  /** Build the request body from the validated arguments. */
  buildAction: (args: BaseActionArgs & Record<string, unknown>) => Types.RawTableSourceAction;
}

/**
 * Append/insert one or more rows/columns. The studio takes a single block target
 * (`rows`/`columns`, accepting one or more) — there is no singular row/column
 * target — so `cells` (a 2D array, one inner list per row/column) is always sent
 * as-is; a single row is just a one-element block.
 */
function buildRowColumnEdit(
  operation: "append" | "insert",
  orientation: "rows" | "columns",
  cells: unknown,
  position?: number,
): Types.RawTableSourceAction {
  const target: Types.RawTableActionTarget = {
    type: orientation,
    cells: cells as Types.RawCellInput[][],
  };
  if (position !== undefined) {
    target.position = position;
  }
  return { operation, target };
}

/**
 * Delete one or more rows/columns. The studio takes a single block target
 * (`rows`/`columns`) with a required `count`, so `count` defaults to 1 (delete a
 * single row/column) when the caller omits it.
 */
function buildRowColumnDelete(
  orientation: "rows" | "columns",
  position: number,
  count: number | undefined,
): Types.RawTableSourceAction {
  return {
    operation: "delete",
    target: { type: orientation, position, count: count ?? 1 },
  };
}

const ACTION_TOOLS: ActionToolSpec[] = [
  // --- Rows / columns: ONE OR MORE, one tool per operation×orientation. The
  //     studio takes a single `rows`/`columns` block target (a single row/column
  //     is a one-element block), so there is no "row" vs "rows" tool to choose. ---
  {
    name: "append_table_rows",
    title: "Append Table Rows (raw)",
    schema: schemas.appendTableRowsSchema,
    pastTenseEdit: "appended rows to",
    annotations: { openWorldHint: true },
    description:
      "Add ONE OR MORE rows to the END of a table's raw source. 'cells' is a 2D array: outer = rows top to bottom, inner = that row's cells left to right (one per column; use { value: null } for blanks). Pass a single row to add one, several for a block." +
      ACTION_SUFFIX,
    buildAction: (a) => buildRowColumnEdit("append", "rows", a.cells),
  },
  {
    name: "append_table_columns",
    title: "Append Table Columns (raw)",
    schema: schemas.appendTableColumnsSchema,
    pastTenseEdit: "appended columns to",
    annotations: { openWorldHint: true },
    description:
      "Add ONE OR MORE columns to the END of a table's raw source. 'cells' is a 2D array: outer = columns left to right, inner = that column's cells top to bottom (one per row). Pass a single column to add one, several for a block." +
      ACTION_SUFFIX,
    buildAction: (a) => buildRowColumnEdit("append", "columns", a.cells),
  },
  {
    name: "insert_table_rows",
    title: "Insert Table Rows (raw)",
    schema: schemas.insertTableRowsSchema,
    pastTenseEdit: "inserted rows into",
    annotations: { openWorldHint: true },
    description:
      "Insert ONE OR MORE rows at 'position' in a table's raw source, shifting the rows at and below it down. 'position' is 1..height (height appends to the end). 'cells' is a 2D array (rows × that row's cells)." +
      ACTION_SUFFIX,
    buildAction: (a) => buildRowColumnEdit("insert", "rows", a.cells, a.position as number),
  },
  {
    name: "insert_table_columns",
    title: "Insert Table Columns (raw)",
    schema: schemas.insertTableColumnsSchema,
    pastTenseEdit: "inserted columns into",
    annotations: { openWorldHint: true },
    description:
      "Insert ONE OR MORE columns at 'position' in a table's raw source, shifting the columns at and to the right of it. 'position' is 1..width (width appends to the end). 'cells' is a 2D array (columns × that column's cells)." +
      ACTION_SUFFIX,
    buildAction: (a) => buildRowColumnEdit("insert", "columns", a.cells, a.position as number),
  },
  {
    name: "delete_table_rows",
    title: "Delete Table Rows (raw)",
    schema: schemas.deleteTableRowsSchema,
    pastTenseEdit: "deleted rows from",
    annotations: { destructiveHint: true, openWorldHint: true },
    description:
      "Delete ONE OR MORE rows starting at 'position' (1..height-1) from a table's raw source, shifting the rows below up. 'count' defaults to 1. The header row (0) cannot be deleted." +
      ACTION_SUFFIX,
    buildAction: (a) => buildRowColumnDelete("rows", a.position as number, a.count as number | undefined),
  },
  {
    name: "delete_table_columns",
    title: "Delete Table Columns (raw)",
    schema: schemas.deleteTableColumnsSchema,
    pastTenseEdit: "deleted columns from",
    annotations: { destructiveHint: true, openWorldHint: true },
    description:
      "Delete ONE OR MORE columns starting at 'position' (1..width-1) from a table's raw source, shifting the columns to the right left. 'count' defaults to 1. The leading-label column (0) cannot be deleted." +
      ACTION_SUFFIX,
    buildAction: (a) => buildRowColumnDelete("columns", a.position as number, a.count as number | undefined),
  },
  {
    name: "update_table_row",
    title: "Update Table Row (raw)",
    schema: schemas.updateTableRowSchema,
    pastTenseEdit: "updated a row of",
    annotations: { idempotentHint: true, openWorldHint: true },
    description:
      "Overwrite the cells of an existing row at 'position' (0..height-1) in a table's raw source, left to right. The table is not resized." +
      ACTION_SUFFIX,
    buildAction: (a) => ({
      operation: "update",
      target: { type: "row", position: a.position as number, cells: a.cells as Types.RawCellInput[] },
    }),
  },
  {
    name: "update_table_column",
    title: "Update Table Column (raw)",
    schema: schemas.updateTableColumnSchema,
    pastTenseEdit: "updated a column of",
    annotations: { idempotentHint: true, openWorldHint: true },
    description:
      "Overwrite the cells of an existing column at 'position' (0..width-1) in a table's raw source, top to bottom. The table is not resized." +
      ACTION_SUFFIX,
    buildAction: (a) => ({
      operation: "update",
      target: { type: "column", position: a.position as number, cells: a.cells as Types.RawCellInput[] },
    }),
  },
  {
    name: "update_table_cell",
    title: "Update Table Cell (raw)",
    schema: schemas.updateTableCellSchema,
    pastTenseEdit: "updated a cell of",
    annotations: { idempotentHint: true, openWorldHint: true },
    description:
      "Update the value of a single existing cell at ('row','column') in a table's raw source. 'value' is required: pass a scalar or one-dimensional multi-value array to set the cell, or null to clear it." +
      ACTION_SUFFIX,
    buildAction: (a) => ({
      operation: "update",
      target: {
        type: "cell",
        row: a.row as number,
        column: a.column as number,
        // `value` is required by the schema (nullable), so it is always present and
        // sent explicitly: a non-null value sets the cell, null clears it.
        value: a.value as Types.RawCellValue,
      },
    }),
  },
  {
    name: "merge_table_cells",
    title: "Merge Table Cells (raw)",
    schema: schemas.mergeTableCellsSchema,
    pastTenseEdit: "merged cells of",
    annotations: { idempotentHint: true, openWorldHint: true },
    description:
      "Merge a rectangular range of cells into one in a table's raw source, keeping the value of the top-left cell at ('row','column'). The range ('rowspan'×'colspan') must cover more than one cell and stay within the table." +
      ACTION_SUFFIX,
    buildAction: (a) => ({
      operation: "merge",
      target: {
        type: "cells",
        row: a.row as number,
        column: a.column as number,
        rowspan: a.rowspan as number,
        colspan: a.colspan as number,
      },
    }),
  },
  {
    name: "unmerge_table_cells",
    title: "Unmerge Table Cells (raw)",
    schema: schemas.unmergeTableCellsSchema,
    pastTenseEdit: "unmerged cells of",
    annotations: { idempotentHint: true, openWorldHint: true },
    description:
      "Unmerge the merged cell that covers ('row','column') in a table's raw source, splitting it back into individual cells." +
      ACTION_SUFFIX,
    buildAction: (a) => ({
      operation: "unmerge",
      target: { type: "cells", row: a.row as number, column: a.column as number },
    }),
  },
  {
    name: "update_table_range",
    title: "Update Table Range (raw)",
    schema: schemas.updateTableRangeSchema,
    pastTenseEdit: "updated a range of",
    annotations: { idempotentHint: true, openWorldHint: true },
    description:
      "Overwrite a rectangular RANGE of cells in place, anchored at the top-left ('row','column'), in a table's raw source. 'cells' is a 2D array (rows × that row's cells); the range must cover more than one cell and fit within the table (not resized). For a single cell use openl_update_table_cell." +
      ACTION_SUFFIX,
    buildAction: (a) => ({
      operation: "update",
      target: {
        type: "range",
        row: a.row as number,
        column: a.column as number,
        cells: a.cells as Types.RawCellInput[][],
      },
    }),
  },
];

export function registerTableActionHandlers(): void {
  for (const spec of ACTION_TOOLS) {
    registerTool({
      name: spec.name,
      category: "Rules & Tables",
      title: spec.title,
      description: spec.description,
      schema: spec.schema,
      annotations: spec.annotations,
      handler: async (args, client): Promise<ToolResponse> => {
        const typedArgs = args as BaseActionArgs & Record<string, unknown>;
        const format = typedArgs.response_format;
        const action = spec.buildAction(typedArgs);
        return runTableSourceAction(client, typedArgs.projectId, typedArgs.tableId, action, spec.pastTenseEdit, format);
      },
    });
  }
}
