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
      [{ id: "t26", name: "calculatePremium", tableType: "SimpleRules", kind: "Rules" }],
      { total: 26 },
    ));

    const result = await executeTool("list_tables", {
      projectId: "p1", limit: 50, offset: 25, }, harness.client);

    const response = JSON.parse(textOf(result));
    expect(response.data.map((table: { id: string }) => table.id)).toEqual(["t26"]);
    expect(response.pagination).toEqual({ limit: 50, offset: 25, total_count: 26, has_more: false });
  });

  it("openl_get_table forwards startRow/maxRows/styles to the raw view query and returns the slice", async () => {
    harness.mockHttp.onGet(tablePath("calculatePremium_1234")).reply(200, rawTable("calculatePremium_1234",
      [[{ cell: "A3", value: "Age", style: { bold: true, background: "#ccccff" } }]], { totalRows: 120 }));

    const result = await executeTool("get_table", {
      projectId: "p1",
      tableId: "calculatePremium_1234",
      startRow: 2,
      maxRows: 1,
      styles: true,
    }, harness.client);

    expect(harness.mockHttp.history.get[0].params).toEqual({ raw: true, startRow: 2, maxRows: 1, cutMerges: true, styles: true });
    expect(dataOf(result)).toMatchObject({
      totalRows: 120,
      source: [[{ cell: "A3", style: { background: "#ccccff" } }]],
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
    expect(response.data.source).toHaveLength(133);
    expect(response.data.totalRows).toBeUndefined();
  });

  it.each([
    ["the whole table", undefined, 0],
    ["a window", 40, 40],
  ])("openl_get_table cuts %s over the limit by whole rows and names the row to continue from (EPBDS-16853)",
    async (_case, startRow, first) => {
      harness.mockHttp.onGet(tablePath("t1")).reply(200, rawTable("t1", rows(600), startRow ? { totalRows: 700 } : {}));

      const result = await executeTool("get_table", {
        projectId: "p1", tableId: "t1", startRow, }, harness.client);

      expect(textOf(result).length).toBeLessThanOrEqual(25_000);
      const response = JSON.parse(textOf(result));
      const delivered = response.data.source.length;
      expect(delivered).toBeGreaterThan(0);
      expect(delivered).toBeLessThan(600);
      expect(response.data.source.at(-1)).toHaveLength(5);
      expect(response.data.totalRows).toBe(startRow ? 700 : 600);
      expect(response.truncated).toBe(true);
      expect(response.truncation_message).toBe(`Response truncated due to size. It holds ${delivered} of 600 rows; `
        + `continue with startRow=${first + delivered}.`);
      expect(response.data).not.toHaveProperty("truncated_json_preview");
      expect(response.window).toEqual({
        startRow: first, endRow: first + delivered - 1, rows: delivered, totalRows: startRow ? 700 : 600, complete: false,
        nextStartRow: first + delivered,
      });
    });

  it.each([
    ["a table read whole", {}, 5, undefined,
      { startRow: 0, endRow: 4, rows: 5, totalRows: 5, complete: true }],
    ["a window larger than the table", { startRow: 0, maxRows: 100 }, 5, undefined,
      { startRow: 0, endRow: 4, rows: 5, totalRows: 5, complete: true }],
    ["a window reaching the end", { startRow: 3, maxRows: 10 }, 2, 5,
      { startRow: 3, endRow: 4, rows: 2, totalRows: 5, complete: false }],
    ["a window inside the table", { startRow: 100, maxRows: 2 }, 2, 133,
      { startRow: 100, endRow: 101, rows: 2, totalRows: 133, complete: false, nextStartRow: 102 }],
    ["a window past the end", { startRow: 10 }, 0, 5,
      { startRow: 10, rows: 0, totalRows: 5, complete: false }],
  ])("openl_get_table states the rows of %s (EPBDS-16851)", async (_case, window, returned, totalRows, expected) => {
    harness.mockHttp.onGet(tablePath("t1")).reply(200, rawTable("t1", rows(returned), totalRows ? { totalRows } : {}));

    const result = await executeTool("get_table", {
      projectId: "p1", tableId: "t1", ...window, }, harness.client);

    expect(JSON.parse(textOf(result)).window).toEqual(expected);
  });
});

describe("structured table payloads", () => {
  it("openl_create_project_table sends a complete RawSource table", async () => {
    harness.mockHttp.onPost(tablesPath).reply(201, { id: "t1", name: "LoanApplication", tableType: "RawSource", file: "Main.xlsx" });
    const table = { tableType: "RawSource", kind: "Datatype", name: "LoanApplication", source: [[{ value: "Datatype LoanApplication" }]] };

    const result = await executeTool("create_project_table", {
      projectId: "p1",
      moduleName: "Main",
      table,
    }, harness.client);

    expect(bodyOf(harness.mockHttp.history.post[0])).toEqual({ moduleName: "Main", table });
    expect(dataOf(result)).toMatchObject({
      success: true,
      tableId: "t1",
      message: "Successfully created RawSource table 'LoanApplication' in module 'Main'",
    });
  });

  it.each([
    ["create_project_table", "an unknown tableType", { table: { tableType: "frobnicate", name: "X" } }],
    ["create_project_table", "a typed table DTO", {
      table: { tableType: "SimpleRules", name: "CreditCategory", signature: "String CreditCategory(Integer creditScore)", rules: [] },
    }],
    ["create_project_table", "no tableType", { table: { name: "X", fields: [] } }],
    ["append_table", "no tableType", { appendData: { rules: [{ "Commission Type": "UDI", "Partner Code": "CIDP_CL_FB" }] } }],
    ["append_table", "a miscased RawSource", { appendData: { tableType: "rawsource", rows: [[{ value: 1 }]] } }],
    ["append_table", "a typed Data append", { appendData: { tableType: "Data", rules: [{ a: 1 }] } }],
    ["append_table", "a typed Spreadsheet append", {
      appendData: { tableType: "Spreadsheet", rows: [{ name: "Step1" }, { name: "Step2" }], cells: [[{ value: "=1+1" }]] },
    }],
    ["append_table", "a typed Datatype append", {
      appendData: { tableType: "Datatype", fields: [{ name: "age", type: "Integer", requried: true }] },
    }],
    ["update_table", "a typed view", { view: { tableType: "SimpleRules", name: "calc", rules: [] } }],
  ])("%s refuses %s with the raw-only guidance, before any request", async (tool, _case, payload) => {
    await expect(executeTool(tool, { projectId: "p1", moduleName: "Main", tableId: "t1", ...payload }, harness.client))
      .rejects.toThrow(/tableType must be exactly "RawSource"\. Typed table DTOs are intentionally unsupported/);
    expectNoRequest();
  });

  it("openl_append_table accepts appendData sent as a JSON string and forwards a real object", async () => {
    harness.mockHttp.onGet(tablePath("t1")).reply(200, rawTable("t1"));
    harness.mockHttp.onPost(`${tablePath("t1")}/lines`).reply(200);

    // The whole payload arrives as a JSON *string*, not an object.
    const result = await executeTool("append_table", {
      projectId: "p1",
      tableId: "t1",
      appendData: '{"tableType":"RawSource","rows":[[{"value":2035},{"value":"01/01/2035"},{"value":1000}]]}',
      version: "v1",
    }, harness.client);

    expect(textOf(result)).toContain("Successfully appended");
    // The stringified payload reached the backend as a parsed object, not a string literal.
    expect(bodyOf(harness.mockHttp.history.post[0])).toEqual({
      tableType: "RawSource",
      rows: [[{ value: 2035 }, { value: "01/01/2035" }, { value: 1000 }]],
    });
  });

  it("openl_update_table accepts view sent as a JSON string and forwards a real object", async () => {
    const view = rawTable("t1");
    harness.mockHttp.onGet(tablePath("t1")).reply(200, view);
    harness.mockHttp.onPut(tablePath("t1")).reply(204);

    const result = await executeTool("update_table", { projectId: "p1", tableId: "t1", view: JSON.stringify(view) }, harness.client);

    expect(textOf(result)).toContain("Successfully updated table");
    expect(bodyOf(harness.mockHttp.history.put[0])).toEqual(view);
  });

  it.each([
    ["append_table", "appendData", '{"tableType":"RawSource","rows":[', /not valid JSON/],
    ["update_table", "view", "SimpleRules", /Invalid arguments for update_table/],
  ])("%s refuses a %s string that is no JSON object, before any request", async (tool, payloadArg, value, message) => {
    await expect(executeTool(tool, { projectId: "p1", tableId: "t1", [payloadArg]: value }, harness.client)).rejects.toThrow(message);
    expectNoRequest();
  });
});

describe("openl_update_table", () => {
  it("replaces the table and reads it back to recompile it", async () => {
    const view = rawTable("Rules.xls_1234");
    harness.mockHttp.onPut(tablePath("Rules.xls_1234")).reply(204);
    harness.mockHttp.onGet(tablePath("Rules.xls_1234")).reply(200, view);

    const result = await executeTool("update_table", {
      projectId: "p1", tableId: "Rules.xls_1234", view, }, harness.client);

    expect(dataOf(result)).toEqual({
      success: true,
      message: "Successfully updated table Rules.xls_1234",
      recompileTriggered: true,
      tableId: "Rules.xls_1234",
      version: "v1",
    });
    // The row-count probe before the PUT, and the recompile read after it
    expect(harness.mockHttp.history.get.map((request) => request.params)).toEqual([
      { raw: true, startRow: 0, maxRows: 1, cutMerges: true },
      { raw: true, startRow: 0, maxRows: 1, cutMerges: true },
    ]);
  });

  it("replaces the version of the table it read, and answers the version the write left", async () => {
    harness.mockHttp.onPut(tablePath("t1")).reply(204);
    harness.mockHttp.onGet(tablePath("t1")).reply(200, rawTable("t1", undefined, { version: "v2" }));

    const result = await executeTool("update_table", {
      projectId: "p1", tableId: "t1", view: rawTable("t1", undefined, { version: "v1" }), }, harness.client);

    expect(harness.mockHttp.history.put[0].headers).toMatchObject({ "If-Match": "\"v1\"" });
    expect(dataOf(result)).toMatchObject({ success: true, tableId: "t1", version: "v2" });
  });

  it("passes on Studio's refusal of a write over a table changed since it was read", async () => {
    harness.mockHttp.onGet(tablePath("t1")).reply(200, rawTable("t1"));
    harness.mockHttp.onPut(tablePath("t1")).reply(409, {
      code: "openl.error.412.table.changed.since.read.message",
      message: "The table changed since it was read, on disk or through another client.",
    });

    await expect(executeTool("update_table", {
      projectId: "p1", tableId: "t1", view: rawTable("t1", undefined, { version: "v1" }),
    }, harness.client)).rejects.toThrow(/The table changed since it was read/);
  });

  it("keeps multi-value cells through a get and a full update", async () => {
    const view = rawTable("t1", [
      [{ cell: "A1", value: "Data Rating ratings" }],
      [{ cell: "A2", value: "attributes" }],
      [{ cell: "A3", value: "Ratings" }],
      [{ cell: "A4", value: ["MA2", "FA+", "SPA"] }],
    ]);
    harness.mockHttp.onGet(tablePath("t1")).reply(200, view);
    harness.mockHttp.onPut(tablePath("t1")).reply(204);

    const read = dataOf(await executeTool("get_table", { projectId: "p1", tableId: "t1" }, harness.client));
    await executeTool("update_table", { projectId: "p1", tableId: "t1", view: read }, harness.client);

    expect(bodyOf(harness.mockHttp.history.put[0]).source[3][0].value).toEqual(["MA2", "FA+", "SPA"]);
  });

  it("rejects a windowed RawSource view before it can delete omitted rows", async () => {
    await expect(executeTool("update_table", {
      projectId: "p1",
      tableId: "t1",
      view: rawTable("t1", undefined, { totalRows: 400 }),
    }, harness.client)).rejects.toThrow(/windowed RawSource view cannot replace the whole table/);
    expectNoRequest();
  });

  it("rejects an incomplete view after totalRows was removed", async () => {
    harness.mockHttp.onGet(tablePath("t1")).reply(200, rawTable("t1", undefined, { totalRows: 359 }));

    await expect(executeTool("update_table", {
      projectId: "p1",
      tableId: "t1",
      view: rawTable("t1", Array.from({ length: 5 }, () => [{ value: null }])),
    }, harness.client)).rejects.toThrow(/current table has 359 row\(s\)/);
    expect(harness.mockHttp.history.get[0].params).toMatchObject({ startRow: 0, maxRows: 1 });
    expect(harness.mockHttp.history.put).toHaveLength(0);
  });

  it("rejects mismatched table ids before probing the live table", async () => {
    await expect(executeTool("update_table", {
      projectId: "p1",
      tableId: "t1",
      view: rawTable("other-table"),
    }, harness.client)).rejects.toMatchObject({
      code: ProtocolErrorCode.InvalidParams,
      message: expect.stringMatching(/Table ID mismatch.*t1.*other-table/),
    });
    expectNoRequest();
  });
});

describe("openl_get_table cut by rows", () => {
  it("cuts a merged cell crossing the last row it delivers as Studio cuts a window, naming its area", async () => {
    // Column B of the first 400 rows is one merged cell, whose value is long enough to need a cut
    const source = Array.from({ length: 400 }, (_, row) => [
      row === 0 ? { cell: "B4", value: "Dog", rowspan: 400 } : { covered: true },
      { cell: `C${row + 4}`, value: "x".repeat(80) },
    ]);
    harness.mockHttp.onGet(tablePath("t1")).reply(200, rawTable("t1", source));

    const result = await executeTool("get_table", { projectId: "p1", tableId: "t1" }, harness.client);

    const response = JSON.parse(textOf(result));
    const delivered = response.data.source.length;
    expect(delivered).toBeLessThan(400);
    expect(response.data.source[0][0]).toEqual({ cell: "B4", value: "Dog", rowspan: delivered, mergedArea: "B4:B403" });
  });
});

describe("openl_append_table", () => {
  const append = (tableId: string, rows: object[][]) =>
    executeTool("append_table", {
      projectId: "p1", tableId, appendData: { tableType: "RawSource", rows }, version: "v1", }, harness.client);

  it("appends the rows, keeps the id of a table edited in place, and reads the table back", async () => {
    harness.mockHttp.onPost(`${tablePath("t1")}/lines`).reply(204);
    harness.mockHttp.onGet(tablePath("t1")).reply(200, rawTable("t1"));

    const rows = [[{ value: "A1" }, { value: "B1" }], [{ value: "A2" }, { value: "B2" }]];
    const payload = dataOf(await append("t1", rows));

    expect(bodyOf(harness.mockHttp.history.post[0])).toEqual({ tableType: "RawSource", rows });
    expect(payload).toEqual({
      success: true,
      message: "Successfully appended 2 raw source row(s) to table t1",
      recompileTriggered: true,
      tableId: "t1",
      version: "v1",
    });
    expect(harness.mockHttp.history.get.map((request) => request.url)).toEqual([tablePath("t1")]);
  });

  it("appends to the version of the table it names", async () => {
    harness.mockHttp.onPost(`${tablePath("t1")}/lines`).reply(204);
    harness.mockHttp.onGet(tablePath("t1")).reply(200, rawTable("t1"));

    await executeTool("append_table", {
      projectId: "p1", tableId: "t1", appendData: { tableType: "RawSource", rows: [[{ value: 1 }]] }, version: "v1",
    }, harness.client);

    expect(harness.mockHttp.history.post[0].headers).toMatchObject({ "If-Match": "\"v1\"" });
  });

  it("passes on Studio's refusal of a row of another width, with no read of the table before it", async () => {
    harness.mockHttp.onPost(`${tablePath("t1")}/lines`).reply(400, {
      code: "table.action.row.width",
      message: "The row has 2 cells but the table is 5 columns wide. A row must have exactly one cell per column.",
    });

    await expect(append("t1", [[{ value: "R2" }, { value: "01/01/2025" }]]))
      .rejects.toThrow(/The row has 2 cells but the table is 5 columns wide/);
    expect(harness.mockHttp.history.get).toHaveLength(0);
  });

  it("reports the new id Studio answers with 200/{id} for a renamed table", async () => {
    const oldId = "aaaa0000aaaa0000";
    const newId = "bbbb1111bbbb1111";
    harness.mockHttp.onGet(tablePath(newId)).reply(200, rawTable(newId));
    harness.mockHttp.onPost(`${tablePath(oldId)}/lines`).reply(200, { id: newId });

    const payload = dataOf(await append(oldId, [[{ value: "R2" }]]));

    expect(payload).toMatchObject({
      success: true,
      tableId: newId,
      tableIdChanged: true,
      previousTableId: oldId,
      recompileTriggered: true,
    });
    expect(payload.note).toContain(`now known by the id '${newId}'`);
    // Recompile read targeted the studio-reported id.
    expect(harness.mockHttp.history.get.map((request) => request.url)).toEqual([tablePath(newId)]);
  });

  it("tells an applied append whose table cannot be read back", async () => {
    harness.mockHttp.onPost(`${tablePath("t1")}/lines`).reply(204);
    harness.mockHttp.onGet(tablePath("t1")).reply(503, { message: "Compilation model is unavailable" });

    const payload = dataOf(await append("t1", [[{ value: 1 }]]));

    expect(payload).toMatchObject({ success: true, recompileTriggered: false });
    expect(payload.note).toContain("the append WAS applied");
  });
});

describe("openl_delete_table", () => {
  it("deletes the table and reports success", async () => {
    harness.mockHttp.onDelete(tablePath("t1")).reply(204);

    const result = await executeTool("delete_table", { projectId: "p1", tableId: "t1", version: "v1" }, harness.client);

    expect(harness.mockHttp.history.delete).toHaveLength(1);
    expect(textOf(result)).toContain("Successfully deleted table t1");
    expect(textOf(result)).toContain("openl_project_status");
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
