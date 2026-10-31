/**
 * Integration tests of the local change tools of src/handlers/local-change-handlers.ts: the local history of a module
 * of a project, and the restore of one of its versions.
 */

import { describe, expect, it } from "vitest";
import { executeTool } from "./index.js";
import { dataOf, harness, useToolHarness } from "../testing/harness.js";

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
    expect(dataOf(response)).toEqual([
      { historyId: "1787821200000_current", modifiedOn: "2026-08-27T09:00:00Z", current: true },
      { historyId: "Revision Version" },
    ]);
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

  it("requires an explicit project and module for both local-history operations", async () => {
    await expect(executeTool("list_project_local_changes", {
      projectId: "design:Project",
    }, harness.client)).rejects.toThrow(/module/);
    await expect(executeTool("restore_project_local_change", {
      module: "Main Rules",
      historyId: "Revision Version",
    }, harness.client)).rejects.toThrow(/projectId/);

    expect(harness.mockHttp.history.get).toHaveLength(0);
    expect(harness.mockHttp.history.post).toHaveLength(0);
  });
});
