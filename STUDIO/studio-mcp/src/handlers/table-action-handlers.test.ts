/**
 * Integration tests of the raw table-source action tools of src/handlers/table-action-handlers.ts: the action each tool
 * sends to `/tables/{id}/actions`, the validation before it, and the id of the table after the edit.
 */

import { describe, it, expect } from "vitest";
import { executeTool } from "./index.js";
import { bodyOf } from "../testing/mock-http.js";
import { dataOf, harness, textOf, useToolHarness } from "../testing/harness.js";

useToolHarness();

const tablePath = "/projects/p1/tables/t1";
const a = { value: "a" };
const b = { value: "b" };

describe("raw table-source actions", () => {
  it.each([
    ["append_table_rows", { cells: [[a, b]] }, { operation: "append", target: { type: "rows", cells: [[a, b]] } },
      "appended rows to"],
    ["append_table_columns", { cells: [[a, b]] }, { operation: "append", target: { type: "columns", cells: [[a, b]] } },
      "appended columns to"],
    // The studio has only a `rows` block target — one row is a one-element block.
    ["insert_table_rows", { position: 3, cells: [[a, b]] },
      { operation: "insert", target: { type: "rows", position: 3, cells: [[a, b]] } }, "inserted rows into"],
    ["insert_table_rows", { position: 2, cells: [[a], [b]] },
      { operation: "insert", target: { type: "rows", position: 2, cells: [[a], [b]] } }, "inserted rows into"],
    ["insert_table_columns", { position: 1, cells: [[a, b]] },
      { operation: "insert", target: { type: "columns", position: 1, cells: [[a, b]] } }, "inserted columns into"],
    // count omitted → defaults to 1 on the `rows` block target.
    ["delete_table_rows", { position: 4 }, { operation: "delete", target: { type: "rows", position: 4, count: 1 } },
      "deleted rows from"],
    ["delete_table_rows", { position: 3, count: 4 }, { operation: "delete", target: { type: "rows", position: 3, count: 4 } },
      "deleted rows from"],
    ["delete_table_columns", { position: 2, count: 2 },
      { operation: "delete", target: { type: "columns", position: 2, count: 2 } }, "deleted columns from"],
    ["update_table_row", { position: 1, cells: [a, b] }, { operation: "update", target: { type: "row", position: 1, cells: [a, b] } },
      "updated a row of"],
    ["update_table_column", { position: 2, cells: [a, b] },
      { operation: "update", target: { type: "column", position: 2, cells: [a, b] } }, "updated a column of"],
    // An explicit null clears the cell.
    ["update_table_cell", { row: 2, column: 1, value: null },
      { operation: "update", target: { type: "cell", row: 2, column: 1, value: null } }, "updated a cell of"],
    ["update_table_cell", { row: 3, column: 0, value: ["MA2", "FA+", "SPA"] },
      { operation: "update", target: { type: "cell", row: 3, column: 0, value: ["MA2", "FA+", "SPA"] } }, "updated a cell of"],
    ["merge_table_cells", { row: 1, column: 0, rowspan: 2, colspan: 3 },
      { operation: "merge", target: { type: "cells", row: 1, column: 0, rowspan: 2, colspan: 3 } }, "merged cells of"],
    ["unmerge_table_cells", { row: 1, column: 0 }, { operation: "unmerge", target: { type: "cells", row: 1, column: 0 } },
      "unmerged cells of"],
    ["update_table_range", { row: 1, column: 2, cells: [[a, b]] },
      { operation: "update", target: { type: "range", row: 1, column: 2, cells: [[a, b]] } }, "updated a range of"],
  ])("%s %j posts its action and reads the table back", async (tool, args, action, edit) => {
    harness.mockHttp.onPost(`${tablePath}/actions`).reply(204);
    harness.mockHttp.onGet(tablePath).reply(200, { id: "t1", name: "T", tableType: "RawSource", kind: "Other" });

    const result = await executeTool(tool, {
      projectId: "p1", tableId: "t1", version: "v1", ...args, response_format: "json",
    }, harness.client);

    expect(harness.mockHttp.history.post.map(bodyOf)).toEqual([action]);
    expect(dataOf(result)).toEqual({
      success: true,
      message: `Successfully ${edit} table t1`,
      recompileTriggered: true,
      tableId: "t1",
    });
  });

  it("edits the version of the table it names, and answers the version the edit left", async () => {
    harness.mockHttp.onPost(`${tablePath}/actions`).reply(204);
    harness.mockHttp.onGet(tablePath).reply(200, {
      id: "t1", name: "T", tableType: "RawSource", kind: "Other", source: [], version: "v2",
    });

    const result = await executeTool("update_table_cell", {
      projectId: "p1", tableId: "t1", row: 1, column: 1, value: "x", version: "v1", response_format: "json",
    }, harness.client);

    expect(harness.mockHttp.history.post[0].headers).toMatchObject({ "If-Match": "\"v1\"" });
    expect(dataOf(result)).toMatchObject({ success: true, version: "v2" });
  });

  it("refuses an edit that names no version of the table before reaching the backend", async () => {
    await expect(executeTool("update_table_cell", {
      projectId: "p1", tableId: "t1", row: 1, column: 1, value: "x",
    }, harness.client)).rejects.toThrow(/Invalid arguments for update_table_cell/);
    expect(harness.mockHttp.history.post).toHaveLength(0);
  });

  it("tells an applied edit after which the table does not compile, with its errors (EPBDS-16804)", async () => {
    harness.mockHttp.onPost(`${tablePath}/actions`).reply(204);
    harness.mockHttp.onGet(tablePath).reply(200, {
      id: "t1", name: "PolicyPremiumCalculation", tableType: "RawSource", kind: "Test", source: [],
      messages: [
        { severity: "ERROR", summary: "Expected one of the parameters of the method, but found 'P5'." },
        { severity: "WARN", summary: "A warning that does not stop the compilation." },
        { severity: "ERROR", summary: "Table '3229' is not found." },
      ],
    });

    const result = await executeTool("append_table_rows", {
      projectId: "p1", tableId: "t1", version: "v1", cells: [[a, b]], response_format: "json",
    }, harness.client);

    expect(dataOf(result)).toMatchObject({
      success: true,
      recompileTriggered: true,
      compiles: false,
      errors: ["Expected one of the parameters of the method, but found 'P5'.", "Table '3229' is not found."],
    });
    expect(dataOf(result).note).toContain("The table does NOT compile after the edit, which WAS applied");
  });

  it("reports the new table id and previousTableId when Studio answers a new id for the table", async () => {
    harness.mockHttp.onPost(`${tablePath}/actions`).reply(200, { id: "t1_renamed" });
    harness.mockHttp.onGet("/projects/p1/tables/t1_renamed").reply(200, { id: "t1_renamed", name: "T", tableType: "RawSource" });

    const result = await executeTool("delete_table_rows", {
      projectId: "p1", tableId: "t1", version: "v1", position: 5, response_format: "json",
    }, harness.client);

    expect(dataOf(result)).toMatchObject({ tableId: "t1_renamed", tableIdChanged: true, previousTableId: "t1" });
    expect(textOf(result)).toContain("now known by the id 't1_renamed'");
  });

  it.each([
    ["append_table_columns", "without cells", {}, /Invalid arguments for append_table_columns/],
    ["append_table_rows", "with a zero colspan", { cells: [[{ value: "a", colspan: 0 }]] }, /Invalid arguments for append_table_rows/],
    ["insert_table_rows", "without a position", { cells: [[a]] }, /Invalid arguments for insert_table_rows/],
    ["delete_table_rows", "at a negative position", { position: -1 }, /Invalid arguments for delete_table_rows/],
    ["delete_table_rows", "of the header row", { position: 0 }, /Invalid arguments for delete_table_rows/],
    ["update_table_cell", "without a value", { row: 2, column: 1 }, /Invalid arguments for update_table_cell/],
    ["merge_table_cells", "of one cell", { row: 0, column: 0, rowspan: 1, colspan: 1 }, /more than one cell/],
    ["update_table_range", "of one cell", { row: 0, column: 0, cells: [[{ value: "x" }]] }, /more than one cell/],
  ])("%s refuses an edit %s before reaching the backend", async (tool, _case, args, message) => {
    await expect(executeTool(tool, { projectId: "p1", tableId: "t1", version: "v1", ...args }, harness.client))
      .rejects.toThrow(message);
    expect(harness.mockHttp.history.post).toHaveLength(0);
  });
});
