/**
 * Unit tests for client.ts: the requests the OpenL Studio REST client sends and what it makes of the answers, on a
 * mocked HTTP transport. The tools that pass a call through unchanged are tested by the tests of their handlers only.
 */

import { describe, it, expect, beforeEach } from "vitest";
import { bodyOf, MockHttp, queryOf } from "./testing/mock-http.js";
import { OpenLClient, normalizeOpenLBaseUrl } from "./client.js";
import { buildUrl } from "./http.js";
import type * as Types from "./types.js";
import { pageOf, projectView, suspendedStack } from "./testing/openl-api-mocks.js";

describe("normalizeOpenLBaseUrl", () => {
  it.each([
    ["http://localhost:8080", "http://localhost:8080/rest"],
    ["http://localhost:8080/rest", "http://localhost:8080/rest"],
    ["http://localhost:8080/studio", "http://localhost:8080/studio/rest"],
    ["http://localhost:8080/", "http://localhost:8080/rest"],
    ["http://localhost:8080////", "http://localhost:8080/rest"],
    ["http://localhost:8080/studio///", "http://localhost:8080/studio/rest"],
  ])("calls the REST API of %s at %s", (address, rest) => {
    expect(normalizeOpenLBaseUrl(address)).toBe(rest);
  });

  it.each(["", "not-a-url", "/relative/path"])("refuses the address %j", (address) => {
    expect(() => normalizeOpenLBaseUrl(address)).toThrow(/Invalid address of OpenL Studio/);
  });
});

describe("OpenLClient", () => {
  let client: OpenLClient;
  let mockHttp: MockHttp;

  /** No request of any method reached OpenL Studio. */
  const expectNoRequest = (): void => expect(mockHttp.requests).toHaveLength(0);

  beforeEach(() => {
    client = new OpenLClient({ baseUrl: "http://localhost:8080", authorization: "Token openl_pat_test" });
    mockHttp = new MockHttp(client);
  });

  describe("repositories", () => {
    const repos: Types.Repository[] = [
      { id: "design", name: "Design Repository" },
      { id: "production", name: "Production Repository" },
    ];

    it("reads the design repositories once for the client", async () => {
      mockHttp.onGet("/repos").reply(200, repos);

      await expect(client.listRepositories()).resolves.toEqual(repos);
      await expect(client.getRepositoryIdByName("design")).resolves.toBe("design");
      await expect(client.getRepositoryFeatures("design")).resolves.toEqual({});

      expect(mockHttp.history.get).toHaveLength(1);
    });

    it.each([
      ["an exact id", "design"],
      ["an exact display name", "Design Repository"],
      ["a case-insensitive id", "DESIGN"],
      ["a case-insensitive display name", "design repository"],
    ])("resolves %s to the id of the repository", async (_case, name) => {
      mockHttp.onGet("/repos").reply(200, repos);

      await expect(client.getRepositoryIdByName(name)).resolves.toBe("design");
    });

    it("throws with a helpful message listing id + name pairs when no repository matches", async () => {
      mockHttp.onGet("/repos").reply(200, repos);

      await expect(client.getRepositoryIdByName("ghost")).rejects.toThrow(
        /Repository "ghost" not found.*design \(Design Repository\), production \(Production Repository\)/,
      );
      await expect(client.getRepositoryFeatures("ghost")).rejects.toThrow(/not found/);
    });

    it("names no available repository when Studio has none", async () => {
      mockHttp.onGet("/repos").reply(200, []);

      await expect(client.getRepositoryIdByName("design")).rejects.toThrow(/Available repositories: none\./);
    });

    it("reads the branches of a repository by its URL-encoded name", async () => {
      mockHttp.onGet("/repos/my%20repo/branches").reply(200, ["main", "development"]);

      await expect(client.listBranches("my repo")).resolves.toEqual(["main", "development"]);
    });

    describe("getProjectRevisions", () => {
      const page = pageOf([{
        revisionNo: "abc123",
        createdAt: "2026-01-01T00:00:00Z",
        fullComment: "Initial",
        deleted: false,
        technicalRevision: false,
      }], { total: 1 });

      it("uses the project-level history URL and forwards filter params", async () => {
        const revisionProjectId = "design:Insurance Rules:abc/123";
        mockHttp.onGet(`/projects/${encodeURIComponent(revisionProjectId)}/history`).reply(200, page);

        const result = await client.getProjectRevisions(revisionProjectId, {
          search: "premium",
          techRevs: false,
          page: 0,
          size: 50,
        });

        expect(mockHttp.history.get[0].params).toEqual({ search: "premium", techRevs: false, page: 0, size: 50 });
        expect(result.content[0].revisionNo).toBe("abc123");
      });
    });
  });

  describe("projects", () => {
    it("reads a page of projects with the metadata Studio adds to it", async () => {
      const content = Array.from({ length: 50 }, (_, i) =>
        projectView("CLOSED", { id: `design:p${i + 50}:hash${i + 50}`, name: `p${i + 50}` }));
      const metadata = {
        projectIndexHealth: { healthy: true },
        repositoryCounts: [{ repository: "design", count: 120 }],
        statusCounts: { CLOSED: 120 },
        statuses: ["CLOSED"],
        tagCounts: [{ tag: "release", count: 3 }],
      };
      mockHttp.onGet("/projects", { params: { offset: 50, size: 50 } })
        .reply(200, { ...pageOf(content, { pageNumber: 1, total: 120 }), ...metadata });

      const result = await client.listProjectsPage({ offset: 50, limit: 50 });

      expect(result).toMatchObject({ pageNumber: 1, pageSize: 50, total: 120, metadata });
      expect(result.items).toHaveLength(50);
      expect(result.items[0].name).toBe("p50");
    });

    it("forwards the project filters, tags with their prefix, and the exact item offset", async () => {
      mockHttp.onGet("/projects").reply(200, pageOf([]));

      await client.listProjectsPage({
        repository: "design",
        status: "OPENED",
        tags: { tag: "v1.0" },
        dependsOn: "design:base",
        name: "rate",
        author: "admin",
        branch: "feature",
        sort: "updated",
        include: ["status", "descriptor"],
        offset: 25,
        limit: 50,
      });

      const [request] = mockHttp.history.get;
      expect(request.params).toEqual({
        repository: "design",
        status: "OPENED",
        "tags.tag": "v1.0",
        dependsOn: "design:base",
        name: "rate",
        author: "admin",
        branch: "feature",
        sort: "updated",
        include: ["status", "descriptor"],
        offset: 25,
        size: 50,
      });
      expect(buildUrl(request)).toContain("include=status&include=descriptor");
    });

    it("addresses a project by its encoded id and forwards the include expansions", async () => {
      mockHttp.onGet("/projects/design-Example%201%20-%20Bank%20Rating").reply(200, projectView("OPENED"));

      await client.getProject("design-Example 1 - Bank Rating", ["status", "descriptor"]);

      expect(mockHttp.history.get[0].params).toEqual({ include: ["status", "descriptor"] });
      expect(buildUrl(mockHttp.history.get[0])).toContain("include=status&include=descriptor");
    });

    it("reads the status from the project Studio returns with include=status", async () => {
      const status: Types.ProjectStatusView = {
        projectId: "design-AutoInsurance",
        branch: "main",
        revision: "abc123",
        compileState: "ok",
        compilation: {
          messages: { items: [], total: 0, errors: 0, warnings: 0 },
          modules: { total: 1, compiled: 1, compiledModules: ["Main"] },
          tests: { total: 5 },
        },
      };
      mockHttp.onGet("/projects/design-AutoInsurance", { params: { include: ["status"] } })
        .reply(200, { id: "design-AutoInsurance", name: "AutoInsurance", compileStatus: status });

      await expect(client.getProjectStatus("design-AutoInsurance")).resolves.toEqual(status);
    });

    it("triggers lazy compilation by listing a single table", async () => {
      const controller = new AbortController();
      mockHttp.onGet("/projects/design-AutoInsurance/tables").reply(200, pageOf([], { pageSize: 1, total: 0 }));

      await client.triggerProjectCompilation("design-AutoInsurance", {
        signal: controller.signal,
        timeoutMs: 120_000,
      });

      expect(mockHttp.history.get[0]).toMatchObject({
        params: { offset: 0, size: 1 },
        signal: controller.signal,
        timeout: 120_000,
      });
    });

    it("deletes a project without reading it first", async () => {
      mockHttp.onDelete("/projects/design-project1").reply(204);

      await expect(client.deleteProject("design-project1")).resolves.toBeUndefined();
      expect(mockHttp.history.delete).toHaveLength(1);
      expect(mockHttp.history.get).toHaveLength(0);
    });

    it("lists the branches of a project, of the whole repository on request", async () => {
      mockHttp.onGet("/projects/design-project1/branches").reply(200, []);

      await client.listProjectBranches("design-project1");
      await client.listProjectBranches("design-project1", "repository");

      expect(mockHttp.history.get.map((request) => request.params?.scope)).toEqual([undefined, "repository"]);
    });

    it("rejects traversal segments of a branch to delete before issuing a request", async () => {
      await expect(client.deleteProjectBranch("design-project1", "feature/../../main", false)).rejects.toThrow(
        /project-relative|not allowed/,
      );
      expectNoRequest();
    });

    it("reads notReady while a merge runs and its result once it has ended", async () => {
      mockHttp.onGet("/projects/p1/merge").replyOnce(202, { status: "notReady" });
      mockHttp.onGet("/projects/p1/merge").replyOnce(200, { status: "success" });

      await expect(client.getMergeResult("p1")).resolves.toEqual({ status: "notReady" });
      await expect(client.getMergeResult("p1")).resolves.toEqual({ status: "success" });
    });
  });

  describe("open, save and close", () => {
    const projectPath = "/projects/design-project1";

    it("always sends status OPENED, also for a project opened already, with the revision given", async () => {
      mockHttp.onGet(projectPath).reply(200, projectView("OPENED"));
      mockHttp.onPatch(projectPath).reply(204);

      await client.openProject("design-project1", { revision: "abc123" });

      expect(mockHttp.history.patch.map(bodyOf)).toEqual([{ status: "OPENED", revision: "abc123" }]);
    });

    it("opens a closed project on the branch with the given options", async () => {
      mockHttp.onGet(projectPath).reply(200, projectView("CLOSED"));
      mockHttp.onPatch(projectPath).reply(204);

      const result = await client.openOnBranch("design-project1", "feature/new-rules", { revision: "abc" });

      expect(result).toBe("opened");
      expect(mockHttp.history.patch.map(bodyOf)).toEqual([{ status: "OPENED", revision: "abc", branch: "feature/new-rules" }]);
    });

    it.each([
      ["OPENED", { revision: "abc" }, { status: "OPENED", revision: "abc", branch: "feature/new-rules" }, "opened"],
      ["EDITING", { openDependencies: true }, { status: "OPENED", openDependencies: true, branch: "feature/new-rules" },
        "opened"],
      ["OPENED", {}, { branch: "feature/new-rules" }, "switched_branch"],
      ["EDITING", { openDependencies: false }, { branch: "feature/new-rules" }, "switched_branch"],
    ] as const)("puts an %s project on the branch with %j", async (status, options, patch, action) => {
      mockHttp.onGet(projectPath).reply(200, projectView(status));
      mockHttp.onPatch(projectPath).reply(204);

      const result = await client.openOnBranch("design-project1", "feature/new-rules", options);

      expect(result).toBe(action);
      expect(mockHttp.history.patch.map(bodyOf)).toEqual([patch]);
    });

    it.each([
      ["open", (projectId: string) => client.openProject(projectId)],
      ["open on a branch", (projectId: string) => client.openOnBranch(projectId, "main")],
      ["close", (projectId: string) => client.closeProject(projectId)],
    ])("refuses to %s a project of the local repository", async (_case, change) => {
      mockHttp.onGet("/projects/local-myproject").reply(200, projectView("OPENED", { id: "local:myproject:hash", repository: "local" }));

      await expect(change("local-myproject")).rejects.toThrow(/local repository.*not connected to a remote Git/i);
      expect(mockHttp.history.patch).toHaveLength(0);
    });

    it("short-circuits with 'nothing to save' and issues NO PATCH when the project is not EDITING", async () => {
      mockHttp.onGet(projectPath).reply(200, projectView("OPENED"));

      const result = await client.saveProject("design-project1", "irrelevant comment");

      expect(result.success).toBe(true);
      expect(result.message).toMatch(/nothing to save/i);
      expect(mockHttp.history.patch).toHaveLength(0);
    });

    it.each([
      ["closes an opened", "OPENED", [{ status: "CLOSED" }], "nothing to save. The project is closed."],
      ["leaves a closed", "CLOSED", [], "nothing to save."],
    ] as const)("%s project without unsaved changes when closeAfterSave asks for it", async (_case, status, patches, end) => {
      mockHttp.onGet(projectPath).reply(200, projectView(status));
      mockHttp.onPatch(projectPath).reply(204);

      const result = await client.saveProject("design-project1", "irrelevant comment", { closeAfterSave: true });

      expect(result.message.endsWith(end)).toBe(true);
      expect(mockHttp.history.patch.map(bodyOf)).toEqual(patches);
    });

    it("requires a non-empty comment when the project IS EDITING", async () => {
      mockHttp.onGet(projectPath).reply(200, projectView("EDITING"));

      await expect(client.saveProject("design-project1", "   ")).rejects.toThrow(/comment is required for save/i);
      expect(mockHttp.history.patch).toHaveLength(0);
    });

    it("saves a file repository project without a comment", async () => {
      mockHttp.onGet(projectPath).reply(200, projectView("EDITING", {
        repositoryInfo: { name: "File", features: { comments: false } },
      }));
      mockHttp.onPatch(projectPath).reply(204);

      const result = await client.saveProject("design-project1");

      expect(result).toEqual({ success: true, message: "Project saved" });
      expect(mockHttp.history.patch.map(bodyOf)).toEqual([{ save: true }]);
    });

    it("PATCHes the trimmed comment when EDITING after one read of the project", async () => {
      mockHttp.onGet(projectPath).reply(200, projectView("EDITING"));
      mockHttp.onPatch(projectPath).reply(204);

      const result = await client.saveProject("design-project1", "  Add premium rule  ");

      expect(result.success).toBe(true);
      // closeAfterSave was not requested -> no status field.
      expect(mockHttp.history.patch.map(bodyOf)).toEqual([{ save: true, comment: "Add premium rule" }]);
      expect(mockHttp.history.get.map((request) => request.url)).toEqual([projectPath]);
    });
  });

  describe("tables", () => {
    const tablesPath = "/projects/design-project1/tables";

    it("reads a page of the tables of a project with the filters given", async () => {
      mockHttp.onGet(tablesPath).reply(200, pageOf([
        { id: "calculatePremium_1234", name: "calculatePremium", tableType: "SimpleRules", kind: "Rules" },
      ]));

      const result = await client.listTablesPage("design-project1", { kind: ["Rules"], name: "calculate" });

      expect(result.items.map((table) => table.name)).toEqual(["calculatePremium"]);
      expect(mockHttp.history.get[0].params).toEqual({ kind: ["Rules"], name: "calculate" });
      expect(buildUrl(mockHttp.history.get[0])).toContain("kind=Rules");
    });

    it("passes startRow/maxRows/styles for a raw slice with styles, by the encoded table id", async () => {
      mockHttp.onGet(`${tablesPath}/table%20id`).reply(200, { id: "table id", source: [], totalRows: 200 });

      const result = await client.getTable("design-project1", "table id", { startRow: 10, maxRows: 50, styles: true });

      expect(mockHttp.history.get[0].params).toEqual({ raw: true, startRow: 10, maxRows: 50, cutMerges: true, styles: true });
      expect(result.totalRows).toBe(200);
    });

    it("answers the version of the table the ETag header names, beside the table", async () => {
      mockHttp.onGet(`${tablesPath}/calculatePremium_1234`)
        .reply(200, { id: "calculatePremium_1234", source: [] }, { etag: "\"v1\"" });

      const table = await client.getTable("design-project1", "calculatePremium_1234");

      expect(table.version).toBe("v1");
    });

    it("omits unset raw-view options and a false styles flag from the query", async () => {
      mockHttp.onGet(`${tablesPath}/calculatePremium_1234`).reply(200, { id: "calculatePremium_1234", source: [] });

      await client.getTable("design-project1", "calculatePremium_1234", { maxRows: 25, styles: false });

      expect(mockHttp.history.get[0].params).toEqual({ raw: true, maxRows: 25, cutMerges: true });
    });

    it("answers the table id Studio gives in the 200 answer to an action, at the encoded table path", async () => {
      mockHttp.onPost(`${tablesPath}/Customer%201234/actions`).reply(200, { id: "Customer_5678" });

      await expect(client.editTableSource("design-project1", "Customer 1234", {
        operation: "insert", target: { type: "row", position: 1 },
      }, "v1")).resolves.toBe("Customer_5678");
    });

    it("deletes a table by its encoded id", async () => {
      mockHttp.onDelete(`${tablesPath}/Customer%201234`).reply(204);

      await client.deleteTable("design-project1", "Customer 1234", "v1");

      expect(mockHttp.history.delete).toHaveLength(1);
      expect(mockHttp.history.delete[0].headers?.["If-Match"]).toBe('"v1"');
    });
  });

  describe("tests", () => {
    it("adds the number of passed tests to the summary of a run", async () => {
      mockHttp.onGet("/projects/design-project1/tests/summary").reply(200, {
        executionTimeMs: 42,
        numberOfTests: 5,
        numberOfFailures: 1,
        testCases: [],
      });

      await expect(client.getTestResultsSummary("design-project1")).resolves.toEqual({
        executionTimeMs: 42,
        numberOfTests: 5,
        numberOfFailures: 1,
        numberOfPassed: 4,
      });
    });

    it("starts the tests of a project of the local repository without opening it", async () => {
      mockHttp.onGet("/projects/local-project1").reply(200, projectView("CLOSED", { repository: "local" }));
      mockHttp.onPost("/projects/local-project1/tests/run").reply(202);

      const result = await client.startProjectTests("local-project1");

      expect(result).toMatchObject({ status: "started", projectId: "local-project1", projectWasOpened: false });
      expect(mockHttp.history.patch).toHaveLength(0);
    });
  });

  describe("project creation", () => {
    it("PUTs an empty project as multipart to /repos/{repo}/projects/{name} and returns the revision", async () => {
      mockHttp.onPut("/repos/design/projects/Offer-CW").reply(200, { revision: "abc123", branch: "main" });

      const result = await client.createEmptyProject("design", "Offer-CW", {
        comment: "Initial commit",
        branch: "feature/rates",
      });

      expect(result).toEqual({ revision: "abc123", branch: "main" });
      const [request] = mockHttp.history.put;
      // fetch writes the multipart type with its boundary; the JSON default of the client must not stay.
      expect(request.headers["Content-Type"]).toBeUndefined();
      expect(Object.fromEntries(request.data as FormData)).toEqual({
        templateType: "predefined",
        templateCategory: "templates",
        templateName: "Empty Project",
        status: "CLOSED",
        branch: "feature/rates",
        comment: "Initial commit",
      });
    });

    it("copies a project on Studio without reading the source project", async () => {
      mockHttp.onPost("/repos/design/projects/Offer-CW/from-project").reply(200, { revision: "copy123", branch: "feature/rates" });

      await expect(client.copyProject(
        "design",
        "Offer-CW",
        "mapped:Offer-US:opaque-id",
        { comment: "Copy rates", branch: "feature/rates" },
      )).resolves.toEqual({ revision: "copy123", branch: "feature/rates" });
      expect(bodyOf(mockHttp.history.post[0])).toEqual({
        sourceProject: "mapped:Offer-US:opaque-id",
        comment: "Copy rates",
        branch: "feature/rates",
      });
      expect(mockHttp.history.get).toHaveLength(0);
    });
  });

  describe("project files", () => {
    it("returns file bytes with their lower-cased content type and forwards version/branch", async () => {
      mockHttp.onGet("/projects/p1/files/rules/Model.xlsx").reply(200, "hello-bytes", {
        "content-type": "Text/Plain; Charset=UTF-8",
        "content-disposition": "attachment; filename=Model.xlsx",
      });

      const res = await client.readProjectFile("p1", "rules/Model.xlsx", { version: "abc123", branch: "main" });

      expect(res.data.toString("utf-8")).toBe("hello-bytes");
      expect(res.contentType).toBe("text/plain; charset=utf-8");
      expect(mockHttp.history.get[0].params).toMatchObject({ version: "abc123", branch: "main" });
    });

    it("percent-encodes path segments but preserves '/' separators", async () => {
      mockHttp.onGet("/projects/p1/files/a%20b/My%20Rules%20%231.xlsx").reply(200, "x");

      await client.readProjectFile("p1", "a b/My Rules #1.xlsx");

      expect(mockHttp.history.get).toHaveLength(1);
    });

    it("rejects '.'/'..' path segments, in a URL or a body, before issuing a request", async () => {
      const refused = /project-relative|not allowed/;

      await expect(client.readProjectFile("p1", "rules/../../etc/passwd")).rejects.toThrow(refused);
      await expect(client.deleteProjectFile("p1", "..")).rejects.toThrow(refused);
      await expect(client.transferProjectFile("copy", "p1", { sourcePath: "a.xlsx", destinationPath: "../b.xlsx" }))
        .rejects.toThrow(refused);
      await expect(client.transferProjectFile("copy", "p1", { sourcePath: "../a.xlsx", destinationPath: "b.xlsx" }))
        .rejects.toThrow(refused);
      await expect(client.transferProjectFile("move", "p1", { sourcePath: "a.xlsx", destinationPath: "sub/../../x.xlsx" }))
        .rejects.toThrow(refused);
      await expect(client.searchProjectFiles("p1", { from: "../secrets" })).rejects.toThrow(refused);
      expectNoRequest();
    });

    it("POSTs octet-stream bytes that create a file, never over one already there", async () => {
      mockHttp.onPost("/projects/p1/files/docs/readme.md").reply(201);

      await client.writeProjectFile("p1", "docs/readme.md", Buffer.from("hello"), {
        createFolders: true,
        branch: "main",
      });

      const [request] = mockHttp.history.post;
      expect(request.headers["Content-Type"]).toContain("octet-stream");
      expect(request.params).toEqual({ createFolders: true, conflictPolicy: "FAIL", branch: "main" });
      expect(Buffer.from(request.data as Uint8Array).toString("utf-8")).toBe("hello");
    });

    it("deletes a file with the branch query param, and without one when no branch is given", async () => {
      mockHttp.onDelete("/projects/p1/files/old.txt").reply(204);

      await client.deleteProjectFile("p1", "old.txt", { branch: "dev" });
      await client.deleteProjectFile("p1", "old.txt");

      expect(mockHttp.history.delete.map(queryOf)).toEqual([{ branch: "dev" }, {}]);
    });

    it("POSTs the FileSearchQuery body and returns the FsNode array", async () => {
      const nodes = [{ path: "rules/M.xlsx", name: "M.xlsx", type: "file" }];
      mockHttp.onPost("/projects/p1/file-search").reply(200, nodes);

      const res = await client.searchProjectFiles(
        "p1",
        { pattern: "**/*.xlsx", content: "premium", type: "FILE" },
        { branch: "main", fields: "path,name" }
      );

      expect(bodyOf(mockHttp.history.post[0])).toEqual({ pattern: "**/*.xlsx", content: "premium", type: "FILE" });
      expect(mockHttp.history.post[0].params).toMatchObject({ branch: "main", fields: "path,name" });
      expect(res).toEqual(nodes);
    });

    it("copies and moves with a {sourcePath,destinationPath} body and the branch param", async () => {
      mockHttp.onPost("/projects/p1/file-copy").reply(201);
      mockHttp.onPost("/projects/p1/file-move").reply(204);

      await client.transferProjectFile("copy", "p1", { sourcePath: "a.xlsx", destinationPath: "b.xlsx" }, { branch: "main" });
      await client.transferProjectFile("move", "p1", { sourcePath: "a.xlsx", destinationPath: "sub/a.xlsx" });

      const [copy, move] = mockHttp.history.post;
      expect(copy).toMatchObject({ url: "/projects/p1/file-copy", params: { branch: "main" } });
      expect(bodyOf(copy)).toEqual({ sourcePath: "a.xlsx", destinationPath: "b.xlsx" });
      expect(move.url).toBe("/projects/p1/file-move");
      expect(bodyOf(move)).toEqual({ sourcePath: "a.xlsx", destinationPath: "sub/a.xlsx" });
    });

    it("normalizes body paths (strips leading slash) but does NOT percent-encode them", async () => {
      mockHttp.onPost("/projects/p1/file-copy").reply(201);
      mockHttp.onPost("/projects/p1/file-search").reply(200, []);

      // Leading slash is stripped; the space is preserved (NOT %20) — body paths are raw JSON.
      await client.transferProjectFile("copy", "p1", { sourcePath: "/rules/My File.xlsx", destinationPath: "/out/Copy File.xlsx" });
      await client.searchProjectFiles("p1", { from: "/rules" });

      expect(mockHttp.history.post.map(bodyOf)).toEqual([
        { sourcePath: "rules/My File.xlsx", destinationPath: "out/Copy File.xlsx" },
        { from: "rules" },
      ]);
    });

    describe("getProjectAgentContext", () => {
      const ancestorsNodes = [
        { path: "foo/Project-1/AGENTS.md", name: "AGENTS.md", type: "file", basePath: "foo/Project-1", size: 7, lastModified: "2026-06-12T10:47:05Z", content: "project" },
        { path: "foo/AGENTS.md", name: "AGENTS.md", type: "file", basePath: "foo", size: 0, lastModified: "2026-06-12T10:47:06Z" },
      ];

      it("issues a fixed ANCESTORS search from AGENTS.md and returns path+content nearest-first", async () => {
        mockHttp.onPost("/projects/p1/file-search").reply(200, ancestorsNodes);

        const res = await client.getProjectAgentContext("p1");

        // Direction + start path are fixed (not caller-controllable).
        expect(bodyOf(mockHttp.history.post[0])).toEqual({ scope: "ANCESTORS", from: "AGENTS.md" });
        // Nearest-first, carrying only path/content; a missing content is empty.
        expect(res).toEqual([
          { path: "foo/Project-1/AGENTS.md", content: "project" },
          { path: "foo/AGENTS.md", content: "" },
        ]);
        // No enrichment round-trip through getProject.
        expect(mockHttp.history.get).toHaveLength(0);
      });

      it("starts the walk from a sub-folder and on the branch given", async () => {
        mockHttp.onPost("/projects/p1/file-search").reply(200, ancestorsNodes);

        await client.getProjectAgentContext("p1", { folder: "/rules/pricing/", branch: "release" });

        // Leading/trailing slashes trimmed; '/AGENTS.md' appended.
        expect(bodyOf(mockHttp.history.post[0])).toEqual({ scope: "ANCESTORS", from: "rules/pricing/AGENTS.md" });
        expect(mockHttp.history.post[0].params).toMatchObject({ branch: "release" });
      });
    });
  });

  describe("trace debug API", () => {
    const tracePath = "/projects/p1/trace";

    it("POSTs a start with tableId in the query, the inputJson serialized as JSON body, and returns the initial stack", async () => {
      mockHttp.onPost(tracePath).reply(200, suspendedStack);

      const stack = await client.startTrace({ projectId: "p1", tableId: "calc_42", inputJson: { params: { age: 30 } } });

      const [request] = mockHttp.history.post;
      expect(queryOf(request)).toEqual({ tableId: "calc_42" });
      expect(bodyOf(request)).toEqual({ params: { age: 30 } });
      expect(request.headers["Content-Type"]).toBe("application/json");
      expect(stack).toEqual(suspendedStack);
    });

    it("threads the trace controls into the query string and sends no body when inputJson is omitted", async () => {
      mockHttp.onPost(tracePath).reply(200, { status: "completed", frames: [] });

      await client.startTrace({
        projectId: "p1",
        tableId: "MyTest",
        testRanges: "1-3,5",
        stopAtEntry: false,
        profiling: true,
        detailedTitles: true,
        breakOnErrors: false,
        includeTree: false,
        profileTop: 30,
      });

      const [request] = mockHttp.history.post;
      // The query encodes the comma in "1-3,5".
      expect(buildUrl(request)).toContain("testRanges=1-3%2C5");
      expect(queryOf(request)).toEqual({
        tableId: "MyTest",
        testRanges: "1-3,5",
        stopAtEntry: "false",
        profiling: "true",
        detailedTitles: "true",
        breakOnErrors: "false",
        includeTree: "false",
        profileTop: "30",
      });
      expect(request.data).toBeUndefined();
    });

    it("GETs the compact stack, forwarding includeTree/profileTop", async () => {
      mockHttp.onGet(`${tracePath}/stack`).reply(200, suspendedStack);

      await expect(client.getTraceStack("p1", { includeTree: false, profileTop: 10 })).resolves.toEqual(suspendedStack);
      expect(queryOf(mockHttp.history.get[0])).toEqual({ view: "compact", includeTree: "false", profileTop: "10" });
    });

    it("GETs the children of a tree node, forwarding uri/instance/step/offset/limit as query params", async () => {
      const page: Types.TreeChildrenView = {
        children: [{
          uri: "P/Rules.xlsx?sheet=Rules&range=A1:B4",
          name: "AgeFactor",
          kind: "decisionTable",
          instance: 0,
          durationMillis: 2,
          selfMillis: 2,
          steps: [{ ref: "R1C0", label: "$Factor", status: "executed", childrenTotal: 0 }],
          notRetained: 5,
        }],
        total: 4,
      };
      mockHttp.onGet(`${tracePath}/tree/children`).reply(200, page);

      const result = await client.getTraceTreeChildren("p1", {
        uri: "P/Rules.xlsx?sheet=Main&range=B2:D8",
        instance: 3,
        step: "R1C0",
        offset: 100,
        limit: 50,
      });
      await client.getTraceTreeChildren("p1", { uri: "P/R.xlsx", instance: 0, step: "R1C0" });

      // Every value is a query parameter, so the URI's own ?sheet=…&range=… is percent-encoded, not merged into
      // the request path; offset and limit are left to the backend defaults when not given.
      expect(mockHttp.history.get.map(queryOf)).toEqual([
        { uri: "P/Rules.xlsx?sheet=Main&range=B2:D8", instance: "3", step: "R1C0", offset: "100", limit: "50" },
        { uri: "P/R.xlsx", instance: "0", step: "R1C0" },
      ]);
      expect(result).toEqual(page);
    });

    it("asks for the schemas of the watched points when no projection is given", async () => {
      mockHttp.onGet(`${tracePath}/watch`).reply(200, { series: [] });

      await client.getTraceWatch("p1");

      expect(queryOf(mockHttp.history.get[0])).toEqual({ includeSchema: "true" });
    });
  });
});
