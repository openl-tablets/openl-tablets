import { afterEach, beforeAll, beforeEach, describe, expect, it } from "@jest/globals";
import MockAdapter from "axios-mock-adapter";

import { OpenLClient } from "../../src/client.js";
import { executeTool, registerAllTools } from "../../src/handlers/index.js";

function jsonResult<T>(text: string): T {
  return (JSON.parse(text) as { data: T }).data;
}

describe("project branch and merge handlers", () => {
  let client: OpenLClient;
  let mockAxios: MockAdapter;

  beforeAll(() => {
    registerAllTools();
  });

  beforeEach(() => {
    client = new OpenLClient({ baseUrl: "http://localhost:8080" });
    // @ts-expect-error Test-only access to the client's HTTP adapter.
    mockAxios = new MockAdapter(client.axiosInstance);
  });

  afterEach(() => {
    mockAxios.restore();
  });

  it("lists project branches by default and repository-wide merge targets on request", async () => {
    const projectBranches = [
      { name: "feature/rates" },
    ];
    const repositoryBranches = [
      { name: "main", base: true, protected: true },
      ...projectBranches,
    ];
    const scopes: unknown[] = [];
    mockAxios.onGet("/projects/p1/branches").reply((config) => {
      const scope = config.params?.scope;
      scopes.push(scope);
      return [200, scope === "repository" ? repositoryBranches : projectBranches];
    });

    const projectResponse = await executeTool("list_project_branches", {
      projectId: "p1", response_format: "json",
    }, client);
    const repositoryResponse = await executeTool("list_project_branches", {
      projectId: "p1", scope: "repository", response_format: "json",
    }, client);

    expect(scopes).toEqual(["project", "repository"]);
    expect(jsonResult(projectResponse.content[0].text)).toEqual(projectBranches);
    expect(jsonResult(repositoryResponse.content[0].text)).toEqual(repositoryBranches);
  });

  it("checks merge direction with the current backend request shape", async () => {
    let body: unknown;
    mockAxios.onPost("/projects/p1/merge/check").reply((config) => {
      body = JSON.parse(config.data);
      return [200, {
        sourceBranch: "feature", targetBranch: "main", status: "mergeable", canMerge: true,
      }];
    });

    const response = await executeTool("check_project_merge", {
      projectId: "p1", otherBranch: "feature", mode: "receive", response_format: "json",
    }, client);

    expect(body).toEqual({ mode: "receive", otherBranch: "feature" });
    expect(jsonResult<Record<string, unknown>>(response.content[0].text)).toMatchObject({
      sourceBranch: "feature", targetBranch: "main", status: "mergeable",
    });
  });

  it("returns up-to-date without issuing the mutating merge request", async () => {
    let mergeCalled = false;
    mockAxios.onPost("/projects/p1/merge/check").reply(200, {
      sourceBranch: "feature", targetBranch: "main", status: "up-to-date", canMerge: true,
    });
    mockAxios.onPost("/projects/p1/merge").reply(() => {
      mergeCalled = true;
      return [200, { status: "success" }];
    });

    const response = await executeTool("merge_project_branches", {
      projectId: "p1", otherBranch: "feature", mode: "receive", response_format: "json",
    }, client);

    expect(mergeCalled).toBe(false);
    expect(jsonResult<Record<string, unknown>>(response.content[0].text)).toMatchObject({
      success: true, status: "up-to-date",
    });
  });

  it("returns the conflict recovery path without the superseded relationship check", async () => {
    let mergeBody: unknown;
    let mergeParams: Record<string, unknown> | undefined;
    mockAxios.onPost("/projects/p1/merge/check").reply(200, {
      sourceBranch: "feature", targetBranch: "main", status: "mergeable", canMerge: true,
    });
    mockAxios.onPost("/projects/p1/merge").reply((config) => {
      mergeBody = JSON.parse(config.data);
      mergeParams = config.params;
      return [200, {
        status: "conflicts",
        conflictGroups: [{ projectName: "Rating", files: ["rules/Main.xlsx"] }],
      }];
    });

    const response = await executeTool("merge_project_branches", {
      projectId: "p1", otherBranch: "feature", mode: "receive", response_format: "json",
    }, client);

    expect(mergeBody).toEqual({ mode: "receive", otherBranch: "feature" });
    expect(mergeParams).toEqual({ force: false });
    const result = jsonResult<Record<string, unknown>>(response.content[0].text);
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
    mockAxios.onPost("/projects/p1/merge/check").reply(200, {
      sourceBranch: "feature",
      targetBranch: "main",
      status: "mergeable",
      canMerge: true,
      blockedBy: null,
    });
    mockAxios.onPost("/projects/p1/merge").reply(200, { status: "success" });

    const response = await executeTool("merge_project_branches", {
      projectId: "p1",
      otherBranch: "feature",
      mode: "receive",
    }, client);

    expect(jsonResult<Record<string, unknown>>(response.content[0].text)).toMatchObject({
      success: true,
      status: "success",
    });
  });

  it("refuses a merge blocker before issuing the mutating request", async () => {
    let mergeCalled = false;
    mockAxios.onPost("/projects/p1/merge/check").reply(200, {
      sourceBranch: "feature", targetBranch: "main", status: "mergeable",
      canMerge: false, blockedBy: "locked",
    });
    mockAxios.onPost("/projects/p1/merge").reply(() => {
      mergeCalled = true;
      return [200, { status: "success" }];
    });

    await expect(executeTool("merge_project_branches", {
      projectId: "p1", otherBranch: "feature", mode: "receive",
    }, client)).rejects.toThrow(/locked/);
    expect(mergeCalled).toBe(false);
  });

  it("sends force only for the bypass-required blocker", async () => {
    mockAxios.onPost("/projects/p1/merge/check").reply(200, {
      sourceBranch: "feature", targetBranch: "main", status: "mergeable", canMerge: true,
    });

    await expect(executeTool("merge_project_branches", {
      projectId: "p1", otherBranch: "feature", mode: "receive", force: true, confirmForce: true,
    }, client)).rejects.toThrow(/bypass-required/);
    expect(mockAxios.history.post).toHaveLength(1);

    mockAxios.reset();
    mockAxios.onPost("/projects/p1/merge/check").reply(200, {
      sourceBranch: "feature", targetBranch: "main", status: "mergeable",
      canMerge: false, blockedBy: "bypass-required",
    });
    mockAxios.onPost("/projects/p1/merge").reply(200, { status: "success" });

    await executeTool("merge_project_branches", {
      projectId: "p1", otherBranch: "feature", mode: "receive", force: true, confirmForce: true,
    }, client);
    expect(mockAxios.history.post[1].params).toEqual({ force: true });
  });

  it("reads conflict details and bounded binary sides from the same session", async () => {
    const conflicts = {
      conflictGroups: [{ projectName: "Rating", files: ["rules/Main.xlsx"] }],
      oursRevision: { branch: "main", commit: "abc", exists: true },
      theirsRevision: { branch: "feature", commit: "def", exists: true },
      baseRevision: { commit: "000", exists: true },
      defaultMessage: "Merge feature into main",
    };
    let fileParams: Record<string, unknown> | undefined;
    mockAxios.onGet("/projects/p1/merge/conflicts").reply(200, conflicts);
    mockAxios.onGet("/projects/p1/merge/conflicts/files").reply((config) => {
      fileParams = config.params;
      return [200, Buffer.from([0, 1, 2, 3, 4]), {
        "content-type": "Application/Vnd.Openxmlformats-Officedocument.Spreadsheetml.Sheet",
        "content-disposition": "attachment; filename=Main.xlsx",
      }];
    });

    const detailsResponse = await executeTool("get_merge_conflicts", {
      projectId: "p1", response_format: "json",
    }, client);
    const fileResponse = await executeTool("read_merge_conflict_file", {
      projectId: "p1", file: "rules/Main.xlsx", side: "OURS", length: 3,
    }, client);

    expect(jsonResult(detailsResponse.content[0].text)).toEqual(conflicts);
    expect(fileParams).toEqual({ file: "rules/Main.xlsx", side: "OURS" });
    expect(jsonResult<Record<string, unknown>>(fileResponse.content[0].text)).toMatchObject({
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
    }, client);
    expect(jsonResult<Record<string, unknown>>(conciseResponse.content[0].text)).toMatchObject({
      file: "rules/Main.xlsx",
      content: Buffer.from([0, 1, 2]).toString("base64"),
      nextOffset: 3,
    });
  });

  it("returns binary conflict chunks as lossless base64 TextContent", async () => {
    const bytes = Buffer.from([0, 1, 2, 3]);
    mockAxios.onGet("/projects/p1/merge/conflicts/files").reply(200, bytes, {
      "content-type": "application/octet-stream",
    });

    const response = await executeTool("read_merge_conflict_file", {
      projectId: "p1",
      file: "rules/Main.xlsx",
      side: "THEIRS",
      length: 3,
    }, client, { signal: new AbortController().signal });

    expect(jsonResult<Record<string, unknown>>(response.content[0].text)).toMatchObject({
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
    mockAxios.onGet("/projects/p1/merge/conflicts/files").reply(200, content, {
      "content-type": "text/plain; charset=utf-8",
    });

    const first = jsonResult<Record<string, unknown>>((await executeTool("read_merge_conflict_file", {
      projectId: "p1", file: "rules/config.txt", side: "OURS", encoding: "utf-8", length: 2,
    }, client)).content[0].text);
    const second = jsonResult<Record<string, unknown>>((await executeTool("read_merge_conflict_file", {
      projectId: "p1", file: "rules/config.txt", side: "OURS", encoding: "utf-8",
      offset: first.nextOffset as number, length: 2,
    }, client)).content[0].text);

    expect(first).toMatchObject({ content: "A😀", offset: 0, returnedBytes: 5, nextOffset: 5 });
    expect(second).toMatchObject({ content: "B", offset: 5, returnedBytes: 1, hasMore: false });
    expect(`${first.content}${second.content}`).toBe("A😀B");
  });

  it("clears the pending conflict session", async () => {
    mockAxios.onDelete("/projects/p1/merge/conflicts").reply(204);
    const response = await executeTool("cancel_merge_conflicts", {
      projectId: "p1", response_format: "json",
    }, client);
    expect(jsonResult<Record<string, unknown>>(response.content[0].text)).toMatchObject({ success: true });
  });

  it("deletes a project only after its current name matches confirmation", async () => {
    let deleteParams: Record<string, unknown> | undefined;
    mockAxios.onGet("/projects/p1").reply(200, { id: "p1", name: "Rating" });
    mockAxios.onDelete("/projects/p1").reply((config) => {
      deleteParams = config.params;
      return [204];
    });

    await expect(executeTool("delete_project", {
      projectId: "p1", confirmProjectName: "Other",
    }, client)).rejects.toThrow(/does not match/);
    expect(mockAxios.history.delete).toHaveLength(0);

    const response = await executeTool("delete_project", {
      projectId: "p1", confirmProjectName: "Rating", comment: "Remove retired project", response_format: "json",
    }, client);
    expect(deleteParams).toEqual({ comment: "Remove retired project" });
    expect(jsonResult<Record<string, unknown>>(response.content[0].text)).toMatchObject({
      success: true, projectName: "Rating",
    });
  });

  it("protects base/protected branches and preserves slashes when force-deleting", async () => {
    const branches = [
      { name: "main", base: true },
      { name: "release", protected: true },
      { name: "feature/rates 2026" },
    ];
    const deletions: Array<{ url: string; params?: Record<string, unknown> }> = [];
    mockAxios.onGet("/projects/p1/branches").reply((config) => {
      expect(config.params).toBeUndefined();
      return [200, branches];
    });
    mockAxios.onGet("/projects/p1").reply(200, {
      id: "p1", name: "Rating", branch: "feature/rates 2026", status: "OPENED",
    });
    mockAxios.onPost("/projects/p1/merge/check").reply(200, {
      sourceBranch: "feature/rates 2026", targetBranch: "main", status: "up-to-date", canMerge: true,
    });
    mockAxios.onDelete(/\/projects\/p1\/branches\//).reply((config) => {
      deletions.push({ url: config.url ?? "", params: config.params });
      return [204];
    });

    await expect(executeTool("delete_project_branch", {
      projectId: "p1", branch: "main", confirmBranchName: "main",
    }, client)).rejects.toThrow(/base branch/);
    await expect(executeTool("delete_project_branch", {
      projectId: "p1", branch: "release", confirmBranchName: "release",
    }, client)).rejects.toThrow(/protected/);

    await executeTool("delete_project_branch", {
      projectId: "p1",
      branch: "release",
      confirmBranchName: "release",
      force: true,
      confirmForce: true,
      confirmDataLoss: true,
    }, client);

    await executeTool("delete_project_branch", {
      projectId: "p1",
      branch: "feature/rates 2026",
      confirmBranchName: "feature/rates 2026",
    }, client);
    expect(deletions).toEqual([
      { url: "/projects/p1/branches/release", params: { force: true } },
      { url: "/projects/p1/branches/feature/rates%202026", params: { force: false } },
    ]);
  });

  it("refuses an unmerged current branch until data loss is explicitly confirmed", async () => {
    const branches = [
      { name: "main", base: true },
      { name: "feature/rates" },
    ];
    mockAxios.onGet("/projects/p1/branches").reply(200, branches);
    mockAxios.onGet("/projects/p1").reply(200, {
      id: "p1", name: "Rating", branch: "feature/rates", status: "OPENED",
    });
    mockAxios.onPost("/projects/p1/merge/check").reply(200, {
      sourceBranch: "feature/rates", targetBranch: "main", status: "mergeable", canMerge: true,
    });
    mockAxios.onDelete("/projects/p1/branches/feature/rates").reply(204);

    const request = {
      projectId: "p1", branch: "feature/rates", confirmBranchName: "feature/rates",
    };
    await expect(executeTool("delete_project_branch", request, client)).rejects.toThrow(
      /not merged into base branch 'main'.*confirmDataLoss=true/,
    );
    expect(mockAxios.history.delete).toHaveLength(0);

    const response = await executeTool("delete_project_branch", {
      ...request,
      confirmDataLoss: true,
      response_format: "json",
    }, client);

    expect(mockAxios.history.post.map((call) => JSON.parse(call.data))).toEqual([
      { mode: "send", otherBranch: "main" },
      { mode: "send", otherBranch: "main" },
    ]);
    expect(jsonResult<Record<string, unknown>>(response.content[0].text)).toMatchObject({
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
    mockAxios.onGet("/projects/p1/branches").reply(200, branches);
    mockAxios.onGet("/projects/p1").reply(200, {
      id: "p1", name: "Rating", branch: "main", status: "OPENED",
    });

    await expect(executeTool("delete_project_branch", {
      projectId: "p1", branch: "feature/other", confirmBranchName: "feature/other",
    }, client)).rejects.toThrow(/current branch 'main'.*Open 'feature\/other'/);

    expect(mockAxios.history.post).toHaveLength(0);
    expect(mockAxios.history.delete).toHaveLength(0);
  });

  it.each([
    ["feature/concurrent", "main"],
    ["feature/rates", "develop"],
  ])("rejects an up-to-date check for unexpected branches (%s -> %s)", async (sourceBranch, targetBranch) => {
    const branches = [
      { name: "main", base: true },
      { name: "feature/rates" },
    ];
    mockAxios.onGet("/projects/p1/branches").reply(200, branches);
    mockAxios.onGet("/projects/p1").reply(200, {
      id: "p1", name: "Rating", branch: "feature/rates", status: "OPENED",
    });
    mockAxios.onPost("/projects/p1/merge/check").reply(200, {
      sourceBranch, targetBranch, status: "up-to-date", canMerge: true,
    });

    await expect(executeTool("delete_project_branch", {
      projectId: "p1", branch: "feature/rates", confirmBranchName: "feature/rates",
    }, client)).rejects.toThrow(
      `Studio checked branch '${sourceBranch}' into '${targetBranch}' instead of ` +
        "'feature/rates' into 'main', so deletion-target divergence was not verified.",
    );

    expect(mockAxios.history.delete).toHaveLength(0);
  });

  it("requires data-loss confirmation for unsaved changes without running merge check", async () => {
    const branches = [
      { name: "main", base: true },
      { name: "feature/rates" },
    ];
    mockAxios.onGet("/projects/p1/branches").reply(200, branches);
    mockAxios.onGet("/projects/p1").reply(200, {
      id: "p1", name: "Rating", branch: "feature/rates", status: "EDITING",
    });

    await expect(executeTool("delete_project_branch", {
      projectId: "p1", branch: "feature/rates", confirmBranchName: "feature/rates",
    }, client)).rejects.toThrow(/unsaved working-copy changes.*confirmDataLoss=true/);

    expect(mockAxios.history.post).toHaveLength(0);
    expect(mockAxios.history.delete).toHaveLength(0);
  });
});
