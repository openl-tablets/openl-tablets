/**
 * Integration tests of the project tools of src/handlers/project-handlers.ts: the list and the details of projects,
 * their compile status, open/save/close, branches, creation and copies, and their modules and worksheets.
 */

import { describe, it, expect } from "vitest";
import { executeTool, getAllTools } from "./index.js";
import type { ProjectStatusView, ProjectViewModel } from "../types.js";
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
  it("openl_list_projects resolves the repository by its display name and answers each id once, as projectId", async () => {
    harness.mockHttp.onGet("/repos").reply(200, mockRepositories);
    harness.mockHttp.onGet("/projects", { params: { repository: "design", offset: 0, size: 50 } })
      .reply(200, pageOf([projectView("OPENED", { id: "design:Project 1:hash123", name: "Project 1" })]));

    const result = await executeTool("list_projects", { repository: "Design Repository" }, harness.client);

    expect(textOf(result)).toBe("1 project.\n\n| projectId | name | repository | branch | status | modified |\n"
      + "| --- | --- | --- | --- | --- | --- |\n"
      + "| design:Project 1:hash123 | Project 1 | design |  | OPENED | 2024-01-01T00:00:00Z by admin |");
  });

  it("openl_list_projects leaves out the time or the author of a change Studio does not name", async () => {
    harness.mockHttp.onGet("/projects", { params: { offset: 0, size: 50 } }).reply(200, pageOf([
      projectView("CLOSED", { id: "p1", modifiedBy: undefined }),
      projectView("CLOSED", { id: "p2", modifiedAt: undefined }),
      projectView("CLOSED", { id: "p3", modifiedAt: undefined, modifiedBy: undefined }),
    ]));

    const text = textOf(await executeTool("list_projects", {}, harness.client));

    expect(text).toContain("| CLOSED | 2024-01-01T00:00:00Z |");
    expect(text).toContain("| CLOSED | by admin |");
    expect(text).toContain("| CLOSED |  |");
    expect(text).not.toContain("undefined");
  });

  it("openl_list_projects returns requested summary and status expansions", async () => {
    harness.mockHttp.onGet("/projects", {
      params: { include: ["summary", "status"], offset: 0, size: 50 },
    }).reply(200, {
      ...pageOf([projectView("OPENED", { compileStatus: { projectId: "p", compileState: "warnings" } })], { total: 1 }),
      repositoryCounts: [{ repository: "design", count: 3 }],
      statusCounts: { OPENED: 2, CLOSED: 1 },
      statuses: ["OPENED", "CLOSED"],
      tagCounts: [{ tag: "release", count: 1 }],
    });

    const result = await executeTool("list_projects", {
      include: ["summary", "status"],
    }, harness.client);

    // The status adds the compile state of each project; the summary of the listing follows the table
    expect(textOf(result)).toContain("| modified | compileState |");
    expect(textOf(result)).toContain("| 2024-01-01T00:00:00Z by admin | warnings |");
    expect(textOf(result)).toContain('Listing: {"repositoryCounts":[{"repository":"design","count":3}],'
      + '"statusCounts":{"OPENED":2,"CLOSED":1},"statuses":["OPENED","CLOSED"],"tagCounts":[{"tag":"release","count":1}]}');
  });

  it("openl_list_projects pages the projects Studio pages, also from an offset off the page grid", async () => {
    const allProjects = projects(120, "CLOSED");
    harness.mockHttp.onGet("/projects").reply((config) => {
      const offset = Number(config.params?.offset);
      const pageSize = Number(config.params?.size);
      const content = allProjects.slice(offset, offset + pageSize);
      return [200, pageOf(content, { pageNumber: Math.floor(offset / pageSize), pageSize, total: allProjects.length })];
    });

    const page = async (offset: number): Promise<string[]> =>
      textOf(await executeTool("list_projects", { limit: 50, offset }, harness.client)).split("\n");

    const [second, third, shifted] = [await page(50), await page(100), await page(25)];
    expect(second[0]).toBe("120 projects; 51–100 below, the next page at offset=100.");
    expect(second[4]).toContain("| p50 |");
    expect(third[0]).toBe("120 projects; 101–120 below.");
    expect(third.filter((line) => line.startsWith("| design:"))).toHaveLength(20);
    expect(shifted[0]).toBe("120 projects; 26–75 below, the next page at offset=75.");
    expect(shifted[4]).toContain("| p25 |");
  });

  it("openl_get_project answers the project Studio describes", async () => {
    harness.mockHttp.onGet(lifecyclePath).reply(200, projectView("OPENED"));

    const result = await executeTool("get_project", { projectId: "design-project1" }, harness.client);

    const { id, ...project } = projectView("OPENED");
    expect(dataOf(result)).toEqual({ projectId: id, ...project });
  });

  it("names the capabilities of a project it reads, leaving out one Studio marks unavailable", async () => {
    const capabilities = { canOpen: true, canSave: false, canDeploy: true } as ProjectViewModel["capabilities"];
    harness.mockHttp.onGet(lifecyclePath).reply(200, projectView("OPENED", { capabilities }));

    const project = dataOf(await executeTool("get_project", { projectId: "design-project1" }, harness.client));

    expect(project.capabilities).toEqual(["canOpen", "canDeploy"]);
  });

  it("answers the compile status of a project without what the project names already", async () => {
    const compileStatus = {
      projectId: "design:project1:hash123", branch: "main", revision: "r1", compileState: "ok",
      lastModifiedBy: { author: "admin", date: "2024-01-01T00:00:00Z" },
    };
    harness.mockHttp.onGet(lifecyclePath).reply(200, projectView("OPENED", {
      branch: "main", revision: "r1", repositoryInfo: { name: "Design" }, compileStatus,
    } as Partial<ProjectViewModel>));
    const project = dataOf(await executeTool("get_project", { projectId: "design-project1" }, harness.client));

    expect(project.compileStatus).toEqual({ compileState: "ok" });
    // openl_get_project keeps the repository of its one project
    expect(project.repositoryInfo).toEqual({ name: "Design" });
  });

  it("openl_list_project_modules and openl_list_module_sheets read the modules and the worksheets of a project", async () => {
    const modules = [{ name: "Main", path: "rules/*.xlsx", modules: [{ name: "Rates", path: "rules/Rates.xlsx" }] }];
    harness.mockHttp.onGet("/projects/p1/modules").reply(200, modules);
    harness.mockHttp.onGet("/projects/p1/modules/Main%20Rules/sheets").reply(200, ["Rates", "Tests"]);

    const moduleResult = await executeTool("list_project_modules", { projectId: "p1" }, harness.client);
    const sheetResult = await executeTool("list_module_sheets", {
      projectId: "p1", module: "Main Rules", }, harness.client);

    // A module a pattern matches is listed beside the module of the pattern
    expect(textOf(moduleResult)).toBe("2 modules.\n\n| module | path |\n| --- | --- |\n| Main | rules/*.xlsx |\n"
      + "| Rates | rules/Rates.xlsx |");
    expect(textOf(sheetResult)).toBe("2 sheets.\n\n| sheet |\n| --- |\n| Rates |\n| Tests |");
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

    const response = dataOf(await executeTool("project_status", { projectId }, harness.client));

    expect(response.compileState).toBe("ok");
    expect(response.compilation.tests.total).toBe(34);
    expect(harness.mockHttp.history.get.filter((request) => request.url.endsWith("/tables"))).toHaveLength(1);
    // The default path above starts compilation, so MCP clients must not be
    // told that the tool is unconditionally read-only.
    expect(getAllTools().find((tool) => tool.name === "project_status")?.annotations?.readOnlyHint).toBe(false);
  });

  it("cuts many compile messages to the limit and names the arguments that choose them", async () => {
    const items = Array.from({ length: 400 }, (_, id) => ({ id, summary: `Field 'f${id}' is not defined.${" ".repeat(60)}`, severity: "ERROR" }));
    harness.mockHttp.onGet(projectPath).reply(200, withStatus(compiled("errors", items)));

    const response = JSON.parse(textOf(await executeTool("project_status", { projectId }, harness.client)));

    expect(response.truncated).toBe(true);
    expect(response.truncation_message).toMatch(/of 400 compile messages; choose the messages to read with severity or limit\.$/);
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

    const response = dataOf(await executeTool("project_status", { projectId }, harness.client));

    expect(harness.mockHttp.history.post).toHaveLength(1);
    expect(response.compileState).toBe("ok");
    expect(response.note).toContain("Read the tables again");
    expect(response.refreshed).toBeUndefined();
  });

  it("reports a compilation already running as compiling, with a note to call again", async () => {
    harness.mockHttp.onGet(projectPath).reply(200, withStatus({ projectId, branch: "main", compileState: "compiling" }));

    const response = dataOf(await executeTool("project_status", { projectId }, harness.client));

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
      projectId, timeoutMs: 5_000, }, harness.client));

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

    const result = await executeTool("project_status", { projectId, branch: "develop" }, harness.client);

    expect(dataOf(result).branch).toBe("develop");
    expect(harness.mockHttp.history.patch).toHaveLength(1);
  });

  it("reads the status of the branch the project is opened on without switching it", async () => {
    harness.mockHttp.onGet(projectPath).reply(200, withStatus({ ...compiled("ok"), branch: "develop" }));

    const result = await executeTool("project_status", { projectId, branch: "develop" }, harness.client);

    expect(dataOf(result)).toMatchObject({ compileState: "ok", branch: "develop" });
    expect(harness.mockHttp.history.patch).toHaveLength(0);
  });

  it("reads a final compile state once, without compiling the project", async () => {
    harness.mockHttp.onGet(projectPath).reply(200, withStatus(compiled("errors", [
      { id: 7, summary: "Datatype 'Foo' not found", severity: "ERROR" },
    ])));

    const response = dataOf(await executeTool("project_status", { projectId }, harness.client));

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

    const response = dataOf(await executeTool("project_status", { projectId }, harness.client));

    expect(response.compilation).toEqual({
      messages: { items: [], total: 1, errors: 0, warnings: 0 },
      modules: { total: 2, compiled: 2, compiledModules: ["A", "B"] },
      tests: { total: 7 },
    });
  });

  it.each([
    [{}, ["ERROR_THIRD", "ERROR_FIFTH", "WARN_FIRST", "WARN_FOURTH", "INFO_SECOND"]],
    [{ severity: ["ERROR", "INFO"] }, ["ERROR_THIRD", "ERROR_FIFTH", "INFO_SECOND"]],
    [{ limit: 3 }, ["ERROR_THIRD", "ERROR_FIFTH", "WARN_FIRST"]],
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
      projectId, ...options, }, harness.client));

    expect(response.compilation.messages.items.map((item: { summary: string }) => item.summary)).toEqual(summaries);
  });

  it("cuts a status over the size limit by whole messages, errors first, and says how to read the rest", async () => {
    const items = Array.from({ length: 400 }, (_, id) => ({
      id, summary: `${id % 2 ? "WARN" : "ERROR"} ${"x".repeat(100)}`, severity: id % 2 ? "WARN" : "ERROR",
    }));
    harness.mockHttp.onGet(projectPath).reply(200, withStatus(compiled("errors", items)));

    const text = textOf(await executeTool("project_status", { projectId }, harness.client));

    const response = JSON.parse(text);
    expect(text.length).toBeLessThanOrEqual(25_000);
    expect(response.data.compileState).toBe("errors");
    expect(response.data.compilation.messages.items.length).toBeLessThan(400);
    expect(response.data.compilation.messages.items[0].severity).toBe("ERROR");
    expect(response.truncation_message).toContain("compile messages; choose the messages to read with severity");
  });
});

describe("open, save and close", () => {
  it.each([
    [{}, { status: "OPENED" }],
    [{ branch: "develop" }, { status: "OPENED", branch: "develop" }],
    [{ revision: "abc123" }, { status: "OPENED", revision: "abc123" }],
  ])("openl_open_project opens a closed project as %j asks", async (args, patch) => {
    harness.mockHttp.onGet(lifecyclePath).reply(200, projectView("CLOSED"));
    harness.mockHttp.onPatch(lifecyclePath).reply(204);

    const result = await executeTool("open_project", { projectId: "design-project1", ...args }, harness.client);

    expect(harness.mockHttp.history.patch.map(bodyOf)).toEqual([patch]);
    expect(dataOf(result)).toEqual({ opened: true });
  });

  it("openl_open_project leaves an opened project as it is, which Studio would refuse to open again", async () => {
    harness.mockHttp.onGet(lifecyclePath).reply(200, projectView("EDITING"));

    const result = await executeTool("open_project", { projectId: "design-project1" }, harness.client);

    expect(harness.mockHttp.history.patch).toHaveLength(0);
    expect(dataOf(result)).toEqual({ alreadyOpen: true });
  });

  it("openl_open_project switches the branch alone of an opened project", async () => {
    harness.mockHttp.onGet(lifecyclePath).reply(200, projectView("OPENED"));
    harness.mockHttp.onPatch(lifecyclePath).reply(204);

    const result = await executeTool("open_project", {
      projectId: "design-project1", branch: "develop", }, harness.client);

    expect(harness.mockHttp.history.get).toHaveLength(1);
    expect(harness.mockHttp.history.patch.map(bodyOf)).toEqual([{ branch: "develop" }]);
    expect(dataOf(result)).toEqual({ opened: true });
  });

  it("openl_open_project opens an opened project again on the branch at the revision asked for (EPBDS-16798)", async () => {
    harness.mockHttp.onGet(lifecyclePath).reply(200, projectView("OPENED"));
    harness.mockHttp.onPatch(lifecyclePath).reply(204);

    const result = await executeTool("open_project", {
      projectId: "design-project1", branch: "develop", revision: "abc123", openDependencies: true, }, harness.client);

    expect(harness.mockHttp.history.patch.map(bodyOf)).toEqual([
      { status: "OPENED", revision: "abc123", openDependencies: true, branch: "develop" },
    ]);
    expect(dataOf(result)).toEqual({ opened: true });
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
    expect(dataOf(result)).toEqual({ saved: true, closed: true });
  });

  it("openl_save_project omits the comment for a file repository", async () => {
    harness.mockHttp.onGet(lifecyclePath).reply(200, projectView("EDITING", {
      repositoryInfo: { name: "File", features: { comments: false } },
    }));
    harness.mockHttp.onPatch(lifecyclePath).reply(204);

    const result = await executeTool("save_project", {
      projectId: "design-project1", }, harness.client);

    expect(harness.mockHttp.history.patch.map(bodyOf)).toEqual([{ save: true }]);
    expect(dataOf(result)).toEqual({ saved: true });
  });

  it("openl_save_project closes a project without unsaved changes when closeAfterSave asks for it", async () => {
    harness.mockHttp.onGet(lifecyclePath).reply(200, projectView("OPENED"));
    harness.mockHttp.onPatch(lifecyclePath).reply(204);

    const result = await executeTool("save_project", {
      projectId: "design-project1", comment: "Save and close", closeAfterSave: true, }, harness.client);

    expect(harness.mockHttp.history.patch.map(bodyOf)).toEqual([{ status: "CLOSED" }]);
    expect(dataOf(result)).toEqual({ saved: false, closed: true });
  });

  it.each([
    ["a project without unsaved changes", "OPENED", {}, { status: "CLOSED" }, { closed: true }],
    ["saving the changes", "EDITING", { saveChanges: true, comment: "Save before close" },
      { save: true, comment: "Save before close", status: "CLOSED" }, { closed: true, saved: true }],
    ["discarding the changes", "EDITING", { discardChanges: true, confirmDiscard: true },
      { status: "CLOSED", discardChanges: true }, { closed: true, discarded: true }],
  ] as const)("openl_close_project closes %s in one PATCH", async (_case, status, args, patch, answer) => {
    harness.mockHttp.onGet(lifecyclePath).reply(200, projectView(status));
    harness.mockHttp.onPatch(lifecyclePath).reply(204);

    const result = await executeTool("close_project", {
      projectId: "design-project1", ...args, }, harness.client);

    expect(harness.mockHttp.history.patch.map(bodyOf)).toEqual([patch]);
    expect(dataOf(result)).toEqual(answer);
  });

  it("openl_close_project saves and closes a file repository project without a comment", async () => {
    harness.mockHttp.onGet(lifecyclePath).reply(200, projectView("EDITING", {
      repositoryInfo: { name: "File", features: { comments: false } },
    }));
    harness.mockHttp.onPatch(lifecyclePath).reply(204);

    const result = await executeTool("close_project", {
      projectId: "design-project1", saveChanges: true, }, harness.client);

    expect(harness.mockHttp.history.patch.map(bodyOf)).toEqual([{ save: true, status: "CLOSED" }]);
    expect(dataOf(result)).toEqual({ closed: true, saved: true });
  });

  it("openl_close_project asks for confirmDiscard before discarding unsaved changes", async () => {
    harness.mockHttp.onGet(lifecyclePath).reply(200, projectView("EDITING"));

    const result = await executeTool("close_project", {
      projectId: "design-project1", discardChanges: true, }, harness.client);

    expect(dataOf(result)).toMatchObject({ confirmationRequired: true });
    expect(dataOf(result)).not.toHaveProperty("success");
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

  it.each(["abc123", undefined])("openl_create_project_branch creates the branch from the revision %s", async (revision) => {
    harness.mockHttp.onPost(`${projectPath}/branches`).reply(201);

    const result = await executeTool("create_project_branch", {
      projectId, branch: "feature/test-branch", revision, }, harness.client);

    expect(bodyOf(harness.mockHttp.history.post[0])).toEqual({ branch: "feature/test-branch", revision });
    expect(dataOf(result)).toEqual({ created: "feature/test-branch" });
  });

  it.each([undefined, "dev"])("openl_create_project creates an empty project on the branch %s and answers its id", async (branch) => {
    harness.mockHttp.onGet("/repos").reply(200, mockRepositories);
    harness.mockHttp.onPut("/repos/design/projects/Offer-CW").reply(200, { id: createdId, revision: "abc123", branch: branch ?? "main" });

    const result = await executeTool("create_project", {
      repository: "Design Repository", name: "Offer-CW", branch, }, harness.client);

    expect(dataOf(result)).toEqual({ projectId: createdId, branch: branch ?? "main", revision: "abc123" });
  });

  it.each([undefined, "dev"])("openl_create_project copies a project on Studio onto the branch %s", async (branch) => {
    const sourceProjectId = "mapped:Offer-US:opaque-id";
    harness.mockHttp.onGet("/repos").reply(200, mockRepositories);
    // Studio finds the source project by its id: the copy names no repository of the source.
    harness.mockHttp.onPost("/repos/design/projects/Offer-CW/from-project", { sourceProject: sourceProjectId, branch })
      .reply(200, { id: createdId, revision: "def456", branch: branch ?? "main" });

    const result = await executeTool("create_project", {
      repository: "design", template: sourceProjectId, name: "Offer-CW", branch, }, harness.client);

    expect(dataOf(result)).toEqual({ projectId: createdId, branch: branch ?? "main", revision: "def456" });
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

    await expect(executeTool("create_project", { repository: "design", template, name: "Existing" }, harness.client))
      .rejects.toThrow(message);
  });
});
