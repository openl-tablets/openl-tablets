/**
 * Unit tests of src/table-grid.ts: a table between Studio's matrix of cells and the rows, merges and formulas the
 * table tools read and write.
 */

import { describe, it, expect } from "vitest";
import { gridOf, leadingRows, sourceOf, widthOf } from "./table-grid.js";

describe("table grid", () => {
  it("writes back what it reads, a vertical merge and a formula included", () => {
    const source = [
      [{ cell: "B2", value: "Rules Double rate(String zip)", colspan: 2 }, { covered: true }],
      [{ cell: "B3", value: "Zone", rowspan: 2 }, { cell: "C3", value: 1.5 }],
      [{ covered: true }, { cell: "C4", value: 3, formula: "=C3*2" }],
    ];

    const grid = gridOf(source, 0, "B2:C4");

    expect(grid).toEqual({
      rows: [["Rules Double rate(String zip)"], ["Zone", 1.5], [null, 3]],
      merges: [[0, 0, 1, 2], [1, 0, 2, 1]],
      formulas: [[2, 1, "=C3*2"]],
    });
    expect(sourceOf(grid, widthOf("B2:C4"))).toEqual([
      [{ value: "Rules Double rate(String zip)", colspan: 2 }, { covered: true }],
      [{ value: "Zone", rowspan: 2 }, { value: 1.5 }],
      [{ covered: true }, { formula: "=C3*2" }],
    ]);
  });

  it.each([
    ["below the rows", [5, 0, "=1"]],
    ["right of the columns", [0, 3, "=1"]],
  ] as const)("refuses a formula %s, which names no cell of the table", (_case, formula) => {
    expect(() => sourceOf({ rows: [["a", "b"], [1, 2]], formulas: [[...formula]] }, 3))
      .toThrow(`The formula at [${formula[0]}, ${formula[1]}] lies outside the table of 2 rows and 3 columns`);
  });

  it("keeps the positions of a slice those of the whole table", () => {
    const grid = gridOf([[{ value: "a", comment: "note" }, { value: 1, formula: "=1" }]], 40, "A1:B100");

    expect(grid).toEqual({ rows: [["a", 1]], formulas: [[40, 1, "=1"]], notes: [[40, 0, "note"]] });
  });

  it("leaves out with the rows it cuts what they hold, and keeps a merge reaching past the cut whole", () => {
    const grid = {
      rows: [["a"], ["b"], ["c"]],
      merges: [[10, 0, 3, 1] as [number, number, number, number], [12, 1, 1, 2] as [number, number, number, number]],
      formulas: [[12, 0, "=1"] as [number, number, string]],
    };

    expect(leadingRows(grid, 10, 2)).toEqual({ rows: [["a"], ["b"]], merges: [[10, 0, 3, 1]] });
  });

  it.each([
    ["B2:E920", 4],
    ["A1:AA3", 27],
    [undefined, 0],
  ])("reads the width of a table at %s", (pos, width) => {
    expect(widthOf(pos)).toBe(width);
  });
});
