/**
 * The raw table-source edit tool (POST /projects/{id}/tables/{tableId}/actions).
 *
 * One tool makes every in-place edit Studio takes at that endpoint — append, insert or delete rows or columns, update
 * a row, a column, a cell or a range, merge and unmerge cells — on the table's RAW source, whatever its type. It ends
 * as the full update tool does, with the id of the table after the edit and a read that recompiles it (see
 * `table-edit.ts`).
 */

import type { z } from "zod";

import * as schemas from "../schemas.js";
import type * as Types from "../types.js";
import { registerTool, type ToolResponse } from "./common.js";
import { finishTableEdit } from "./table-edit.js";

type EditArgs = z.output<typeof schemas.editTableSchema>;

/** An edit: what it did, for the answer, and the action Studio takes for it. */
interface Edit {
  pastTense: string;
  build: (args: EditArgs) => Types.RawTableSourceAction;
}

// The schema has checked that every argument an action needs is there
/* eslint-disable @typescript-eslint/no-non-null-assertion */

/** Rows or columns added: Studio takes them as one block, however many there are. */
function addLines(operation: "append" | "insert", type: "rows" | "columns", pastTense: string): Edit {
  return {
    pastTense,
    build: ({ cells, position }) => ({
      operation, target: { type, cells: cells!, ...(position === undefined ? {} : { position }) },
    }),
  };
}

function deleteLines(type: "rows" | "columns", pastTense: string): Edit {
  return { pastTense, build: ({ position, count }) => ({ operation: "delete", target: { type, position, count: count ?? 1 } }) };
}

function updateLine(type: "row" | "column", pastTense: string): Edit {
  return { pastTense, build: ({ position, cells }) => ({ operation: "update", target: { type, position, cells: cells![0] } }) };
}

const EDITS: Record<schemas.EditAction, Edit> = {
  append_rows: addLines("append", "rows", "appended rows to"),
  append_columns: addLines("append", "columns", "appended columns to"),
  insert_rows: addLines("insert", "rows", "inserted rows into"),
  insert_columns: addLines("insert", "columns", "inserted columns into"),
  delete_rows: deleteLines("rows", "deleted rows from"),
  delete_columns: deleteLines("columns", "deleted columns from"),
  update_row: updateLine("row", "updated a row of"),
  update_column: updateLine("column", "updated a column of"),
  update_cell: {
    pastTense: "updated a cell of",
    // An Excel formula goes alone; otherwise the value is always sent, as null clears the cell
    build: ({ row, column, value, formula }) => ({
      operation: "update", target: { type: "cell", row, column, ...(formula === undefined ? { value: value! } : { formula }) },
    }),
  },
  update_range: {
    pastTense: "updated a range of",
    build: ({ row, column, cells }) => ({ operation: "update", target: { type: "range", row, column, cells } }),
  },
  merge: {
    pastTense: "merged cells of",
    build: ({ row, column, rowspan, colspan }) => ({
      operation: "merge", target: { type: "cells", row, column, rowspan, colspan },
    }),
  },
  unmerge: {
    pastTense: "unmerged cells of",
    build: ({ row, column }) => ({ operation: "unmerge", target: { type: "cells", row, column } }),
  },
};

/* eslint-enable @typescript-eslint/no-non-null-assertion */

export function registerTableActionHandlers(): void {
  registerTool({
    name: "edit_table",
    title: "Edit Table (raw)",
    description:
      "Edit a table's raw source in place, whatever its type, by one 'action':\n" +
      "- append_rows / append_columns: add the lines of 'cells' at the end.\n" +
      "- insert_rows / insert_columns: add the lines of 'cells' at 'position', 1 to the height or width; the rows " +
      "below or the columns to the right shift.\n" +
      "- delete_rows / delete_columns: remove 'count' lines, 1 by default, from 'position', 1 or more.\n" +
      "- update_row / update_column: overwrite the line at 'position' with the one line of 'cells'.\n" +
      "- update_cell: set the cell at ('row', 'column') to 'value', null clearing it, or to the Excel 'formula'.\n" +
      "- update_range: overwrite the cells from ('row', 'column') with the lines of 'cells', more than one cell.\n" +
      "- merge: merge 'rowspan' × 'colspan' cells from ('row', 'column'); unmerge: split the merged cell covering " +
      "('row', 'column').\n" +
      "Positions count from 0 in the whole table: row 0 is the header, column 0 the leading labels, and row r of a " +
      "slice openl_get_table read is row window.startRow + r. A line holds one cell per column of " +
      "a row, or per row of a column: { value: null } for a blank cell, { covered: true } where a merged cell covers " +
      "the position, as openl_get_table marks it. A value is a scalar or a one-dimensional array of scalars; one " +
      "starting with '=' is an OpenL formula, written as text. A 'formula', such as '=B2*C2', writes an Excel formula " +
      "instead of a value, never beside one. " +
      "'version', from openl_get_table or the previous edit, is required: Studio refuses an edit of a table changed " +
      "since and writes nothing. The answer gives the current 'tableId' and the new 'version' of the table, and " +
      "'compiles: false' with its 'errors' when the table no longer compiles.",
    schema: schemas.editTableSchema,
    annotations: { destructiveHint: true, openWorldHint: true },
    compiled: true,
    handler: async (args, client): Promise<ToolResponse> => {
      const edit = EDITS[args.action];
      const reportedId = await client.editTableSource(args.projectId, args.tableId, edit.build(args), args.version);
      return finishTableEdit(client, args, reportedId, `Successfully ${edit.pastTense} table ${args.tableId}`, "edit");
    },
  });
}
