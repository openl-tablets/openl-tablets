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
      moduleName: "Main Rules",
      response_format: "json",
    }, harness.client);

    expect(dataOf(response)).toEqual(items);
  });

  it("restores the requested project module and identifies it in the result", async () => {
    harness.mockHttp.onPost("/projects/design%3AProject/local-history/restore", {
      version: "Revision Version",
    }).reply(204);

    const response = await executeTool("restore_project_local_change", {
      projectId: "design:Project",
      moduleName: "Main Rules",
      historyId: "Revision Version",
      response_format: "json",
    }, harness.client);

    expect(harness.mockHttp.history.post[0].params).toEqual({ module: "Main Rules" });
    expect(dataOf(response)).toEqual({
      success: true,
      message: "Successfully restored project module to local history version 'Revision Version'",
      projectId: "design:Project",
      moduleName: "Main Rules",
      historyId: "Revision Version",
    });
  });

  it("requires an explicit project and module for both local-history operations", async () => {
    await expect(executeTool("list_project_local_changes", {
      projectId: "design:Project",
    }, harness.client)).rejects.toThrow(/moduleName/);
    await expect(executeTool("restore_project_local_change", {
      moduleName: "Main Rules",
      historyId: "Revision Version",
    }, harness.client)).rejects.toThrow(/projectId/);

    expect(harness.mockHttp.history.get).toHaveLength(0);
    expect(harness.mockHttp.history.post).toHaveLength(0);
  });
});
