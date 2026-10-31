/**
 * Integration tests of the table tools of src/handlers/table-handlers.ts: listing and reading tables, creating one,
 * replacing or appending to its raw source, and deleting it.
 */

import { describe, it, expect } from "vitest";
import { ProtocolErrorCode } from "@modelcontextprotocol/server";
import { executeTool } from "./index.js";
import { bodyOf } from "../testing/mock-http.js";
import { pageOf } from "../testing/openl-api-mocks.js";
import { dataOf, harness, textOf, useToolHarness } from "../testing/harness.js";

useToolHarness();

const tablesPath = "/projects/p1/tables";
const tablePath = (tableId: string): string => `${tablesPath}/${tableId}`;

/** A raw table of project p1 as `openl_get_table` returns it. */
const rawTable = (tableId: string, source: object[][] = [[{ value: "Rules void calc()" }]], extra: object = {}) => ({
  id: tableId, name: "calc", tableType: "RawSource", kind: "Rules", source, version: "v1", ...extra,
});

/** No request of any method reached OpenL Studio. */
const expectNoRequest = (): void => expect(harness.mockHttp.requests).toHaveLength(0);

describe("reading tables", () => {
  it("openl_list_tables pages the tables Studio pages, also from an offset off the page grid", async () => {
    harness.mockHttp.onGet(tablesPath, { params: { offset: 25, size: 50 } }).reply(200, pageOf(
      [{ id: "t26", name: "calculatePremium", tableType: "SimpleRules", kind: "Rules", module: "Main", project: "Rates", projectId: "p1" }],
      { total: 26 },
    ));

    const result = await executeTool("list_tables", {
      projectId: "p1", limit: 50, offset: 25, }, harness.client);

    // The project the caller named is not repeated in each table
    expect(textOf(result)).toBe("26 tables; 26–26 below.\n\n| id | name | kind | module |\n| --- | --- | --- | --- |\n"
      + "| t26 | calculatePremium | Rules | Main |");
  });

  it("openl_list_tables finds the tables a text is in, and names the project of a table of a dependency", async () => {
    harness.mockHttp.onGet(tablesPath).reply(200, pageOf([
      { id: "t1", name: "Messages", tableType: "SimpleRules", kind: "Rules", project: "Rates", projectId: "p1" },
      { id: "t9", name: "Codes", tableType: "Data", kind: "Data", project: "Common", projectId: "p9" },
    ], { total: 2 }));

    const result = await executeTool("list_tables", {
      projectId: "p1", kind: "Rules", text: "BaseRate", header: "Policy", module: "Lookups", scope: "all", sort: "position",
    }, harness.client);

    // A single kind is taken as a list of one
    expect(harness.mockHttp.history.get[0].params).toMatchObject({
      kind: ["Rules"], text: "BaseRate", header: "Policy", module: "Lookups", scope: "all", sort: "position",
    });
    expect(textOf(result)).toContain("| id | name | kind | module | project |");
    expect(textOf(result)).toContain("| t1 | Messages | Rules |  |  |\n| t9 | Codes | Data |  | Common (p9) |");
  });

  it("openl_get_table forwards offset/limit/styles to the raw view query and answers the rows, the styles apart", async () => {
    harness.mockHttp.onGet(tablePath("calculatePremium_1234")).reply(200, rawTable("calculatePremium_1234",
      [[{ cell: "A3", value: "Age", style: { bold: true, background: "#ccccff" } }]], { totalRows: 120 }));

    const result = await executeTool("get_table", {
      projectId: "p1",
      tableId: "calculatePremium_1234",
      offset: 2,
      limit: 1,
      styles: true,
    }, harness.client);

    expect(harness.mockHttp.history.get[0].params).toEqual({ raw: true, startRow: 2, maxRows: 1, cutMerges: true, styles: true });
    // The window tells the rows, which the data does not repeat; a style is named by the position of its cell
    expect(dataOf(result)).toEqual({
      id: "calculatePremium_1234", name: "calc", kind: "Rules", version: "v1",
      rows: [["Age"]], styles: [[2, 0, { bold: true, background: "#ccccff" }]],
    });
  });

  it("openl_get_table answers merged cells, formulas and notes by their positions in the table", async () => {
    harness.mockHttp.onGet(tablePath("t1")).reply(200, rawTable("t1", [
      [{ cell: "B2", value: "Rules Double rate(Integer age)", colspan: 3 }, { covered: true }, { covered: true }],
      [{ cell: "B3", value: "Age" }, { cell: "C3", value: "Rate", comment: "per year" }, {}],
      [{ cell: "B4", value: 25 }, { cell: "C4", value: 1.5 }, { cell: "D4", value: 3, formula: "=C4*2" }],
    ], { pos: "B2:D4" }));

    const result = await executeTool("get_table", { projectId: "p1", tableId: "t1" }, harness.client);

    expect(dataOf(result)).toMatchObject({
      rows: [["Rules Double rate(Integer age)"], ["Age", "Rate"], [25, 1.5, 3]],
      merges: [[0, 0, 1, 3]],
      formulas: [[2, 2, "=C4*2"]],
      notes: [[1, 1, "per year"]],
    });
  });

  /** Rows of five cells, as many as `count`, from row `first` of the sheet. */
  const rows = (count: number, first = 3): object[][] => Array.from({ length: count }, (_, row) =>
    ["B", "C", "D", "E", "F"].map((column) => ({ cell: `${column}${first + row}`, value: `value ${row} ${column}` })));

  it("openl_get_table returns a table that fits the limit without indentation whole (EPBDS-16853)", async () => {
    harness.mockHttp.onGet(tablePath("t1")).reply(200, rawTable("t1", rows(133)));

    const result = await executeTool("get_table", { projectId: "p1", tableId: "t1" }, harness.client);

    expect(textOf(result).length).toBeLessThanOrEqual(25_000);
    const response = JSON.parse(textOf(result));
    expect(response.truncated).toBeUndefined();
    expect(response.data.rows).toHaveLength(133);
    expect(response.window).toMatchObject({ rows: 133, totalRows: 133, complete: true });
  });

  it.each([
    ["the whole table", undefined, 0],
    ["a window", 40, 40],
  ])("openl_get_table cuts %s over the limit by whole rows and names the row to continue from (EPBDS-16853)",
    async (_case, offset, first) => {
      harness.mockHttp.onGet(tablePath("t1")).reply(200, rawTable("t1", rows(600), offset ? { totalRows: 700 } : {}));

      const result = await executeTool("get_table", {
        projectId: "p1", tableId: "t1", offset, }, harness.client);

      expect(textOf(result).length).toBeLessThanOrEqual(25_000);
      const response = JSON.parse(textOf(result));
      const delivered = response.data.rows.length;
      expect(delivered).toBeGreaterThan(0);
      expect(delivered).toBeLessThan(600);
      expect(response.data.rows.at(-1)).toHaveLength(5);
      expect(response.truncated).toBe(true);
      expect(response.truncation_message).toBe(`Response truncated due to size. It holds ${delivered} of 600 rows; `
        + `continue with offset=${first + delivered}.`);
      expect(response.window).toEqual({
        offset: first, rows: delivered, totalRows: offset ? 700 : 600, complete: false, next_offset: first + delivered,
      });
    });

  it.each([
    ["a table read whole", {}, 5, undefined,
      { offset: 0, rows: 5, totalRows: 5, complete: true }],
    ["a window larger than the table", { offset: 0, limit: 100 }, 5, undefined,
      { offset: 0, rows: 5, totalRows: 5, complete: true }],
    ["a window reaching the end", { offset: 3, limit: 10 }, 2, 5,
      { offset: 3, rows: 2, totalRows: 5, complete: false }],
    ["a window inside the table", { offset: 100, limit: 2 }, 2, 133,
      { offset: 100, rows: 2, totalRows: 133, complete: false, next_offset: 102 }],
    ["a window past the end", { offset: 10 }, 0, 5,
      { offset: 10, rows: 0, totalRows: 5, complete: false }],
  ])("openl_get_table states the rows of %s (EPBDS-16851)", async (_case, window, returned, totalRows, expected) => {
    harness.mockHttp.onGet(tablePath("t1")).reply(200, rawTable("t1", rows(returned), totalRows ? { totalRows } : {}));

    const result = await executeTool("get_table", {
      projectId: "p1", tableId: "t1", ...window, }, harness.client);

    expect(JSON.parse(textOf(result)).window).toEqual(expected);
  });
});

describe("openl_get_table find", () => {
  it("reads the header rows and the rows with a cell holding the text, a number by its digits, at their positions", async () => {
    harness.mockHttp.onGet(tablePath("t1")).reply(200, rawTable("t1", [
      [{ value: "SmartLookup Double RegionFactor(String region, String plan)", colspan: 3 }, { covered: true }, { covered: true }],
      [{ value: "Region" }, { value: "Silver" }, { value: "Gold" }],
      [{ value: "north" }, { value: 1.1 }, { value: 1.2 }],
      [{ value: 42 }, { value: 1.3 }, { value: 1.4, formula: "=1+0.4" }],
      [{ value: "south" }, { value: 1.5 }, { value: 1.6 }],
    ], { pos: "B2:D6", headerHeight: 1 }));

    const response = JSON.parse(textOf(await executeTool("get_table", { projectId: "p1", tableId: "t1", find: "42" }, harness.client)));

    expect(response.found).toEqual({ rows: 3, totalRows: 5 });
    expect(response.data).toMatchObject({
      rows: [["SmartLookup Double RegionFactor(String region, String plan)"], ["Region", "Silver", "Gold"], [42, 1.3, 1.4]],
      rowIndexes: [0, 1, 3],
      merges: [[0, 0, 1, 3]],
      formulas: [[3, 2, "=1+0.4"]],
    });
    // The whole table is read to find the rows
    expect(harness.mockHttp.history.get[0].params).toEqual({ raw: true, cutMerges: true });
  });

  it("refuses a find in a slice before reading", async () => {
    await expect(executeTool("get_table", { projectId: "p1", tableId: "t1", find: "42", limit: 10 }, harness.client))
      .rejects.toThrow(/find searches the whole table/);
    expectNoRequest();
  });
});

describe("openl_create_project_table", () => {
  it("writes the rows, merges and formulas as Studio's cells and answers the version of the new table", async () => {
    harness.mockHttp.onPost(tablesPath).reply(201, { id: "t1", name: "MinimumPremium", tableType: "RawSource", file: "Main.xlsx" });
    harness.mockHttp.onGet(tablePath("t1")).reply(200, rawTable("t1"));

    const result = await executeTool("create_project_table", {
      projectId: "p1",
      module: "Main",
      name: "MinimumPremium",
      rows: [["SmartRules Double MinimumPremium(String state)"], ["State", "Minimum"], ["CA", 50, 0]],
      merges: [[0, 0, 1, 3]],
      formulas: [[2, 2, "=C4*2"]],
    }, harness.client);

    expect(bodyOf(harness.mockHttp.history.post[0])).toEqual({
      moduleName: "Main",
      table: {
        tableType: "RawSource",
        name: "MinimumPremium",
        source: [
          [{ value: "SmartRules Double MinimumPremium(String state)", colspan: 3 }, { covered: true }, { covered: true }],
          [{ value: "State" }, { value: "Minimum" }, { value: null }],
          [{ value: "CA" }, { value: 50 }, { formula: "=C4*2" }],
        ],
      },
    });
    expect(dataOf(result)).toEqual({ tableId: "t1", version: "v1" });
  });

  it.each([
    ["the rows sent as a JSON string", '[["Datatype X"]]', undefined],
    ["rows sent as a string that is no JSON", '[["Datatype X"]', /not valid JSON/],
    ["rows sent as a word", "Datatype X", /Invalid arguments for create_project_table/],
  ])("takes %s", async (_case, rows, refusal) => {
    harness.mockHttp.onPost(tablesPath).reply(201, { id: "t1" });
    harness.mockHttp.onGet(tablePath("t1")).reply(200, rawTable("t1"));

    const call = executeTool("create_project_table", { projectId: "p1", module: "Main", name: "X", rows }, harness.client);

    if (refusal) {
      await expect(call).rejects.toThrow(refusal);
      expectNoRequest();
    } else {
      await call;
      expect(bodyOf(harness.mockHttp.history.post[0]).table.source).toEqual([[{ value: "Datatype X" }]]);
    }
  });
});

describe("openl_update_table", () => {
  it("replaces the table with the version it read, filling the columns a row leaves out, and recompiles it", async () => {
    harness.mockHttp.onGet(tablePath("t1")).reply(200, rawTable("t1", [[{ value: "Rules void calc()" }]], { pos: "B2:D3", totalRows: 2, version: "v2" }));
    harness.mockHttp.onPut(tablePath("t1")).reply(204);

    const result = await executeTool("update_table", {
      projectId: "p1", tableId: "t1", version: "v1", rows: [["Rules void calc()"], ["a", 1]],
    }, harness.client);

    expect(harness.mockHttp.history.put[0].headers).toMatchObject({ "If-Match": "\"v1\"" });
    expect(bodyOf(harness.mockHttp.history.put[0])).toEqual({
      tableType: "RawSource",
      source: [
        [{ value: "Rules void calc()" }, { value: null }, { value: null }],
        [{ value: "a" }, { value: 1 }, { value: null }],
      ],
    });
    expect(dataOf(result)).toEqual({ tableId: "t1", version: "v2" });
    // The row-count probe before the PUT, and the recompile read after it
    expect(harness.mockHttp.history.get.map((request) => request.params)).toEqual([
      { raw: true, startRow: 0, maxRows: 1, cutMerges: true },
      { raw: true, startRow: 0, maxRows: 1, cutMerges: true },
    ]);
  });

  it("writes back a table as openl_get_table read it: multi-value cells, merges and formulas", async () => {
    harness.mockHttp.onGet(tablePath("t1")).reply(200, rawTable("t1", [
      [{ cell: "B2", value: "Data Rating ratings", colspan: 2 }, { covered: true }],
      [{ cell: "B3", value: "ratings" }, { cell: "C3", value: 2, formula: "=1+1" }],
      [{ cell: "B4", value: ["MA2", "FA+", "SPA"] }, {}],
    ], { pos: "B2:C4" }));
    harness.mockHttp.onPut(tablePath("t1")).reply(204);

    const read = dataOf<any>(await executeTool("get_table", { projectId: "p1", tableId: "t1" }, harness.client));
    await executeTool("update_table", {
      projectId: "p1", tableId: "t1", version: read.version, rows: read.rows, merges: read.merges, formulas: read.formulas,
    }, harness.client);

    expect(bodyOf(harness.mockHttp.history.put[0]).source).toEqual([
      [{ value: "Data Rating ratings", colspan: 2 }, { covered: true }],
      [{ value: "ratings" }, { formula: "=1+1" }],
      [{ value: ["MA2", "FA+", "SPA"] }, { value: null }],
    ]);
  });

  it("passes on Studio's refusal of a write over a table changed since it was read", async () => {
    harness.mockHttp.onGet(tablePath("t1")).reply(200, rawTable("t1"));
    harness.mockHttp.onPut(tablePath("t1")).reply(409, {
      code: "openl.error.412.table.changed.since.read.message",
      message: "The table changed since it was read, on disk or through another client.",
    });

    await expect(executeTool("update_table", {
      projectId: "p1", tableId: "t1", version: "v1", rows: [["Rules void calc()"]],
    }, harness.client)).rejects.toThrow(/The table changed since it was read/);
  });

  it("refuses fewer rows than the table has, which a slice would leave out, before writing", async () => {
    harness.mockHttp.onGet(tablePath("t1")).reply(200, rawTable("t1", undefined, { totalRows: 359 }));

    await expect(executeTool("update_table", {
      projectId: "p1", tableId: "t1", version: "v1", rows: Array.from({ length: 5 }, () => [null]),
    }, harness.client)).rejects.toMatchObject({
      code: ProtocolErrorCode.InvalidParams,
      message: expect.stringMatching(/current table has 359 row\(s\)/),
    });
    expect(harness.mockHttp.history.put).toHaveLength(0);
  });
});

describe("openl_get_table cut by rows", () => {
  it("keeps the whole area of a merged cell crossing the last row it delivers", async () => {
    // Column B of the first 400 rows is one merged cell, whose rows are long enough to need a cut
    const source = Array.from({ length: 400 }, (_, row) => [
      row === 0 ? { cell: "B4", value: "Dog", rowspan: 400 } : { covered: true },
      { cell: `C${row + 4}`, value: "x".repeat(80) },
    ]);
    harness.mockHttp.onGet(tablePath("t1")).reply(200, rawTable("t1", source, { pos: "B4:C403" }));

    const result = await executeTool("get_table", { projectId: "p1", tableId: "t1" }, harness.client);

    const response = JSON.parse(textOf(result));
    expect(response.data.rows.length).toBeLessThan(400);
    expect(response.data.rows[0]).toEqual(["Dog", "x".repeat(80)]);
    expect(response.data.rows[1]).toEqual([null, "x".repeat(80)]);
    expect(response.data.merges).toEqual([[0, 0, 400, 1]]);
  });

  it("names the whole area of a merged cell a slice Studio read cuts", async () => {
    harness.mockHttp.onGet(tablePath("t1")).reply(200, rawTable("t1",
      [[{ cell: "B104", value: "Dog", mergedArea: "B4:B403" }, { cell: "C104", value: 1 }]], { pos: "B2:C500", totalRows: 499 }));

    const result = await executeTool("get_table", { projectId: "p1", tableId: "t1", offset: 102, limit: 1 }, harness.client);

    expect(dataOf(result)).toMatchObject({ rows: [["Dog", 1]], merges: [[2, 0, 400, 1]] });
  });
});

describe("openl_delete_table", () => {
  it("deletes the table and names it", async () => {
    harness.mockHttp.onDelete(tablePath("t1")).reply(204);

    const result = await executeTool("delete_table", { projectId: "p1", tableId: "t1", version: "v1" }, harness.client);

    expect(harness.mockHttp.history.delete).toHaveLength(1);
    expect(dataOf(result)).toEqual({ deleted: "t1" });
  });

  it("refuses a deletion that names no version of the table before reaching the backend", async () => {
    await expect(executeTool("delete_table", { projectId: "p1", tableId: "t1" }, harness.client))
      .rejects.toThrow(/Invalid arguments for delete_table/);
    expect(harness.mockHttp.history.delete).toHaveLength(0);
  });

  it("deletes the version of the table it names", async () => {
    harness.mockHttp.onDelete(tablePath("t1")).reply(204);

    await executeTool("delete_table", { projectId: "p1", tableId: "t1", version: "v1" }, harness.client);

    expect(harness.mockHttp.history.delete[0].headers).toMatchObject({ "If-Match": "\"v1\"" });
  });
});
