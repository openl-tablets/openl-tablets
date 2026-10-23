/**
 * Integration tests of the project tools of src/handlers/project-handlers.ts: the list and the details of projects,
 * their compile status, open/save/close, branches, creation and copies, and their modules and worksheets.
 */

import { describe, it, expect } from "vitest";
import { executeTool, getAllTools } from "./index.js";
import type { ProjectStatusView } from "../types.js";
import { STATUS_SUMMARY_QUERY } from "../client.js";
import { bodyOf } from "../testing/mock-http.js";
import { mockRepositories, pageOf, projectView } from "../testing/openl-api-mocks.js";
import { dataOf, harness, textOf, useToolHarness } from "../testing/harness.js";

useToolHarness();

const projectId = "design:insurance-rules:hash123";
const projectPath = `/projects/${encodeURIComponent(projectId)}`;
/** The path of the project the lifecycle tests address by "design-project1". */
const lifecyclePath = "/projects/design-project1";

/** Studio answers the status as the `compileStatus` of the project it returns for `include=status`. */
const withStatus = (status: object): object => ({ compileStatus: status });

/** A compilation that ended, with the given diagnostics. */
const compiled = (compileState: string, items: object[] = [], tests = 0): object => ({
  projectId,
  branch: "main",
  compileState,
  compilation: {
    messages: { items, total: items.length, errors: 0, warnings: 0 },
    modules: { total: 1, compiled: 1 },
    tests: { total: tests },
  },
});

const projects = (count: number, status: "OPENED" | "CLOSED") => Array.from({ length: count }, (_, i) =>
  projectView(status, { id: `design:p${i}:hash${i}`, name: `p${i}`, path: `p${i}` }));

describe("project lists and details", () => {
  it("openl_list_projects resolves the repository by its display name and adds a flat projectId", async () => {
    harness.mockHttp.onGet("/repos").reply(200, mockRepositories);
    harness.mockHttp.onGet("/projects", { params: { repository: "design", offset: 0, size: 50 } })
      .reply(200, pageOf([projectView("OPENED", { id: "design:Project 1:hash123", name: "Project 1" })]));

    const result = await executeTool("list_projects", { repository: "Design Repository", response_format: "json" }, harness.client);

    expect(dataOf(result)).toEqual([expect.objectContaining({ name: "Project 1", projectId: "design:Project 1:hash123" })]);
  });

  it("openl_list_projects returns requested summary and status expansions", async () => {
    harness.mockHttp.onGet("/projects", {
      params: { include: ["summary", "status"], offset: 0, size: 50 },
    }).reply(200, {
      ...pageOf([], { total: 0 }),
      repositoryCounts: [{ repository: "design", count: 3 }],
      statusCounts: { OPENED: 2, CLOSED: 1 },
      statuses: ["OPENED", "CLOSED"],
      tagCounts: [{ tag: "release", count: 1 }],
    });

    const result = await executeTool("list_projects", {
      include: ["summary", "status"],
      response_format: "json",
    }, harness.client);

    expect(JSON.parse(textOf(result))).toMatchObject({
      repositoryCounts: [{ repository: "design", count: 3 }],
      statusCounts: { OPENED: 2, CLOSED: 1 },
      statuses: ["OPENED", "CLOSED"],
      tagCounts: [{ tag: "release", count: 1 }],
    });
  });

  it("openl_list_projects pages the projects Studio pages, also from an offset off the page grid", async () => {
    const allProjects = projects(120, "CLOSED");
    harness.mockHttp.onGet("/projects").reply((config) => {
      const offset = Number(config.params?.offset);
      const pageSize = Number(config.params?.size);
      const content = allProjects.slice(offset, offset + pageSize);
      return [200, pageOf(content, { pageNumber: Math.floor(offset / pageSize), pageSize, total: allProjects.length })];
    });

    const first = textOf(await executeTool("list_projects", { limit: 50, offset: 0, response_format: "markdown" }, harness.client));
    const second = JSON.parse(textOf(await executeTool("list_projects", { limit: 50, offset: 50, response_format: "json" }, harness.client)));
    const third = JSON.parse(textOf(await executeTool("list_projects", { limit: 50, offset: 100, response_format: "json" }, harness.client)));
    const shifted = JSON.parse(textOf(await executeTool("list_projects", { limit: 50, offset: 25, response_format: "json" }, harness.client)));

    expect(first).toContain("Total: 120");
    expect(first).toContain("More results available (next offset: 50)");
    expect(second.data).toHaveLength(50);
    expect(second.data[0].name).toBe("p50");
    expect(second.pagination).toEqual({ limit: 50, offset: 50, total_count: 120, has_more: true, next_offset: 100 });
    expect(third.data).toHaveLength(20);
    expect(third.data[0].name).toBe("p100");
    expect(third.pagination).toEqual({ limit: 50, offset: 100, total_count: 120, has_more: false });
    expect(shifted.data[0].name).toBe("p25");
    expect(shifted.pagination).toMatchObject({ offset: 25, next_offset: 75 });
  });

  it("openl_list_projects renders the status breakdown only in detailed Markdown", async () => {
    harness.mockHttp.onGet("/projects").reply(200, pageOf([projectView("OPENED"), projectView("CLOSED", { name: "p2" })], { total: 2 }));

    const concise = textOf(await executeTool("list_projects", { response_format: "markdown_concise" }, harness.client));
    const detailed = textOf(await executeTool("list_projects", { response_format: "markdown_detailed" }, harness.client));

    expect(concise).toContain("Found 2 projects");
    expect(concise).not.toContain("**Status Breakdown:**");
    expect(detailed).toContain("**Status Breakdown:** 1 opened, 1 closed");
    expect(concise.length).toBeLessThan(detailed.length);
  });

  it("openl_get_project answers the project Studio describes", async () => {
    harness.mockHttp.onGet(lifecyclePath).reply(200, projectView("OPENED"));

    const result = await executeTool("get_project", { projectId: "design-project1", response_format: "json" }, harness.client);

    expect(dataOf(result)).toEqual(projectView("OPENED"));
  });

  it("openl_list_project_modules and openl_list_module_sheets read the modules and the worksheets of a project", async () => {
    const modules = [{ name: "Main", path: "rules/*.xlsx", modules: [{ name: "Rates", path: "rules/Rates.xlsx" }] }];
    harness.mockHttp.onGet("/projects/p1/modules").reply(200, modules);
    harness.mockHttp.onGet("/projects/p1/modules/Main%20Rules/sheets").reply(200, ["Rates", "Tests"]);

    const moduleResult = await executeTool("list_project_modules", { projectId: "p1", response_format: "json" }, harness.client);
    const sheetResult = await executeTool("list_module_sheets", {
      projectId: "p1", moduleName: "Main Rules", response_format: "json",
    }, harness.client);

    expect(dataOf(moduleResult)).toEqual(modules);
    expect(dataOf(sheetResult)).toEqual(["Rates", "Tests"]);
  });
});

describe("project status", () => {
  it("starts lazy compilation on idle by default and returns the resulting status", async () => {
    harness.mockHttp
      .onGet(projectPath)
      .replyOnce(200, withStatus({ projectId, branch: "main", compileState: "idle" }))
      .onGet(projectPath)
      .replyOnce(200, withStatus(compiled("ok", [], 34)));
    harness.mockHttp.onGet(`${projectPath}/tables`, { params: { offset: 0, size: 1 } }).reply(200, pageOf([], { pageSize: 1, total: 0 }));

    const response = dataOf(await executeTool("project_status", { projectId, response_format: "json" }, harness.client));

    expect(response.compileState).toBe("ok");
    expect(response.compilation.tests.total).toBe(34);
    expect(harness.mockHttp.history.get.filter((request) => request.url.endsWith("/tables"))).toHaveLength(1);
    // The default path above starts compilation, so MCP clients must not be
    // told that the tool is unconditionally read-only.
    expect(getAllTools().find((tool) => tool.name === "project_status")?.annotations?.readOnlyHint).toBe(false);
  });

  it("compiles the project again from files another program changed, with a note to read its tables again", async () => {
    harness.mockHttp
      .onGet(projectPath)
      .replyOnce(200, withStatus({ ...compiled("ok", [], 34), changedOnDisk: true }))
      .onGet(projectPath)
      .replyOnce(200, withStatus(compiled("ok", [], 35)));
    harness.mockHttp
      .onGet(projectPath, { params: STATUS_SUMMARY_QUERY })
      .replyOnce(200, withStatus({ compileState: "ok", changedOnDisk: true }))
      .onGet(projectPath, { params: STATUS_SUMMARY_QUERY })
      .replyOnce(200, withStatus({ compileState: "compiling" }))
      .onGet(projectPath, { params: STATUS_SUMMARY_QUERY })
      .reply(200, withStatus({ compileState: "ok" }));
    harness.mockHttp.onGet(`${projectPath}/modules`).reply(200, [{ name: "Main Rules", path: "rules/Main.xlsx" }]);
    harness.mockHttp.onPost(`${projectPath}/modules/Main%20Rules/compile`).reply((config) => [config.params?.reset ? 202 : 400]);

    const response = dataOf(await executeTool("project_status", { projectId, response_format: "json" }, harness.client));

    expect(harness.mockHttp.history.post).toHaveLength(1);
    expect(response.compileState).toBe("ok");
    expect(response.note).toContain("Read the tables again");
    expect(response.refreshed).toBeUndefined();
  });

  it("reports a compilation already running as compiling, with a note to call again", async () => {
    harness.mockHttp.onGet(projectPath).reply(200, withStatus({ projectId, branch: "main", compileState: "compiling" }));

    const response = dataOf(await executeTool("project_status", { projectId, response_format: "json" }, harness.client));

    expect(response.compileState).toBe("compiling");
    expect(response.note).toContain("Call openl_project_status again");
    expect(harness.mockHttp.history.get.some((request) => request.url.endsWith("/tables"))).toBe(false);
  });

  it("reports an idle project as compiling when its compilation outlasts timeoutMs", async () => {
    harness.mockHttp
      .onGet(projectPath)
      .replyOnce(200, withStatus({ projectId, branch: "main", compileState: "idle" }))
      .onGet(projectPath)
      .replyOnce(200, withStatus({ projectId, branch: "main", compileState: "compiling" }));
    harness.mockHttp.onGet(`${projectPath}/tables`).timeout();

    const response = dataOf(await executeTool("project_status", {
      projectId, timeoutMs: 5_000, response_format: "json",
    }, harness.client));

    expect(response.compileState).toBe("compiling");
    expect(response.note).toContain("Call openl_project_status again");
    expect(harness.mockHttp.history.get.find((request) => request.url.endsWith("/tables"))?.timeout).toBe(5_000);
  });

  it("switches an opened project to the requested branch before reading its state", async () => {
    let openedBranch = "main";
    harness.mockHttp.onGet(projectPath).reply((config) => [200, config.params?.include
      ? withStatus({ ...compiled("ok"), branch: openedBranch })
      : { id: projectId, repository: "design", status: "OPENED", branch: openedBranch }]);
    harness.mockHttp.onPatch(projectPath).reply((config) => {
      openedBranch = bodyOf(config).branch;
      return [204];
    });

    const result = await executeTool("project_status", { projectId, branch: "develop", response_format: "json" }, harness.client);

    expect(dataOf(result).branch).toBe("develop");
    expect(harness.mockHttp.history.patch).toHaveLength(1);
  });

  it("reads the status of the branch the project is opened on without switching it", async () => {
    harness.mockHttp.onGet(projectPath).reply(200, withStatus({ ...compiled("ok"), branch: "develop" }));

    const result = await executeTool("project_status", { projectId, branch: "develop", response_format: "json" }, harness.client);

    expect(dataOf(result)).toMatchObject({ compileState: "ok", branch: "develop" });
    expect(harness.mockHttp.history.patch).toHaveLength(0);
  });

  it("reads a final compile state once, without compiling the project", async () => {
    harness.mockHttp.onGet(projectPath).reply(200, withStatus(compiled("errors", [
      { id: 7, summary: "Datatype 'Foo' not found", severity: "ERROR" },
    ])));

    const response = dataOf(await executeTool("project_status", { projectId, response_format: "json" }, harness.client));

    expect(response.compileState).toBe("errors");
    expect(response.compilation.messages.items).toEqual([{ id: 7, summary: "Datatype 'Foo' not found", severity: "ERROR" }]);
    expect(harness.mockHttp.history.get).toHaveLength(1);
  });

  it("drops the messages of a successful compilation and keeps its counts", async () => {
    const fixture: ProjectStatusView = {
      projectId,
      branch: "main",
      compileState: "ok",
      compilation: {
        messages: { items: [{ id: 1, summary: "INFO_ITEM", severity: "INFO" }], total: 1, errors: 0, warnings: 0 },
        modules: { total: 2, compiled: 2, compiledModules: ["A", "B"] },
        tests: { total: 7 },
      },
    };
    harness.mockHttp.onGet(projectPath).reply(200, withStatus(fixture));

    const response = dataOf(await executeTool("project_status", { projectId, response_format: "json" }, harness.client));

    expect(response.compilation).toEqual({
      messages: { items: [], total: 1, errors: 0, warnings: 0 },
      modules: { total: 2, compiled: 2, compiledModules: ["A", "B"] },
      tests: { total: 7 },
    });
  });

  it.each([
    [{}, ["ERROR_THIRD", "ERROR_FIFTH", "WARN_FIRST", "WARN_FOURTH", "INFO_SECOND"]],
    [{ severity: ["ERROR", "INFO"] }, ["ERROR_THIRD", "ERROR_FIFTH", "INFO_SECOND"]],
    [{ maxMessages: 3 }, ["ERROR_THIRD", "ERROR_FIFTH", "WARN_FIRST"]],
  ])("sorts the messages by severity, then filters and caps them as %j asks", async (options, summaries) => {
    // Backend returns items in id-ascending order — WARNs first, then ERRORs.
    harness.mockHttp.onGet(projectPath).reply(200, withStatus(compiled("errors", [
      { id: 1, summary: "WARN_FIRST", severity: "WARN" },
      { id: 2, summary: "INFO_SECOND", severity: "INFO" },
      { id: 3, summary: "ERROR_THIRD", severity: "ERROR" },
      { id: 4, summary: "WARN_FOURTH", severity: "WARN" },
      { id: 5, summary: "ERROR_FIFTH", severity: "ERROR" },
    ])));

    const response = dataOf(await executeTool("project_status", {
      projectId, ...options, response_format: "json",
    }, harness.client));

    expect(response.compilation.messages.items.map((item: { summary: string }) => item.summary)).toEqual(summaries);
  });
});

describe("open, save and close", () => {
  it.each([
    [{}, { status: "OPENED" }, "Project opened successfully"],
    [{ branch: "develop" }, { status: "OPENED", branch: "develop" }, "Project opened successfully on branch 'develop'"],
    [{ revision: "abc123" }, { status: "OPENED", revision: "abc123" }, "Project opened successfully at revision 'abc123'"],
  ])("openl_open_project opens a closed project as %j asks", async (args, patch, message) => {
    harness.mockHttp.onGet(lifecyclePath).reply(200, projectView("CLOSED"));
    harness.mockHttp.onPatch(lifecyclePath).reply(204);

    const result = await executeTool("open_project", { projectId: "design-project1", ...args, response_format: "json" }, harness.client);

    expect(harness.mockHttp.history.patch.map(bodyOf)).toEqual([patch]);
    expect(dataOf(result)).toEqual({ success: true, message });
  });

  it("openl_open_project switches the branch alone of an opened project", async () => {
    harness.mockHttp.onGet(lifecyclePath).reply(200, projectView("OPENED"));
    harness.mockHttp.onPatch(lifecyclePath).reply(204);

    const result = await executeTool("open_project", {
      projectId: "design-project1", branch: "develop", response_format: "json",
    }, harness.client);

    expect(harness.mockHttp.history.get).toHaveLength(1);
    expect(harness.mockHttp.history.patch.map(bodyOf)).toEqual([{ branch: "develop" }]);
    expect(dataOf(result)).toEqual({ success: true, message: "Branch switched to 'develop' successfully" });
  });

  it("openl_open_project opens an opened project again on the branch at the revision asked for (EPBDS-16798)", async () => {
    harness.mockHttp.onGet(lifecyclePath).reply(200, projectView("OPENED"));
    harness.mockHttp.onPatch(lifecyclePath).reply(204);

    const result = await executeTool("open_project", {
      projectId: "design-project1", branch: "develop", revision: "abc123", openDependencies: true, response_format: "json",
    }, harness.client);

    expect(harness.mockHttp.history.patch.map(bodyOf)).toEqual([
      { status: "OPENED", revision: "abc123", openDependencies: true, branch: "develop" },
    ]);
    expect(dataOf(result)).toEqual({
      success: true,
      message: "Project opened successfully on branch 'develop' at revision 'abc123'",
    });
  });

  it("openl_open_project reports why it failed to open on a branch, without a second attempt", async () => {
    harness.mockHttp.onGet(lifecyclePath).reply(200, projectView("CLOSED"));
    harness.mockHttp.onPatch(lifecyclePath).reply(404, { message: "Branch 'missing' is not found." });

    await expect(executeTool("open_project", {
      projectId: "design-project1",
      branch: "missing",
    }, harness.client)).rejects.toThrow(/Branch 'missing' is not found/);
    expect(harness.mockHttp.history.patch).toHaveLength(1);
  });

  it("openl_save_project saves and closes the project in one PATCH", async () => {
    harness.mockHttp.onGet(lifecyclePath).reply(200, projectView("EDITING"));
    harness.mockHttp.onPatch(lifecyclePath).reply(204);

    const result = await executeTool("save_project", {
      projectId: "design-project1",
      comment: "Save and close",
      closeAfterSave: true,
    }, harness.client);

    expect(harness.mockHttp.history.patch.map(bodyOf)).toEqual([{ save: true, comment: "Save and close", status: "CLOSED" }]);
    expect(textOf(result)).toContain("success");
    expect(textOf(result)).toContain("Save and close");
  });

  it("openl_save_project omits the comment for a file repository", async () => {
    harness.mockHttp.onGet(lifecyclePath).reply(200, projectView("EDITING", {
      repositoryInfo: { name: "File", features: { comments: false } },
    }));
    harness.mockHttp.onPatch(lifecyclePath).reply(204);

    const result = await executeTool("save_project", {
      projectId: "design-project1", response_format: "json",
    }, harness.client);

    expect(harness.mockHttp.history.patch.map(bodyOf)).toEqual([{ save: true }]);
    expect(dataOf(result)).toEqual({ success: true, message: "Project saved" });
  });

  it("openl_save_project closes a project without unsaved changes when closeAfterSave asks for it", async () => {
    harness.mockHttp.onGet(lifecyclePath).reply(200, projectView("OPENED"));
    harness.mockHttp.onPatch(lifecyclePath).reply(204);

    const result = await executeTool("save_project", {
      projectId: "design-project1", comment: "Save and close", closeAfterSave: true, response_format: "json",
    }, harness.client);

    expect(harness.mockHttp.history.patch.map(bodyOf)).toEqual([{ status: "CLOSED" }]);
    expect(dataOf(result)).toEqual({
      success: true,
      message: "There are no changes in the project; nothing to save. The project is closed.",
    });
  });

  it.each([
    ["a project without unsaved changes", "OPENED", {}, { status: "CLOSED" }, "Project closed successfully"],
    ["saving the changes", "EDITING", { saveChanges: true, comment: "Save before close" },
      { save: true, comment: "Save before close", status: "CLOSED" },
      'Project saved and closed successfully with comment: "Save before close"'],
    ["discarding the changes", "EDITING", { discardChanges: true, confirmDiscard: true },
      { status: "CLOSED", discardChanges: true }, "Project closed (unsaved changes discarded)"],
  ] as const)("openl_close_project closes %s in one PATCH", async (_case, status, args, patch, message) => {
    harness.mockHttp.onGet(lifecyclePath).reply(200, projectView(status));
    harness.mockHttp.onPatch(lifecyclePath).reply(204);

    const result = await executeTool("close_project", {
      projectId: "design-project1", ...args, response_format: "json",
    }, harness.client);

    expect(harness.mockHttp.history.patch.map(bodyOf)).toEqual([patch]);
    expect(dataOf(result)).toEqual({ success: true, message });
  });

  it("openl_close_project saves and closes a file repository project without a comment", async () => {
    harness.mockHttp.onGet(lifecyclePath).reply(200, projectView("EDITING", {
      repositoryInfo: { name: "File", features: { comments: false } },
    }));
    harness.mockHttp.onPatch(lifecyclePath).reply(204);

    const result = await executeTool("close_project", {
      projectId: "design-project1", saveChanges: true, response_format: "json",
    }, harness.client);

    expect(harness.mockHttp.history.patch.map(bodyOf)).toEqual([{ save: true, status: "CLOSED" }]);
    expect(dataOf(result)).toEqual({ success: true, message: "Project saved and closed successfully" });
  });

  it("openl_close_project asks for confirmDiscard before discarding unsaved changes", async () => {
    harness.mockHttp.onGet(lifecyclePath).reply(200, projectView("EDITING"));

    const result = await executeTool("close_project", {
      projectId: "design-project1", discardChanges: true, response_format: "json",
    }, harness.client);

    expect(dataOf(result)).toMatchObject({ success: false, confirmationRequired: true });
    expect(dataOf(result).message).toContain("confirmDiscard");
    expect(harness.mockHttp.history.patch).toHaveLength(0);
  });

  it.each([
    ["neither saveChanges nor discardChanges", {}, /unsaved changes.*saveChanges.*discardChanges/s],
    ["saveChanges without a comment", { saveChanges: true }, /comment is required when saveChanges is true/],
    ["both saveChanges and discardChanges", { saveChanges: true, discardChanges: true, comment: "c" },
      /Cannot set both saveChanges and discardChanges/],
  ])("openl_close_project refuses to close unsaved changes with %s", async (_case, args, message) => {
    harness.mockHttp.onGet(lifecyclePath).reply(200, projectView("EDITING"));

    await expect(executeTool("close_project", { projectId: "design-project1", ...args }, harness.client)).rejects.toThrow(message);
    expect(harness.mockHttp.history.patch).toHaveLength(0);
  });
});

describe("branches, creation and copies", () => {
  /** The id Studio answers a create or a copy with, the one the project list shows. */
  const createdId = "T19W2T33ZAYMF";

  it.each([
    ["abc123", "Successfully created branch 'feature/test-branch' from revision abc123"],
    [undefined, "Successfully created branch 'feature/test-branch'"],
  ])("openl_create_project_branch creates the branch from the revision %s", async (revision, message) => {
    harness.mockHttp.onPost(`${projectPath}/branches`).reply(201);

    const result = await executeTool("create_project_branch", {
      projectId, branchName: "feature/test-branch", revision, response_format: "json",
    }, harness.client);

    expect(bodyOf(harness.mockHttp.history.post[0])).toEqual({ branch: "feature/test-branch", revision });
    expect(dataOf(result)).toMatchObject({ success: true, message });
  });

  it.each([undefined, "dev"])("openl_create_project creates an empty project on the branch %s and answers its id", async (branch) => {
    harness.mockHttp.onGet("/repos").reply(200, mockRepositories);
    harness.mockHttp.onPut("/repos/design/projects/Offer-CW").reply(200, { id: createdId, revision: "abc123", branch: branch ?? "main" });

    const result = await executeTool("create_project", {
      repository: "Design Repository", projectName: "Offer-CW", branch, response_format: "json",
    }, harness.client);

    expect(dataOf(result)).toMatchObject({
      mode: "create",
      projectId: createdId,
      projectName: "Offer-CW",
      branch: branch ?? "main",
      revision: "abc123",
    });
  });

  it.each([undefined, "dev"])("openl_create_project copies a project on Studio onto the branch %s", async (branch) => {
    const sourceProjectId = "mapped:Offer-US:opaque-id";
    harness.mockHttp.onGet("/repos").reply(200, mockRepositories);
    // Studio finds the source project by its id: the copy names no repository of the source.
    harness.mockHttp.onPost("/repos/design/projects/Offer-CW/from-project", { sourceProject: sourceProjectId, branch })
      .reply(200, { id: createdId, revision: "def456", branch: branch ?? "main" });

    const result = await executeTool("create_project", {
      repository: "design", template: sourceProjectId, projectName: "Offer-CW", branch, response_format: "json",
    }, harness.client);

    expect(dataOf(result)).toMatchObject({
      mode: "copy",
      projectId: createdId,
      branch: branch ?? "main",
      revision: "def456",
      message: expect.stringContaining(`Copied '${sourceProjectId}'`),
    });
  });

  it.each([
    ["a create whose name is taken", undefined, "put", "/repos/design/projects/Existing", 409, /duplicated\.project\.message/],
    ["a copy whose destination is taken", "mapped:Offer-US:opaque-id", "post", "/repos/design/projects/Existing/from-project", 409,
      /duplicated\.project\.message/],
    ["a copy of a source that does not exist", "missing-project-id", "post", "/repos/design/projects/Existing/from-project", 404,
      /source project 'missing-project-id' was not found/],
  ] as const)("openl_create_project refuses %s with the reason", async (_case, template, method, url, status, message) => {
    harness.mockHttp.onGet("/repos").reply(200, mockRepositories);
    (method === "put" ? harness.mockHttp.onPut(url) : harness.mockHttp.onPost(url)).reply(status, { message: "duplicated.project.message" });

    await expect(executeTool("create_project", { repository: "design", template, projectName: "Existing" }, harness.client))
      .rejects.toThrow(message);
  });
});
