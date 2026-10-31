/**
 * Integration tests of the raw table-source edit tool of src/handlers/table-action-handlers.ts: the action each edit
 * sends to `/tables/{id}/actions`, the validation before it, and the id of the table after the edit.
 */

import { describe, it, expect } from "vitest";
import { executeTool } from "./index.js";
import { bodyOf } from "../testing/mock-http.js";
import { dataOf, harness, textOf, useToolHarness } from "../testing/harness.js";

useToolHarness();

const tablePath = "/projects/p1/tables/t1";
// A line of the edit holds values, which Studio takes as cells
const a = { value: "a" };
const b = { value: "b" };

describe("raw table-source edits", () => {
  it.each([
    ["append_rows", { cells: [["a", "b"]] }, { operation: "append", target: { type: "rows", cells: [[a, b]] } }],
    ["append_columns", { cells: [["a", "b"]] }, { operation: "append", target: { type: "columns", cells: [[a, b]] } }],
    // The studio has only a `rows` block target — one row is a one-element block.
    ["insert_rows", { position: 3, cells: [["a", "b"]] },
      { operation: "insert", target: { type: "rows", position: 3, cells: [[a, b]] } }],
    ["insert_rows", { position: 2, cells: [["a"], ["b"]] },
      { operation: "insert", target: { type: "rows", position: 2, cells: [[a], [b]] } }],
    ["insert_columns", { position: 1, cells: [["a", "b"]] },
      { operation: "insert", target: { type: "columns", position: 1, cells: [[a, b]] } }],
    // count omitted → defaults to 1 on the `rows` block target.
    ["delete_rows", { position: 4 }, { operation: "delete", target: { type: "rows", position: 4, count: 1 } }],
    ["delete_rows", { position: 3, count: 4 }, { operation: "delete", target: { type: "rows", position: 3, count: 4 } }],
    ["delete_columns", { position: 2, count: 2 },
      { operation: "delete", target: { type: "columns", position: 2, count: 2 } }],
    ["update_row", { position: 1, cells: [["a", "b"]] }, { operation: "update", target: { type: "row", position: 1, cells: [a, b] } }],
    ["update_column", { position: 2, cells: [["a", "b"]] },
      { operation: "update", target: { type: "column", position: 2, cells: [a, b] } }],
    // An explicit null clears the cell.
    ["update_cell", { row: 2, column: 1, value: null },
      { operation: "update", target: { type: "cell", row: 2, column: 1, value: null } }],
    ["update_cell", { row: 3, column: 0, value: ["MA2", "FA+", "SPA"] },
      { operation: "update", target: { type: "cell", row: 3, column: 0, value: ["MA2", "FA+", "SPA"] } }],
    // An Excel formula is sent alone, without a value.
    ["update_cell", { row: 2, column: 1, formula: "=B2*2" },
      { operation: "update", target: { type: "cell", row: 2, column: 1, formula: "=B2*2" } }],
    // A null clears a cell no merged cell covers
    ["update_row", { position: 1, cells: [["a", null]] },
      { operation: "update", target: { type: "row", position: 1, cells: [{ value: "a" }, { value: null }] } }],
    ["merge", { row: 1, column: 0, rowspan: 2, colspan: 3 },
      { operation: "merge", target: { type: "cells", row: 1, column: 0, rowspan: 2, colspan: 3 } }],
    ["unmerge", { row: 1, column: 0 }, { operation: "unmerge", target: { type: "cells", row: 1, column: 0 } }],
    ["update_range", { row: 1, column: 2, cells: [["a", "b"]] },
      { operation: "update", target: { type: "range", row: 1, column: 2, cells: [[a, b]] } }],
  ])("%s %j posts its action and reads the table back", async (editAction, args, action) => {
    harness.mockHttp.onPost(`${tablePath}/actions`).reply(204);
    harness.mockHttp.onGet(tablePath).reply(200, { id: "t1", name: "T", tableType: "RawSource", kind: "Other", source: [] });

    const result = await executeTool("edit_table", {
      projectId: "p1", tableId: "t1", version: "v1", action: editAction, ...args, }, harness.client);

    expect(harness.mockHttp.history.post.map(bodyOf)).toEqual([action]);
    expect(dataOf(result)).toEqual({ tableId: "t1" });
  });

  it.each([
    ["update_row", { position: 0, cells: [["Rules void calc()", null, null]] }, [[0, 1], [0, 2]]],
    ["update_column", { position: 2, cells: [[null, "x"]] }, [[0, 2]]],
    ["update_range", { row: 0, column: 1, cells: [[null], ["y"]] }, [[0, 1]]],
  ])("%s keeps covered a cell a merged cell covers, where it writes a null", async (action, args, covered) => {
    harness.mockHttp.onPost(`${tablePath}/actions`).reply(204);
    harness.mockHttp.onGet(tablePath).reply(200, {
      id: "t1", name: "T", tableType: "RawSource", pos: "B2:D3", source: [
        [{ value: "Rules void calc()", colspan: 3 }, { covered: true }, { covered: true }],
        [{ value: "a" }, { value: 1 }, { value: 2 }],
      ],
    });

    await executeTool("edit_table", { projectId: "p1", tableId: "t1", version: "v1", action, ...args }, harness.client);

    const sent = bodyOf(harness.mockHttp.history.post[0]).target.cells;
    const cells: Array<{ covered?: boolean }> = Array.isArray(sent[0]) ? sent.flat() : sent;
    expect(cells.filter((cell) => cell.covered)).toHaveLength(covered.length);
  });

  it("edits the version of the table it names, and answers the version the edit left", async () => {
    harness.mockHttp.onPost(`${tablePath}/actions`).reply(204);
    harness.mockHttp.onGet(tablePath).reply(200, {
      id: "t1", name: "T", tableType: "RawSource", kind: "Other", source: [], version: "v2",
    });

    const result = await executeTool("edit_table", {
      projectId: "p1", tableId: "t1", action: "update_cell", row: 1, column: 1, value: "x", version: "v1", }, harness.client);

    expect(harness.mockHttp.history.post[0].headers).toMatchObject({ "If-Match": "\"v1\"" });
    expect(dataOf(result)).toMatchObject({ version: "v2" });
  });

  it("refuses an edit that names no version of the table before reaching the backend", async () => {
    await expect(executeTool("edit_table", {
      projectId: "p1", tableId: "t1", action: "update_cell", row: 1, column: 1, value: "x",
    }, harness.client)).rejects.toThrow(/Invalid arguments for edit_table/);
    expect(harness.mockHttp.history.post).toHaveLength(0);
  });

  it("tells an applied edit after which the table does not compile, with its errors (EPBDS-16804)", async () => {
    harness.mockHttp.onPost(`${tablePath}/actions`).reply(204);
    harness.mockHttp.onGet(tablePath).reply(200, {
      id: "t1", name: "PolicyPremiumCalculation", tableType: "RawSource", kind: "Test", source: [],
      messages: [
        { severity: "ERROR", summary: "Expected one of the parameters of the method, but found 'P5'.", location: { cell: "C7" } },
        { severity: "WARN", summary: "A warning that does not stop the compilation." },
        { severity: "ERROR", summary: "Table '3229' is not found." },
      ],
    });

    const result = await executeTool("edit_table", {
      projectId: "p1", tableId: "t1", version: "v1", action: "append_rows", cells: [["a", "b"]], }, harness.client);

    expect(dataOf(result)).not.toHaveProperty("recompileTriggered");
    expect(dataOf(result)).toMatchObject({
      compiles: false,
      errors: ["C7: Expected one of the parameters of the method, but found 'P5'.", "Table '3229' is not found."],
    });
    expect(dataOf(result).note).toContain("The table does NOT compile after the edit, which WAS applied");
  });

  it("reports the new table id and previousTableId when Studio answers a new id for the table", async () => {
    harness.mockHttp.onPost(`${tablePath}/actions`).reply(200, { id: "t1_renamed" });
    harness.mockHttp.onGet("/projects/p1/tables/t1_renamed").reply(200, { id: "t1_renamed", name: "T", tableType: "RawSource" });

    const result = await executeTool("edit_table", {
      projectId: "p1", tableId: "t1", version: "v1", action: "delete_rows", position: 5, }, harness.client);

    expect(dataOf(result)).toMatchObject({ tableId: "t1_renamed", previousTableId: "t1" });
    expect(textOf(result)).toContain("now known by the id 't1_renamed'");
  });

  it("tells an applied edit whose table cannot be read back", async () => {
    harness.mockHttp.onPost(`${tablePath}/actions`).reply(204);
    harness.mockHttp.onGet(tablePath).reply(503, { message: "Compilation model is unavailable" });

    const result = await executeTool("edit_table", {
      projectId: "p1", tableId: "t1", version: "v1", action: "append_rows", cells: [["a", "b"]], }, harness.client);

    expect(dataOf(result)).toMatchObject({ recompileTriggered: false });
    expect(dataOf(result).note).toContain("the edit WAS applied");
  });

  it.each([
    ["append_columns", "without cells", {}, /'append_columns' needs 'cells'/],
    ["append_rows", "of cell objects", { cells: [[{ value: "a" }]] }, /Cell value must be/],
    ["insert_rows", "without a position", { cells: [["a"]] }, /'insert_rows' needs 'position'/],
    ["delete_rows", "at a negative position", { position: -1 }, /position/],
    ["delete_rows", "of the header row", { position: 0 }, /'position' starts at 1/],
    ["insert_columns", "before the label column", { position: 0, cells: [["a"]] }, /'position' starts at 1/],
    ["delete_rows", "with cells it does not take", { position: 1, cells: [["a"]] }, /'delete_rows' does not take 'cells'/],
    ["update_row", "of two lines", { position: 1, cells: [["a"], ["b"]] }, /'update_row' takes one line of cells/],
    ["update_cell", "without a value", { row: 2, column: 1 }, /'update_cell' needs 'value'/],
    ["update_cell", "with both a value and a formula", { row: 2, column: 1, value: 4, formula: "=B2*2" },
      /either 'value' or 'formula'/],
    ["delete_rows", "with a formula it does not take", { position: 1, formula: "=B2*2" }, /'delete_rows' does not take 'formula'/],
    ["merge", "of one cell", { row: 0, column: 0, rowspan: 1, colspan: 1 }, /more than one cell/],
    ["update_range", "of one cell", { row: 0, column: 0, cells: [["x"]] }, /more than one cell/],
    ["rename", "of an unknown action", {}, /action/],
  ])("%s refuses an edit %s before reaching the backend", async (action, _case, args, message) => {
    await expect(executeTool("edit_table", { projectId: "p1", tableId: "t1", version: "v1", action, ...args },
      harness.client)).rejects.toThrow(message);
    expect(harness.mockHttp.history.post).toHaveLength(0);
  });
});
