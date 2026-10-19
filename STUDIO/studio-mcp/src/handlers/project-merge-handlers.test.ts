/**
 * Integration tests of the branch, merge and deletion tools of src/handlers/project-merge-handlers.ts.
 */

import { describe, expect, it } from "vitest";
import { executeTool } from "./index.js";
import { bodyOf } from "../testing/mock-http.js";
import { dataOf, harness, useToolHarness } from "../testing/harness.js";

useToolHarness();

describe("project branch and merge handlers", () => {
  it("lists project branches by default and repository-wide merge targets on request", async () => {
    const projectBranches = [
      { name: "feature/rates" },
    ];
    const repositoryBranches = [
      { name: "main", base: true, protected: true },
      ...projectBranches,
    ];
    const scopes: unknown[] = [];
    harness.mockHttp.onGet("/projects/p1/branches").reply((config) => {
      const scope = config.params?.scope;
      scopes.push(scope);
      return [200, scope === "repository" ? repositoryBranches : projectBranches];
    });

    const projectResponse = await executeTool("list_project_branches", {
      projectId: "p1", response_format: "json",
    }, harness.client);
    const repositoryResponse = await executeTool("list_project_branches", {
      projectId: "p1", scope: "repository", response_format: "json",
    }, harness.client);

    expect(scopes).toEqual(["project", "repository"]);
    expect(dataOf(projectResponse)).toEqual(projectBranches);
    expect(dataOf(repositoryResponse)).toEqual(repositoryBranches);
  });

  it("checks merge direction with the current backend request shape", async () => {
    harness.mockHttp.onPost("/projects/p1/merge/check").reply(200, {
      sourceBranch: "feature", targetBranch: "main", status: "mergeable", canMerge: true,
    });

    const response = await executeTool("check_project_merge", {
      projectId: "p1", otherBranch: "feature", mode: "receive", response_format: "json",
    }, harness.client);

    expect(bodyOf(harness.mockHttp.history.post[0])).toEqual({ mode: "receive", otherBranch: "feature" });
    expect(dataOf<Record<string, unknown>>(response)).toMatchObject({
      sourceBranch: "feature", targetBranch: "main", status: "mergeable",
    });
  });

  it("returns up-to-date without issuing the mutating merge request", async () => {
    harness.mockHttp.onPost("/projects/p1/merge/check").reply(200, {
      sourceBranch: "feature", targetBranch: "main", status: "up-to-date", canMerge: true,
    });

    const response = await executeTool("merge_project_branches", {
      projectId: "p1", otherBranch: "feature", mode: "receive", response_format: "json",
    }, harness.client);

    expect(harness.mockHttp.history.post.map((request) => request.url)).toEqual(["/projects/p1/merge/check"]);
    expect(dataOf<Record<string, unknown>>(response)).toMatchObject({
      success: true, status: "up-to-date",
    });
  });

  it("returns the conflict recovery path without the superseded relationship check", async () => {
    harness.mockHttp.onPost("/projects/p1/merge/check").reply(200, {
      sourceBranch: "feature", targetBranch: "main", status: "mergeable", canMerge: true,
    });
    harness.mockHttp.onPost("/projects/p1/merge").reply(202);
    // Studio runs the merge in the background: the result is read until it is ready.
    harness.mockHttp.onGet("/projects/p1/merge").replyOnce(202, { status: "notReady" });
    harness.mockHttp.onGet("/projects/p1/merge").replyOnce(200, {
      status: "conflicts",
      conflictGroups: [{ projectName: "Rating", files: ["rules/Main.xlsx"] }],
    });

    const response = await executeTool("merge_project_branches", {
      projectId: "p1", otherBranch: "feature", mode: "receive", response_format: "json",
    }, harness.client);

    expect(bodyOf(harness.mockHttp.history.post[1])).toEqual({ mode: "receive", otherBranch: "feature" });
    expect(harness.mockHttp.history.post[1].params).toEqual({ force: false });
    expect(harness.mockHttp.history.get.filter((request) => request.url === "/projects/p1/merge")).toHaveLength(2);
    const result = dataOf<Record<string, unknown>>(response);
    expect(result).toMatchObject({
      success: false,
      status: "conflicts",
      nextAction: expect.stringContaining("openl_get_merge_conflicts"),
    });
    expect(result).not.toHaveProperty("check");
    expect(result.nextAction).toEqual(expect.stringContaining("does not predict conflicts"));
    expect(result.nextAction).toEqual(expect.stringContaining("manual resolution in Studio"));
    expect(result.nextAction).not.toEqual(expect.stringContaining("openl_resolve_merge_conflicts"));
  });

  it("tolerates a null merge blocker as equivalent to an omitted blocker", async () => {
    harness.mockHttp.onPost("/projects/p1/merge/check").reply(200, {
      sourceBranch: "feature",
      targetBranch: "main",
      status: "mergeable",
      canMerge: true,
      blockedBy: null,
    });
    harness.mockHttp.onPost("/projects/p1/merge").reply(202);
    harness.mockHttp.onGet("/projects/p1/merge").reply(200, { status: "success" });

    const response = await executeTool("merge_project_branches", {
      projectId: "p1",
      otherBranch: "feature",
      mode: "receive",
    }, harness.client);

    expect(dataOf<Record<string, unknown>>(response)).toMatchObject({
      success: true,
      status: "success",
    });
  });

  it("refuses a merge blocker before issuing the mutating request", async () => {
    harness.mockHttp.onPost("/projects/p1/merge/check").reply(200, {
      sourceBranch: "feature", targetBranch: "main", status: "mergeable",
      canMerge: false, blockedBy: "locked",
    });

    await expect(executeTool("merge_project_branches", {
      projectId: "p1", otherBranch: "feature", mode: "receive",
    }, harness.client)).rejects.toThrow(/locked/);
    expect(harness.mockHttp.history.post.map((request) => request.url)).toEqual(["/projects/p1/merge/check"]);
  });

  it("sends force only for the bypass-required blocker", async () => {
    harness.mockHttp.onPost("/projects/p1/merge/check").reply(200, {
      sourceBranch: "feature", targetBranch: "main", status: "mergeable", canMerge: true,
    });

    await expect(executeTool("merge_project_branches", {
      projectId: "p1", otherBranch: "feature", mode: "receive", force: true, confirmForce: true,
    }, harness.client)).rejects.toThrow(/bypass-required/);
    expect(harness.mockHttp.history.post).toHaveLength(1);

    harness.mockHttp.reset();
    harness.mockHttp.onPost("/projects/p1/merge/check").reply(200, {
      sourceBranch: "feature", targetBranch: "main", status: "mergeable",
      canMerge: false, blockedBy: "bypass-required",
    });
    harness.mockHttp.onPost("/projects/p1/merge").reply(202);
    harness.mockHttp.onGet("/projects/p1/merge").reply(200, { status: "success" });

    await executeTool("merge_project_branches", {
      projectId: "p1", otherBranch: "feature", mode: "receive", force: true, confirmForce: true,
    }, harness.client);
    expect(harness.mockHttp.history.post[1].params).toEqual({ force: true });
  });

  it("reads conflict details and bounded binary sides from the same session", async () => {
    const conflicts = {
      conflictGroups: [{ projectName: "Rating", files: ["rules/Main.xlsx"] }],
      oursRevision: { branch: "main", commit: "abc", exists: true },
      theirsRevision: { branch: "feature", commit: "def", exists: true },
      baseRevision: { commit: "000", exists: true },
      defaultMessage: "Merge feature into main",
    };
    harness.mockHttp.onGet("/projects/p1/merge/conflicts").reply(200, conflicts);
    harness.mockHttp.onGet("/projects/p1/merge/conflicts/files").reply(200, Buffer.from([0, 1, 2, 3, 4]), {
      "content-type": "Application/Vnd.Openxmlformats-Officedocument.Spreadsheetml.Sheet",
      "content-disposition": "attachment; filename=Main.xlsx",
    });

    const detailsResponse = await executeTool("get_merge_conflicts", {
      projectId: "p1", response_format: "json",
    }, harness.client);
    const fileResponse = await executeTool("read_merge_conflict_file", {
      projectId: "p1", file: "rules/Main.xlsx", side: "OURS", length: 3,
    }, harness.client);

    expect(dataOf(detailsResponse)).toEqual(conflicts);
    expect(harness.mockHttp.history.get[1].params).toEqual({ file: "rules/Main.xlsx", side: "OURS" });
    expect(dataOf<Record<string, unknown>>(fileResponse)).toMatchObject({
      contentType: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
      encoding: "base64",
      content: Buffer.from([0, 1, 2]).toString("base64"),
      returnedBytes: 3,
      hasMore: true,
      nextOffset: 3,
    });

    const conciseResponse = await executeTool("read_merge_conflict_file", {
      projectId: "p1",
      file: "rules/Main.xlsx",
      side: "OURS",
      length: 3,
      response_format: "markdown_concise",
    }, harness.client);
    expect(dataOf<Record<string, unknown>>(conciseResponse)).toMatchObject({
      file: "rules/Main.xlsx",
      content: Buffer.from([0, 1, 2]).toString("base64"),
      nextOffset: 3,
    });
  });

  it("returns binary conflict chunks as lossless base64 TextContent", async () => {
    const bytes = Buffer.from([0, 1, 2, 3]);
    harness.mockHttp.onGet("/projects/p1/merge/conflicts/files").reply(200, bytes, {
      "content-type": "application/octet-stream",
    });

    const response = await executeTool("read_merge_conflict_file", {
      projectId: "p1",
      file: "rules/Main.xlsx",
      side: "THEIRS",
      length: 3,
    }, harness.client, { signal: new AbortController().signal });

    expect(dataOf<Record<string, unknown>>(response)).toMatchObject({
      file: "rules/Main.xlsx",
      side: "THEIRS",
      mimeType: "application/octet-stream",
      returnedBytes: 3,
      hasMore: true,
      nextOffset: 3,
      content: Buffer.from([0, 1, 2]).toString("base64"),
    });
    expect(response.content).toHaveLength(1);
  });

  it("keeps UTF-8 characters complete across conflict-file chunks", async () => {
    const content = Buffer.from("A😀B", "utf-8");
    harness.mockHttp.onGet("/projects/p1/merge/conflicts/files").reply(200, content, {
      "content-type": "text/plain; charset=utf-8",
    });

    const first = dataOf<Record<string, unknown>>(await executeTool("read_merge_conflict_file", {
      projectId: "p1", file: "rules/config.txt", side: "OURS", encoding: "utf-8", length: 2,
    }, harness.client));
    const second = dataOf<Record<string, unknown>>(await executeTool("read_merge_conflict_file", {
      projectId: "p1", file: "rules/config.txt", side: "OURS", encoding: "utf-8",
      offset: first.nextOffset as number, length: 2,
    }, harness.client));

    expect(first).toMatchObject({ content: "A😀", offset: 0, returnedBytes: 5, nextOffset: 5 });
    expect(second).toMatchObject({ content: "B", offset: 5, returnedBytes: 1, hasMore: false });
    expect(`${first.content}${second.content}`).toBe("A😀B");
  });

  it("clears the pending conflict session", async () => {
    harness.mockHttp.onDelete("/projects/p1/merge/conflicts").reply(204);
    const response = await executeTool("cancel_merge_conflicts", {
      projectId: "p1", response_format: "json",
    }, harness.client);
    expect(dataOf<Record<string, unknown>>(response)).toMatchObject({ success: true });
  });

  it("deletes a project only after its current name matches confirmation", async () => {
    harness.mockHttp.onGet("/projects/p1").reply(200, { id: "p1", name: "Rating" });
    harness.mockHttp.onDelete("/projects/p1").reply(204);

    await expect(executeTool("delete_project", {
      projectId: "p1", confirmProjectName: "Other",
    }, harness.client)).rejects.toThrow(/does not match/);
    expect(harness.mockHttp.history.delete).toHaveLength(0);

    const response = await executeTool("delete_project", {
      projectId: "p1", confirmProjectName: "Rating", comment: "Remove retired project", response_format: "json",
    }, harness.client);
    expect(harness.mockHttp.history.delete[0].params).toEqual({ comment: "Remove retired project" });
    expect(dataOf<Record<string, unknown>>(response)).toMatchObject({
      success: true, projectName: "Rating",
    });
  });

  it("protects base/protected branches and preserves slashes when force-deleting", async () => {
    const branches = [
      { name: "main", base: true },
      { name: "release", protected: true },
      { name: "feature/rates 2026" },
    ];
    harness.mockHttp.onGet("/projects/p1/branches").reply((config) => {
      expect(config.params?.scope).toBeUndefined();
      return [200, branches];
    });
    harness.mockHttp.onGet("/projects/p1").reply(200, {
      id: "p1", name: "Rating", branch: "feature/rates 2026", status: "OPENED",
    });
    harness.mockHttp.onPost("/projects/p1/merge/check").reply(200, {
      sourceBranch: "feature/rates 2026", targetBranch: "main", status: "up-to-date", canMerge: true,
    });
    harness.mockHttp.onDelete(/\/projects\/p1\/branches\//).reply(204);

    await expect(executeTool("delete_project_branch", {
      projectId: "p1", branch: "main", confirmBranchName: "main",
    }, harness.client)).rejects.toThrow(/base branch/);
    await expect(executeTool("delete_project_branch", {
      projectId: "p1", branch: "release", confirmBranchName: "release",
    }, harness.client)).rejects.toThrow(/protected/);

    await executeTool("delete_project_branch", {
      projectId: "p1",
      branch: "release",
      confirmBranchName: "release",
      force: true,
      confirmForce: true,
      confirmDataLoss: true,
    }, harness.client);

    await executeTool("delete_project_branch", {
      projectId: "p1",
      branch: "feature/rates 2026",
      confirmBranchName: "feature/rates 2026",
    }, harness.client);
    expect(harness.mockHttp.history.delete.map(({ url, params }) => ({ url, params }))).toEqual([
      { url: "/projects/p1/branches/release", params: { force: true } },
      { url: "/projects/p1/branches/feature/rates%202026", params: { force: false } },
    ]);
  });

  it("refuses an unmerged current branch until data loss is explicitly confirmed", async () => {
    const branches = [
      { name: "main", base: true },
      { name: "feature/rates" },
    ];
    harness.mockHttp.onGet("/projects/p1/branches").reply(200, branches);
    harness.mockHttp.onGet("/projects/p1").reply(200, {
      id: "p1", name: "Rating", branch: "feature/rates", status: "OPENED",
    });
    harness.mockHttp.onPost("/projects/p1/merge/check").reply(200, {
      sourceBranch: "feature/rates", targetBranch: "main", status: "mergeable", canMerge: true,
    });
    harness.mockHttp.onDelete("/projects/p1/branches/feature/rates").reply(204);

    const request = {
      projectId: "p1", branch: "feature/rates", confirmBranchName: "feature/rates",
    };
    await expect(executeTool("delete_project_branch", request, harness.client)).rejects.toThrow(
      /not merged into base branch 'main'.*confirmDataLoss=true/,
    );
    expect(harness.mockHttp.history.delete).toHaveLength(0);

    const response = await executeTool("delete_project_branch", {
      ...request,
      confirmDataLoss: true,
      response_format: "json",
    }, harness.client);

    expect(harness.mockHttp.history.post.map(bodyOf)).toEqual([
      { mode: "send", otherBranch: "main" },
      { mode: "send", otherBranch: "main" },
    ]);
    expect(dataOf<Record<string, unknown>>(response)).toMatchObject({
      success: true,
      dataLossConfirmed: true,
      safety: {
        baseBranch: "main",
        currentBranch: "feature/rates",
        divergenceStatus: "unmerged",
        unsavedChanges: false,
      },
    });
  });

  it("does not run a misleading divergence check for a non-current deletion target", async () => {
    const branches = [
      { name: "main", base: true },
      { name: "feature/other" },
    ];
    harness.mockHttp.onGet("/projects/p1/branches").reply(200, branches);
    harness.mockHttp.onGet("/projects/p1").reply(200, {
      id: "p1", name: "Rating", branch: "main", status: "OPENED",
    });

    await expect(executeTool("delete_project_branch", {
      projectId: "p1", branch: "feature/other", confirmBranchName: "feature/other",
    }, harness.client)).rejects.toThrow(/current branch 'main'.*Open 'feature\/other'/);

    expect(harness.mockHttp.history.post).toHaveLength(0);
    expect(harness.mockHttp.history.delete).toHaveLength(0);
  });

  it.each([
    ["feature/concurrent", "main"],
    ["feature/rates", "develop"],
  ])("rejects an up-to-date check for unexpected branches (%s -> %s)", async (sourceBranch, targetBranch) => {
    const branches = [
      { name: "main", base: true },
      { name: "feature/rates" },
    ];
    harness.mockHttp.onGet("/projects/p1/branches").reply(200, branches);
    harness.mockHttp.onGet("/projects/p1").reply(200, {
      id: "p1", name: "Rating", branch: "feature/rates", status: "OPENED",
    });
    harness.mockHttp.onPost("/projects/p1/merge/check").reply(200, {
      sourceBranch, targetBranch, status: "up-to-date", canMerge: true,
    });

    await expect(executeTool("delete_project_branch", {
      projectId: "p1", branch: "feature/rates", confirmBranchName: "feature/rates",
    }, harness.client)).rejects.toThrow(
      `Studio checked branch '${sourceBranch}' into '${targetBranch}' instead of ` +
        "'feature/rates' into 'main', so deletion-target divergence was not verified.",
    );

    expect(harness.mockHttp.history.delete).toHaveLength(0);
  });

  it("requires data-loss confirmation for unsaved changes without running merge check", async () => {
    const branches = [
      { name: "main", base: true },
      { name: "feature/rates" },
    ];
    harness.mockHttp.onGet("/projects/p1/branches").reply(200, branches);
    harness.mockHttp.onGet("/projects/p1").reply(200, {
      id: "p1", name: "Rating", branch: "feature/rates", status: "EDITING",
    });

    await expect(executeTool("delete_project_branch", {
      projectId: "p1", branch: "feature/rates", confirmBranchName: "feature/rates",
    }, harness.client)).rejects.toThrow(/unsaved working-copy changes.*confirmDataLoss=true/);

    expect(harness.mockHttp.history.post).toHaveLength(0);
    expect(harness.mockHttp.history.delete).toHaveLength(0);
  });
});
