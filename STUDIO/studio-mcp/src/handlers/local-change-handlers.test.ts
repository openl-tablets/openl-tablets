/**
 * Integration tests of the local change tools of src/handlers/local-change-handlers.ts: the local history of a module
 * of a project, and the restore of one of its versions.
 */

import { describe, expect, it } from "vitest";
import { executeTool } from "./index.js";
import { bodyOf } from "../testing/mock-http.js";
import { dataOf, harness, textOf, useToolHarness } from "../testing/harness.js";

useToolHarness();

describe("local change handlers", () => {
  it("lists history for the requested project module", async () => {
    const items = [
      { id: "1787821200000_current", modifiedOn: "2026-08-27T09:00:00Z", current: true },
      { id: "Revision Version" },
    ];
    harness.mockHttp.onGet("/projects/design%3AProject/local-history", {
      params: { module: "Main Rules" },
    }).reply(200, items);

    const response = await executeTool("list_project_local_changes", {
      projectId: "design:Project",
      module: "Main Rules",
    }, harness.client);

    // Each entry is named by the historyId a restore takes
    expect(textOf(response)).toBe("2 versions.\n\n| historyId | kept at | current |\n| --- | --- | --- |\n"
      + "| 1787821200000_current | 2026-08-27T09:00:00Z | yes |\n| Revision Version |  |  |");
  });

  it("lists the history of every module when no module is named", async () => {
    harness.mockHttp.onGet("/projects/p1/modules").reply(200, [{ name: "Main" }, { name: "Lookups" }]);
    harness.mockHttp.onGet("/projects/p1/local-history", { params: { module: "Main" } })
      .reply(200, [{ id: "m1_current", current: true }]);
    harness.mockHttp.onGet("/projects/p1/local-history", { params: { module: "Lookups" } })
      .reply(200, [{ id: "l2_current", current: true }, { id: "l1", modifiedOn: "2026-08-27T09:00:00Z" }]);

    const text = textOf(await executeTool("list_project_local_changes", { projectId: "p1" }, harness.client));

    expect(text).toBe("3 versions.\n\n| module | historyId | kept at | current |\n| --- | --- | --- | --- |\n"
      + "| Main | m1_current |  | yes |\n| Lookups | l2_current |  | yes |\n| Lookups | l1 | 2026-08-27T09:00:00Z |  |");
  });

  it("names the tables a restore to a version would change, comparing it with the current one", async () => {
    harness.mockHttp.onGet("/projects/p1/local-history", { params: { module: "Lookups" } })
      .reply(200, [{ id: "l3_current", current: true }, { id: "l2" }, { id: "l1" }]);
    harness.mockHttp.onPost("/projects/p1/local-history/compare").reply(202, { id: "c1" });
    harness.mockHttp.onGet("/compare/c1").replyOnce(202, { status: "notReady" }).onGet("/compare/c1").reply(200, {
      id: "c1", identical: false, sheets: [
        { name: "Regions", status: "changed", children: [
          { name: "RegionFactor", status: "changed" }, { name: "CityFactor", status: "equal" },
        ] },
        { name: "Rates", status: "equal", children: [{ name: "BaseRate", status: "equal" }] },
        { name: "MinPrem", status: "added", children: [{ name: "MinimumPremium", status: "added" }] },
      ],
    });
    harness.mockHttp.onDelete("/compare/c1").reply(204);

    const text = textOf(await executeTool("list_project_local_changes", {
      projectId: "p1", module: "Lookups", historyId: "l1",
    }, harness.client));

    expect(bodyOf(harness.mockHttp.history.post[0])).toEqual({ first: "l1", second: "l3_current" });
    expect(text).toContain("A restore of Lookups to l1 changes 2 tables;");
    expect(text).toContain("| Regions | RegionFactor | changed |\n| MinPrem | MinimumPremium | added |");
    // The comparison is dropped once read
    expect(harness.mockHttp.history.delete.map((request) => request.url)).toEqual(["/compare/c1"]);
  });

  it("tells a restore to the current version changes nothing, without comparing", async () => {
    harness.mockHttp.onGet("/projects/p1/local-history", { params: { module: "Lookups" } })
      .reply(200, [{ id: "l3_current", current: true }]);

    const text = textOf(await executeTool("list_project_local_changes", {
      projectId: "p1", module: "Lookups", historyId: "l3_current",
    }, harness.client));

    expect(text).toBe("l3_current is the current version of Lookups: a restore to it changes nothing.");
    expect(harness.mockHttp.history.post).toHaveLength(0);
  });

  it("tells a history without a current version allows no preview, without comparing", async () => {
    harness.mockHttp.onGet("/projects/p1/local-history", { params: { module: "Lookups" } })
      .reply(200, [{ id: "l1", current: false }]);

    const text = textOf(await executeTool("list_project_local_changes", {
      projectId: "p1", module: "Lookups", historyId: "l1",
    }, harness.client));

    expect(text).toBe("The history of Lookups marks no current version, so a restore to l1 cannot be compared with it: "
      + "no preview can tell what the restore changes.");
    expect(harness.mockHttp.history.post).toHaveLength(0);
  });

  describe("restore", () => {
    const history = "/projects/design%3AProject/local-history";
    const restore = (historyId: string): ReturnType<typeof executeTool> =>
      executeTool("restore_project_local_change", {
        projectId: "design:Project",
        module: "Main Rules",
        historyId,
      }, harness.client);

    it.each([
      ["the version that was current", [
        { id: "200", modifiedOn: "08/27/2026 12:02 PM" },
        { id: "100", modifiedOn: "08/27/2026 12:01 PM" },
        { id: "Revision Version_current", modifiedOn: "Revision Version", current: true },
      ], "200"],
      ["the version kept of a workbook another program changed", [
        { id: "300", modifiedOn: "08/27/2026 12:03 PM" },
        { id: "200", modifiedOn: "08/27/2026 12:02 PM" },
        { id: "100", modifiedOn: "08/27/2026 12:01 PM" },
        { id: "Revision Version_current", modifiedOn: "Revision Version", current: true },
      ], "300"],
    ])("answers %s as the version that undoes it", async (_case, after, undo) => {
      harness.mockHttp.onGet(history, { params: { module: "Main Rules" } }).replyOnce(200, [
        { id: "200_current", modifiedOn: "08/27/2026 12:02 PM", current: true },
        { id: "100", modifiedOn: "08/27/2026 12:01 PM" },
        { id: "Revision Version", modifiedOn: "Revision Version" },
      ]);
      harness.mockHttp.onGet(history, { params: { module: "Main Rules" } }).reply(200, after);
      harness.mockHttp.onPost(`${history}/restore`, { version: "Revision Version" }).reply(204);

      expect(dataOf(await restore("Revision Version"))).toEqual({ restored: "Revision Version", undo });
      expect(harness.mockHttp.history.post[0].params).toEqual({ module: "Main Rules" });
    });

    it("answers a done restore when the history cannot be read after it", async () => {
      harness.mockHttp.onGet(history, { params: { module: "Main Rules" } }).replyOnce(200, [
        { id: "200_current", modifiedOn: "08/27/2026 12:02 PM", current: true },
        { id: "Revision Version", modifiedOn: "Revision Version" },
      ]);
      harness.mockHttp.onGet(history, { params: { module: "Main Rules" } }).reply(500);
      harness.mockHttp.onPost(`${history}/restore`, { version: "Revision Version" }).reply(204);

      expect(dataOf(await restore("Revision Version"))).toEqual({
        restored: "Revision Version",
        note: expect.stringContaining("openl_list_project_local_changes lists the version that undoes it"),
      });
    });

    it("answers no undo when the restore left the module as it was", async () => {
      const unchanged = [
        { id: "200_current", modifiedOn: "08/27/2026 12:02 PM", current: true },
        { id: "Revision Version", modifiedOn: "Revision Version" },
      ];
      harness.mockHttp.onGet(history, { params: { module: "Main Rules" } }).reply(200, unchanged);
      harness.mockHttp.onPost(`${history}/restore`, { version: "200_current" }).reply(204);

      expect(dataOf(await restore("200_current"))).toEqual({ restored: "200_current" });
    });
  });

  it("requires the project, and the module of a version, before any request", async () => {
    await expect(executeTool("list_project_local_changes", {
      projectId: "design:Project", historyId: "Revision Version",
    }, harness.client)).rejects.toThrow(/historyId is a version of the history of one module/);
    await expect(executeTool("restore_project_local_change", {
      module: "Main Rules",
      historyId: "Revision Version",
    }, harness.client)).rejects.toThrow(/projectId/);

    expect(harness.mockHttp.history.get).toHaveLength(0);
    expect(harness.mockHttp.history.post).toHaveLength(0);
  });
});
