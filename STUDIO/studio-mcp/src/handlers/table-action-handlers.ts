/**
 * The raw table-source edit tool (POST /projects/{id}/tables/{tableId}/actions).
 *
 * One tool makes every in-place edit Studio takes at that endpoint — append, insert or delete rows or columns, update
 * a row, a column, a cell or a range, merge and unmerge cells — on the table's RAW source, whatever its type. It ends
 * as the full update tool does, with the id of the table after the edit and a read that recompiles it (see
 * `table-edit.ts`).
 */

import { ProtocolError, ProtocolErrorCode } from "@modelcontextprotocol/server";

import type { OpenLClient } from "../client.js";
import { isHttpError } from "../http.js";
import * as schemas from "../schemas.js";
import { gridOf, widthOf } from "../table-grid.js";
import type * as Types from "../types.js";
import { registerTool, type ToolResponse, TABLE_ID_ALIAS } from "./common.js";
import { finishTableEdit } from "./table-edit.js";

type EditArgs = schemas.TableEdit;

/** Whether the cell at a row and a column of the table is one a merged cell covers, beside its first one. */
type Covered = (row: number, column: number) => boolean;

/** The action Studio takes for an edit, the cells a merged cell covers told apart from empty ones. */
type Edit = (args: EditArgs, covered: Covered) => Types.RawTableSourceAction;

// The schema has checked that every argument an action needs is there
/* eslint-disable @typescript-eslint/no-non-null-assertion */

/**
 * Lines of values as Studio takes them: a cell of each value, and a null where a merged cell covers the cell of
 * `line` and `index`, a covered cell, which Studio refuses a value for.
 */
function cellsOf(lines: EditArgs["cells"], covered: Covered = () => false): Types.RawCellInput[][] {
  return lines!.map((line, i) => line.map((value, j) => (value === null && covered(i, j) ? { covered: true } : { value })));
}

/** Rows or columns added: Studio takes them as one block, however many there are. */
function addLines(operation: "append" | "insert", type: "rows" | "columns"): Edit {
  return ({ cells, position }) => ({
    operation, target: { type, cells: cellsOf(cells), ...(position === undefined ? {} : { position }) },
  });
}

function deleteLines(type: "rows" | "columns"): Edit {
  return ({ position, count }) => ({ operation: "delete", target: { type, position, count: count ?? 1 } });
}

function updateLine(type: "row" | "column"): Edit {
  return ({ position, cells }, covered) => ({
    operation: "update",
    target: { type, position, cells: cellsOf(cells, (_, index) => (type === "row" ? covered(position!, index) : covered(index, position!)))[0] },
  });
}

const EDITS: Record<schemas.EditAction, Edit> = {
  append_rows: addLines("append", "rows"),
  append_columns: addLines("append", "columns"),
  insert_rows: addLines("insert", "rows"),
  insert_columns: addLines("insert", "columns"),
  delete_rows: deleteLines("rows"),
  delete_columns: deleteLines("columns"),
  update_row: updateLine("row"),
  update_column: updateLine("column"),
  // An Excel formula goes alone; otherwise the value is always sent, as null clears the cell
  update_cell: ({ row, column, value, formula }) => ({
    operation: "update", target: { type: "cell", row, column, ...(formula === undefined ? { value: value! } : { formula }) },
  }),
  update_range: ({ row, column, cells }, covered) => ({
    operation: "update",
    target: { type: "range", row, column, cells: cellsOf(cells, (line, index) => covered(row! + line, column! + index)) },
  }),
  merge: ({ row, column, rowspan, colspan }) => ({ operation: "merge", target: { type: "cells", row, column, rowspan, colspan } }),
  unmerge: ({ row, column }) => ({ operation: "unmerge", target: { type: "cells", row, column } }),
};

/* eslint-enable @typescript-eslint/no-non-null-assertion */

/** The edits that overwrite cells a merged cell may cover. */
const OVERWRITES: ReadonlySet<schemas.EditAction> = new Set(["update_row", "update_column", "update_range"]);

/** The edits that move cells or change which cells are merged. */
const RESHAPES: ReadonlySet<schemas.EditAction> = new Set([
  "insert_rows", "insert_columns", "delete_rows", "delete_columns", "merge", "unmerge",
]);

/**
 * The cells a merged cell of the table covers, read when an edit overwrites cells with a null that may be one of them,
 * for the null to keep the cell covered.
 */
async function coveredCells(
  client: OpenLClient,
  args: { projectId: string; tableId: string },
  edits: EditArgs[],
): Promise<Covered> {
  if (!edits.some((edit) => OVERWRITES.has(edit.action) && edit.cells?.some((line) => line.includes(null)))) {
    return () => false;
  }
  const { source } = await client.getTable(args.projectId, args.tableId);
  return (row, column) => source[row]?.[column]?.covered === true;
}

/** The most cells a refused edit names the values of. */
const NAMED_CELLS = 20;

/** The edits whose cells a refusal names: the others insert, delete or move cells, or overwrite a whole column. */
const NAMED_EDITS: ReadonlySet<schemas.EditAction> = new Set(["update_cell", "update_row", "update_range"]);

/** The cells an edit writes at positions of the table as it is, the cells of the rows it overwrites whole. */
/* eslint-disable @typescript-eslint/no-non-null-assertion -- the schema has checked the arguments of each action */
function cellsOfEdit(edit: EditArgs, width: number): Array<[number, number]> {
  switch (edit.action) {
    case "update_cell":
      return [[edit.row!, edit.column!]];
    case "update_row":
      return Array.from({ length: width }, (_, column) => [edit.position!, column]);
    case "update_range":
      return edit.cells!.flatMap((line, row) => line.map((_, column): [number, number] => [edit.row! + row, edit.column! + column]));
    default:
      return [];
  }
}
/* eslint-enable @typescript-eslint/no-non-null-assertion */

/**
 * The refusal of an edit of a table changed since it was read, with the version the table has now and what the cells
 * the edit names hold now, until an edit moves cells: the agent repeats the edit, or not, without reading it again.
 * When those cells are not all the edit changes, the agent reads the table again: a row it deletes may have moved.
 */
async function changedSince(
  client: OpenLClient,
  args: { projectId: string; tableId: string },
  edits: EditArgs[],
  reshaped: number,
): Promise<ProtocolError> {
  const refusal = "The table changed since it was read, on disk or through another client, and nothing was written.";
  const table = await client.getTable(args.projectId, args.tableId).catch(() => undefined);
  if (!table) {
    return new ProtocolError(ProtocolErrorCode.InvalidRequest, `${refusal} Read the table again and repeat the edit.`);
  }
  const { rows } = gridOf(table.source, 0, table.pos);
  const cells = edits.slice(0, reshaped).flatMap((edit) => cellsOfEdit(edit, widthOf(table.pos)));
  const now = cells.slice(0, NAMED_CELLS)
    .map(([row, column]) => `[${row}, ${column}] ${JSON.stringify(rows[row]?.[column] ?? null)}`).join(", ");
  const more = cells.length > NAMED_CELLS ? ` and ${cells.length - NAMED_CELLS} more` : "";
  const named = now ? `, and the cells the edit names hold ${now}${more}` : "";
  const whole = reshaped === edits.length && cells.length <= NAMED_CELLS && edits.every(({ action }) => NAMED_EDITS.has(action));
  let next = "repeat the edit with that version if it still applies.";
  if (!whole) {
    const partly = now ? "they are not all the edit changes, and " : "";
    next = `${partly}rows or columns may have moved: read the table again before repeating the edit.`;
  }
  return new ProtocolError(ProtocolErrorCode.InvalidRequest, `${refusal} Its version now is '${table.version}'${named}: ${next}`);
}

export function registerTableActionHandlers(): void {
  registerTool({
    name: "edit_table",
    title: "Edit Table (raw)",
    description:
      "Edit a table's raw source in place, whatever its type, by an 'action', or several in 'actions':\n" +
      "- append_rows / append_columns: add the lines of 'cells' at the end.\n" +
      "- insert_rows / insert_columns: add the lines of 'cells' at 'position', 1 to the height or width; the rows " +
      "below or the columns to the right shift.\n" +
      "- delete_rows / delete_columns: remove 'count' lines, 1 by default, from 'position', 1 or more.\n" +
      "- update_row / update_column: overwrite the line at 'position' with the one line of 'cells'.\n" +
      "- update_cell: set the cell at ('row', 'column') to 'value', null clearing it, or to the Excel 'formula'.\n" +
      "- update_range: overwrite the cells from ('row', 'column') with the lines of 'cells', more than one cell.\n" +
      "- merge: merge 'rowspan' × 'colspan' cells from ('row', 'column'); unmerge: split the merged cell covering " +
      "('row', 'column').\n" +
      "Positions count from 0 in the whole table, as openl_get_table gives them: row 0 is the header, column 0 the " +
      "leading labels. A line holds one value per column of a row, or per row of a column, as the rows of " +
      "openl_get_table hold them: a scalar or a one-dimensional array of scalars, null for an empty cell or one a " +
      "merged cell covers, which stays covered. A value starting with '=' is an OpenL formula, written as text; " +
      "update_cell writes an Excel 'formula', such as '=B2*C2', instead of a value, never beside one. " +
      "'actions' makes several edits as one change and one write, in order, each on the table as the previous one " +
      "left it, so an insert shifts the positions after it; when one is refused nothing is written. " +
      "'version', from openl_get_table or the previous edit, is required: Studio refuses an edit of a table changed " +
      "since and writes nothing. The answer gives the current 'tableId' and the new 'version' of the table, and " +
      "'compiles: false' with its 'errors' when the table no longer compiles.",
    schema: schemas.editTableSchema,
    aliases: TABLE_ID_ALIAS,
    annotations: { destructiveHint: true, openWorldHint: true },
    compiled: true,
    handler: async (args, client): Promise<ToolResponse> => {
      // The schema has checked that an edit without 'actions' names its action
      const edits = args.actions ?? [args as EditArgs];
      const covered = await coveredCells(client, args, edits);
      // The merged cells of the table as it was read hold until an edit moves or merges cells
      const found = edits.findIndex(({ action }) => RESHAPES.has(action));
      const reshaped = found < 0 ? edits.length : found;
      const actions = edits.map((edit, index) => EDITS[edit.action](edit, index <= reshaped ? covered : () => false));
      const reportedId = await (actions.length === 1
        ? client.editTableSource(args.projectId, args.tableId, actions[0], args.version)
        : client.editTableSourceBatch(args.projectId, args.tableId, actions, args.version))
        .catch(async (error: unknown) => {
          throw isHttpError(error) && error.response?.status === 412 ? await changedSince(client, args, edits, reshaped) : error;
        });
      return finishTableEdit(client, args, reportedId, "edit");
    },
  });
}
