/**
 * Unit tests for client.ts
 * Tests OpenL API client methods with mocked HTTP responses
 */

import { describe, it, expect, beforeEach, afterEach } from "@jest/globals";
import MockAdapter from "axios-mock-adapter";
import { OpenLClient } from "../src/client.js";
import { isResultNotReady } from "../src/utils.js";
import type { OpenLConfig, ProjectViewModel, SummaryTableView } from "../src/types.js";
import type * as Types from "../src/types.js";

function assertResultReady<T>(
  result: T | Types.ResultNotReadyView,
): asserts result is T {
  if (isResultNotReady(result)) {
    throw new Error("Expected completed test results, received notReady");
  }
}

describe("OpenLClient", () => {
  let client: OpenLClient;
  let mockAxios: MockAdapter;

  beforeEach(() => {
    const config: OpenLConfig = {
      baseUrl: "http://localhost:8080",
      personalAccessToken: "openl_pat_test",
    };
    client = new OpenLClient(config);
    // @ts-ignore - Access private axiosInstance for mocking
    mockAxios = new MockAdapter(client.axiosInstance);
  });

  afterEach(() => {
    mockAxios.reset();
    mockAxios.restore();
  });

  describe("Constructor and Configuration", () => {
    it("should keep /rest when already present in baseUrl", () => {
      const config: OpenLConfig = {
        baseUrl: "http://localhost:8080/rest",
      };
      const testClient = new OpenLClient(config);
      expect(testClient.getBaseUrl()).toMatch(/\/rest$/);
    });

    it("should report the Personal Access Token auth method when a PAT is configured", () => {
      const config: OpenLConfig = {
        baseUrl: "http://localhost:8080",
        personalAccessToken: "openl_pat_test",
      };
      const testClient = new OpenLClient(config);
      expect(testClient.getAuthMethod()).toContain("Personal Access Token");
    });

    it("should auto-append /rest when missing in baseUrl", () => {
      const config: OpenLConfig = {
        baseUrl: "http://localhost:8080",
      };
      const testClient = new OpenLClient(config);
      expect(testClient.getBaseUrl()).toMatch(/\/rest$/);
    });
  });

  describe("Repository Management", () => {
    describe("listRepositories", () => {
      it("should fetch list of repositories", async () => {
        const mockRepos: Repository[] = [
          { id: "design", name: "Design Repository" },
          { id: "production", name: "Production Repository" },
        ];

        mockAxios.onGet("/repos").reply(200, mockRepos);

        const result = await client.listRepositories();
        expect(result).toEqual(mockRepos);
        expect(result.length).toBe(2);
      });

      it("should handle empty repository list", async () => {
        mockAxios.onGet("/repos").reply(200, []);

        const result = await client.listRepositories();
        expect(result).toEqual([]);
      });

      it("should handle network errors", async () => {
        mockAxios.onGet("/repos").networkError();

        await expect(client.listRepositories()).rejects.toThrow();
      });
    });

    describe("listBranches", () => {
      it("should fetch branches for a repository", async () => {
        const mockBranches = ["main", "development", "feature/new-rules"];

        mockAxios.onGet("/repos/design/branches").reply(200, mockBranches);

        const result = await client.listBranches("design");
        expect(result).toEqual(mockBranches);
        expect(result.length).toBe(3);
      });

      it("should URL-encode repository name", async () => {
        mockAxios.onGet(/\/repos\/.*\/branches/).reply((config) => {
          expect(config.url).toContain("my%20repo");
          return [200, ["main"]];
        });

        await client.listBranches("my repo");
      });

      it("should handle repository with no branches", async () => {
        mockAxios.onGet("/repos/empty/branches").reply(200, []);

        const result = await client.listBranches("empty");
        expect(result).toEqual([]);
      });
    });

    describe("getRepositoryIdByName (id-or-name, case-insensitive)", () => {
      const repos: Repository[] = [
        { id: "design", name: "Design Repository" },
        { id: "production", name: "Production Repository" },
      ];

      beforeEach(() => {
        mockAxios.onGet("/repos").reply(200, repos);
      });

      it("resolves exact id", async () => {
        expect(await client.getRepositoryIdByName("design")).toBe("design");
      });

      it("resolves exact display name", async () => {
        expect(await client.getRepositoryIdByName("Design Repository")).toBe("design");
      });

      it("resolves case-insensitive id", async () => {
        expect(await client.getRepositoryIdByName("DESIGN")).toBe("design");
      });

      it("resolves case-insensitive display name", async () => {
        expect(await client.getRepositoryIdByName("design repository")).toBe("design");
      });

      it("throws with a helpful message listing id + name pairs when no match", async () => {
        await expect(client.getRepositoryIdByName("ghost")).rejects.toThrow(
          /Repository "ghost" not found.*design \(Design Repository\), production \(Production Repository\)/,
        );
      });
    });
  });

  describe("Project Management", () => {
    describe("listProjects", () => {
      it("should fetch list of projects", async () => {
        const mockProjects: Partial<ProjectViewModel>[] = [
          {
            id: "design:Project 1:hash1",
            name: "Project 1",
            repository: "design",
            status: "OPENED",
            path: "Project 1",
            modifiedBy: "admin",
            modifiedAt: "2024-01-01T00:00:00Z",
          },
          {
            id: "design:Project 2:hash2",
            name: "Project 2",
            repository: "design",
            status: "CLOSED",
            path: "Project 2",
            modifiedBy: "admin",
            modifiedAt: "2024-01-01T00:00:00Z",
          },
        ];

        mockAxios.onGet("/projects", { params: { repository: "design" } }).reply(200, mockProjects);

        const result = await client.listProjects({ repository: "design" });
        expect(result.length).toBe(2);
        expect(result[0].name).toBe("Project 1");
      });

      it("should preserve project page metadata for paginated consumers", async () => {
        const content = Array.from({ length: 50 }, (_, i) => ({
          id: `design:p${i + 50}:hash${i + 50}`,
          name: `p${i + 50}`,
          repository: "design",
          status: "CLOSED" as const,
          path: `p${i + 50}`,
          modifiedBy: "admin",
          modifiedAt: "2024-01-01T00:00:00Z",
        }));
        mockAxios.onGet("/projects", { params: { offset: 50, size: 50 } }).reply(200, {
          content,
          pageNumber: 1,
          pageSize: 50,
          numberOfElements: 50,
          total: 120,
          projectIndexHealth: { healthy: true },
          repositoryCounts: [{ repository: "design", count: 120 }],
          statusCounts: { CLOSED: 120 },
          statuses: ["CLOSED"],
          tagCounts: [{ tag: "release", count: 3 }],
        });

        const result = await client.listProjectsPage({ offset: 50, limit: 50 });

        expect(result).toMatchObject({
          serverPaginated: true,
          pageNumber: 1,
          pageSize: 50,
          total: 120,
          metadata: {
            projectIndexHealth: { healthy: true },
            repositoryCounts: [{ repository: "design", count: 120 }],
            statusCounts: { CLOSED: 120 },
            statuses: ["CLOSED"],
            tagCounts: [{ tag: "release", count: 3 }],
          },
        });
        expect(result.items).toHaveLength(50);
        expect(result.items[0].name).toBe("p50");
      });

      it("should filter by status", async () => {
        const openProjects: Partial<ProjectViewModel>[] = [
          {
            id: "design:p1:hash1",
            name: "p1",
            repository: "design",
            status: "OPENED",
            path: "p1",
            modifiedBy: "admin",
            modifiedAt: "2024-01-01T00:00:00Z",
          },
        ];

        mockAxios.onGet("/projects", { params: { repository: "design", status: "OPENED" } }).reply(200, openProjects);

        const result = await client.listProjects({
          repository: "design",
          status: "OPENED"
        });
        expect(result.length).toBe(1);
      });

      it("should filter by tag", async () => {
        // Tags are sent with "tags." prefix in the API
        mockAxios.onGet("/projects", { params: { repository: "design", "tags.tag": "v1.0" } }).reply(200, []);

        await client.listProjects({
          repository: "design",
          tags: { tag: "v1.0" }
        });

        expect(mockAxios.history.get.length).toBe(1);
      });

      it("forwards the current project filters and exact item offset", async () => {
        const params = {
          dependsOn: "design:base",
          name: "rate",
          author: "admin",
          branch: "feature",
          sort: "updated" as const,
          include: ["status", "descriptor"] as const,
          offset: 25,
          size: 50,
        };
        mockAxios.onGet("/projects").reply((config) => {
          expect(config.params).toEqual(params);
          expect(config.paramsSerializer).toEqual({ indexes: null });
          // @ts-expect-error Test-only access verifies the final wire representation.
          expect(client.axiosInstance.getUri(config)).toContain("include=status&include=descriptor");
          return [200, { content: [], pageNumber: 0, pageSize: 50, numberOfElements: 0 }];
        });

        await client.listProjectsPage({
          ...params,
          include: [...params.include],
          limit: params.size,
          size: undefined,
        });
      });
    });

    describe("getProject", () => {
      it("should fetch project by ID", async () => {
        // projectId "design-project1" is used as path identifier
        const projectIdForPath = "design-project1";
        const encodedProjectId = encodeURIComponent(projectIdForPath);

        const mockProject: Partial<ProjectViewModel> = {
          id: "design:project1:hash123",
          name: "project1",
          repository: "design",
          status: "OPENED",
          path: "project1",
          modifiedBy: "admin",
          modifiedAt: "2024-01-01T00:00:00Z",
        };

        mockAxios.onGet(`/projects/${encodedProjectId}`).reply(200, mockProject);

        const result = await client.getProject("design-project1");
        expect(result.name).toBe("project1");
      });

      it("forwards current include expansions", async () => {
        mockAxios.onGet("/projects/design-project1").reply((config) => {
          expect(config.params).toEqual({ include: ["status", "descriptor"] });
          expect(config.paramsSerializer).toEqual({ indexes: null });
          // @ts-expect-error Test-only access verifies the final wire representation.
          expect(client.axiosInstance.getUri(config)).toContain("include=status&include=descriptor");
          return [200, { id: "design-project1", name: "project1" }];
        });

        await client.getProject("design-project1", ["status", "descriptor"]);
        expect(mockAxios.history.get).toHaveLength(1);
      });

      it("should parse projectId with hyphen separator", async () => {
        // projectId "design-InsuranceRules" is used as path identifier
        const projectIdForPath = "design-InsuranceRules";
        const encodedProjectId = encodeURIComponent(projectIdForPath);

        mockAxios.onGet(`/projects/${encodedProjectId}`).reply(200, {
          id: "design:InsuranceRules:hash123",
          name: "InsuranceRules",
          repository: "design",
          status: "OPENED",
          path: "InsuranceRules",
          modifiedBy: "admin",
          modifiedAt: "2024-01-01T00:00:00Z",
        });

        await client.getProject("design-InsuranceRules");
        expect(mockAxios.history.get[0].url).toContain(encodedProjectId);
      });

      it("should handle project not found", async () => {
        const projectIdForPath = "design-nonexistent";
        const encodedProjectId = encodeURIComponent(projectIdForPath);

        mockAxios.onGet(`/projects/${encodedProjectId}`).reply(404);

        await expect(client.getProject("design-nonexistent")).rejects.toThrow();
      });

      it("should pass opaque projectId through to API", async () => {
        const encodedProjectId = encodeURIComponent("invalid");
        mockAxios.onGet(`/projects/${encodedProjectId}`).reply(404);

        await expect(client.getProject("invalid")).rejects.toThrow();
      });
    });

    describe("getSessionCookie", () => {
      it("returns null before any HTTP call has captured a Set-Cookie", () => {
        expect(client.getSessionCookie()).toBeNull();
      });

      it("returns the JSESSIONID value extracted from a response Set-Cookie header", async () => {
        // The cookie interceptor parses single-string or array forms of set-cookie.
        mockAxios.onGet("/repos").reply(
          200,
          [],
          { "set-cookie": "JSESSIONID=abc123def; Path=/; HttpOnly" },
        );
        await client.listRepositories();
        expect(client.getSessionCookie()).toBe("abc123def");
      });

      it("survives subsequent responses that don't include a new cookie", async () => {
        mockAxios.onGet("/repos").reply(
          200,
          [],
          { "set-cookie": "JSESSIONID=stay-put; Path=/" },
        );
        await client.listRepositories();
        // A second call without set-cookie should leave the stored value alone.
        mockAxios.onGet("/repos").reply(200, []);
        await client.listRepositories(false);
        expect(client.getSessionCookie()).toBe("stay-put");
      });
    });

    describe("getProjectStatus", () => {
      const projectId = "design-AutoInsurance";
      const encodedProjectId = encodeURIComponent(projectId);

      const okFixture: Types.ProjectStatusView = {
        projectId: { repository: "design", projectName: "AutoInsurance" },
        branch: "main",
        revision: "abc123",
        compileState: "ok",
        compilation: {
          messages: { items: [], total: 0, errors: 0, warnings: 0 },
          modules: { total: 1, compiled: 1, compiledModules: ["Main"] },
          tests: { total: 5 },
        },
      };

      const errorsFixture: Types.ProjectStatusView = {
        projectId: { repository: "design", projectName: "AutoInsurance" },
        branch: "main",
        compileState: "errors",
        compilation: {
          messages: {
            items: [
              { id: 1, summary: "Datatype 'Driver' not found", severity: "ERROR" },
              { id: 2, summary: "Unused field 'tmp'", severity: "WARN" },
            ],
            total: 2,
            errors: 1,
            warnings: 1,
          },
          modules: { total: 1, compiled: 0 },
          tests: { total: 0 },
        },
      };

      it("should fetch project status without branch parameter", async () => {
        mockAxios.onGet(`/projects/${encodedProjectId}/status`).reply(200, okFixture);

        const result = await client.getProjectStatus(projectId);
        expect(result.compileState).toBe("ok");
        expect(result.compilation?.modules.compiled).toBe(1);
        // No query string sent when branch omitted
        expect(mockAxios.history.get[0].params).toEqual({});
      });

      it("should pass branch as query parameter when provided", async () => {
        mockAxios.onGet(`/projects/${encodedProjectId}/status`).reply((config) => {
          if (config.params?.branch === "main") {
            return [200, errorsFixture];
          }
          return [400, { message: "expected branch=main" }];
        });

        const result = await client.getProjectStatus(projectId, "main");
        expect(result.compileState).toBe("errors");
        expect(result.compilation?.messages.items).toHaveLength(2);
        expect(mockAxios.history.get[0].params).toEqual({ branch: "main" });
      });

      it("should surface 409 when branch does not match the opened branch", async () => {
        mockAxios.onGet(`/projects/${encodedProjectId}/status`).reply(409, {
          message: "project.branch.mismatch.message",
        });

        await expect(client.getProjectStatus(projectId, "develop")).rejects.toThrow();
      });

      it("should trigger lazy compilation by listing a single table", async () => {
        const controller = new AbortController();
        mockAxios.onGet(`/projects/${encodedProjectId}/tables`).reply(200, {
          content: [],
          pageNumber: 0,
          pageSize: 1,
          numberOfElements: 0,
          total: 0,
        });

        await client.triggerProjectCompilation(projectId, {
          signal: controller.signal,
          timeoutMs: 120_000,
        });

        expect(mockAxios.history.get[0].params).toEqual({ offset: 0, size: 1 });
        expect(mockAxios.history.get[0].signal).toBe(controller.signal);
        expect(mockAxios.history.get[0].timeout).toBe(120_000);
      });
    });

    describe("updateProjectStatus", () => {
      it("should update project status", async () => {
        // updateProjectStatus first fetches the project, then updates it
        const projectIdForPath = "design-project1";
        const encodedProjectId = encodeURIComponent(projectIdForPath);

        mockAxios.onGet(`/projects/${encodedProjectId}`).reply(200, {
          id: "design:project1:hash123",
          name: "project1",
          repository: "design",
          status: "OPENED",
          path: "project1",
          modifiedBy: "admin",
          modifiedAt: "2024-01-01T00:00:00Z",
        });

        // updateProjectStatus uses PATCH, not PUT
        mockAxios.onPatch(`/projects/${encodedProjectId}`).reply(200, {
          success: true,
          message: "Project status updated successfully",
        });

        const result = await client.updateProjectStatus("design-project1", {
          status: "CLOSED",
        });
        expect(result.success).toBe(true);
      });

      it("should send comment when provided", async () => {
        const projectIdForPath = "design-project1";
        const encodedProjectId = encodeURIComponent(projectIdForPath);

        mockAxios.onGet(`/projects/${encodedProjectId}`).reply(200, {
          id: "design:project1:hash123",
          name: "project1",
          repository: "design",
          status: "OPENED",
          path: "project1",
          modifiedBy: "admin",
          modifiedAt: "2024-01-01T00:00:00Z",
        });

        mockAxios.onPatch(`/projects/${encodedProjectId}`).reply((config) => {
          const data = JSON.parse(config.data);
          expect(data.comment).toBe("Closing project");
          return [200, { success: true }];
        });

        await client.updateProjectStatus("design-project1", {
          status: "CLOSED",
          comment: "Closing project",
        });
      });

      it("should send discardChanges flag", async () => {
        const projectIdForPath = "design-project1";
        const encodedProjectId = encodeURIComponent(projectIdForPath);

        mockAxios.onGet(`/projects/${encodedProjectId}`).reply(200, {
          id: "design:project1:hash123",
          name: "project1",
          repository: "design",
          status: "EDITING", // Project has unsaved changes
          path: "project1",
          modifiedBy: "admin",
          modifiedAt: "2024-01-01T00:00:00Z",
        });

        // discardChanges is handled client-side, not sent to API
        mockAxios.onPatch(`/projects/${encodedProjectId}`).reply(200, {
          success: true,
          message: "Project closed (changes discarded)",
        });

        await client.updateProjectStatus("design-project1", {
          status: "CLOSED",
          discardChanges: true,
        });
      });

      it("should switch branches", async () => {
        const projectIdForPath = "design-project1";
        const encodedProjectId = encodeURIComponent(projectIdForPath);

        mockAxios.onGet(`/projects/${encodedProjectId}`).reply(200, {
          id: "design:project1:hash123",
          name: "project1",
          repository: "design",
          status: "OPENED",
          path: "project1",
          modifiedBy: "admin",
          modifiedAt: "2024-01-01T00:00:00Z",
        });

        mockAxios.onPatch(`/projects/${encodedProjectId}`).reply((config) => {
          const data = JSON.parse(config.data);
          expect(data.branch).toBe("development");
          return [200, { success: true }];
        });

        await client.updateProjectStatus("design-project1", {
          branch: "development",
        });
      });

      it("should throw when project is in local repository", async () => {
        const projectIdForPath = "local-myproject";
        const encodedProjectId = encodeURIComponent(projectIdForPath);

        mockAxios.onGet(`/projects/${encodedProjectId}`).reply(200, {
          id: "local:myproject:hash",
          name: "myproject",
          repository: "local",
          status: "CLOSED",
          path: "myproject",
          modifiedBy: "admin",
          modifiedAt: "2024-01-01T00:00:00Z",
        });

        await expect(
          client.updateProjectStatus("local-myproject", { status: "OPENED" })
        ).rejects.toThrow(/local repository.*not connected to a remote Git/i);
      });
    });

    describe("openProject", () => {
      const projectIdForPath = "design-project1";
      const encodedProjectId = encodeURIComponent(projectIdForPath);

      const mockDesignProject = (status: string) => ({
        id: "design:project1:hash123",
        name: "project1",
        repository: "design",
        status,
        path: "project1",
        modifiedBy: "admin",
        modifiedAt: "2024-01-01T00:00:00Z",
      });

      it("should open a project with status OPENED", async () => {
        mockAxios.onGet(`/projects/${encodedProjectId}`).reply(200, mockDesignProject("CLOSED"));

        mockAxios.onPatch(`/projects/${encodedProjectId}`).reply((config) => {
          const data = JSON.parse(config.data);
          expect(data.status).toBe("OPENED");
          return [204];
        });

        const result = await client.openProject("design-project1");
        expect(result).toBe(true);
        expect(mockAxios.history.patch.length).toBe(1);
      });

      it("should open a project on a specific branch", async () => {
        mockAxios.onGet(`/projects/${encodedProjectId}`).reply(200, mockDesignProject("CLOSED"));

        mockAxios.onPatch(`/projects/${encodedProjectId}`).reply((config) => {
          const data = JSON.parse(config.data);
          expect(data.status).toBe("OPENED");
          expect(data.branch).toBe("development");
          return [204];
        });

        const result = await client.openProject("design-project1", { branch: "development" });
        expect(result).toBe(true);
      });

      it("should always send status OPENED regardless of current project status", async () => {
        // Even if the project is already OPENED, openProject always sends status
        mockAxios.onGet(`/projects/${encodedProjectId}`).reply(200, mockDesignProject("OPENED"));

        mockAxios.onPatch(`/projects/${encodedProjectId}`).reply((config) => {
          const data = JSON.parse(config.data);
          expect(data.status).toBe("OPENED");
          return [204];
        });

        const result = await client.openProject("design-project1");
        expect(result).toBe(true);
      });

      it("should open a project at a specific revision", async () => {
        mockAxios.onGet(`/projects/${encodedProjectId}`).reply(200, mockDesignProject("CLOSED"));

        mockAxios.onPatch(`/projects/${encodedProjectId}`).reply((config) => {
          const data = JSON.parse(config.data);
          expect(data.status).toBe("OPENED");
          expect(data.revision).toBe("abc123");
          return [204];
        });

        const result = await client.openProject("design-project1", { revision: "abc123" });
        expect(result).toBe(true);
      });

      it("should throw when project is in local repository", async () => {
        const localBase64Id = "local-myproject";
        const encodedLocalId = encodeURIComponent(localBase64Id);

        mockAxios.onGet(`/projects/${encodedLocalId}`).reply(200, {
          id: "local:myproject:hash",
          name: "myproject",
          repository: "local",
          status: "CLOSED",
          path: "myproject",
          modifiedBy: "admin",
          modifiedAt: "2024-01-01T00:00:00Z",
        });

        await expect(
          client.openProject("local-myproject")
        ).rejects.toThrow(/local repository.*not connected to a remote Git/i);
      });
    });

    describe("switchBranch", () => {
      const projectIdForPath = "design-project1";
      const encodedProjectId = encodeURIComponent(projectIdForPath);

      const mockDesignProject = (status: string) => ({
        id: "design:project1:hash123",
        name: "project1",
        repository: "design",
        status,
        path: "project1",
        modifiedBy: "admin",
        modifiedAt: "2024-01-01T00:00:00Z",
      });

      it("should send PATCH with only branch (no status)", async () => {
        mockAxios.onGet(`/projects/${encodedProjectId}`).reply(200, mockDesignProject("OPENED"));

        mockAxios.onPatch(`/projects/${encodedProjectId}`).reply((config) => {
          const data = JSON.parse(config.data);
          expect(data.status).toBeUndefined();
          expect(data.branch).toBe("feature/new-rules");
          return [204];
        });

        const result = await client.switchBranch("design-project1", "feature/new-rules");
        expect(result).toBe(true);
        expect(mockAxios.history.patch.length).toBe(1);
      });

      it("should throw when project is in local repository", async () => {
        const localBase64Id = "local-myproject";
        const encodedLocalId = encodeURIComponent(localBase64Id);

        mockAxios.onGet(`/projects/${encodedLocalId}`).reply(200, {
          id: "local:myproject:hash",
          name: "myproject",
          repository: "local",
          status: "OPENED",
          path: "myproject",
          modifiedBy: "admin",
          modifiedAt: "2024-01-01T00:00:00Z",
        });

        await expect(
          client.switchBranch("local-myproject", "main")
        ).rejects.toThrow(/local repository.*not connected to a remote Git/i);
      });
    });
  });

  describe("Table Management", () => {
    describe("listTables", () => {
      it("should fetch list of tables", async () => {
        const projectIdForPath = "design-project1";
        const encodedProjectId = encodeURIComponent(projectIdForPath);

        const mockTables: Partial<SummaryTableView>[] = [
          {
            id: "calculatePremium_1234",
            name: "calculatePremium",
            tableType: "SimpleRules",
            kind: "Rules",
            file: "Rules.xlsx",
            pos: "A1",
          },
          {
            id: "validatePolicy_5678",
            name: "validatePolicy",
            tableType: "Spreadsheet",
            kind: "Spreadsheet",
            file: "Rules.xlsx",
            pos: "A1",
          },
        ];

        mockAxios.onGet(`/projects/${encodedProjectId}/tables`).reply(200, mockTables);

        const result = await client.listTables("design-project1");
        expect(result.length).toBe(2);
        expect(result[0].name).toBe("calculatePremium");
      });

      it("should filter by table type", async () => {
        const projectIdForPath = "design-project1";
        const encodedProjectId = encodeURIComponent(projectIdForPath);

        mockAxios.onGet(`/projects/${encodedProjectId}/tables`).reply((config) => {
          expect(config.params).toEqual({ kind: ["Rules"] });
          expect(config.paramsSerializer).toEqual({ indexes: null });
          return [200, []];
        });

        await client.listTables("design-project1", { kind: ["Rules"] });
        expect(mockAxios.history.get.length).toBe(1);
      });

      it("should filter by name pattern", async () => {
        const projectIdForPath = "design-project1";
        const encodedProjectId = encodeURIComponent(projectIdForPath);

        mockAxios.onGet(`/projects/${encodedProjectId}/tables`, {
          params: { name: "calculate" }
        }).reply(200, []);

        await client.listTables("design-project1", { name: "calculate" });
        expect(mockAxios.history.get.length).toBe(1);
      });
    });

    describe("getTable", () => {
      it("should fetch table by ID", async () => {
        const projectIdForPath = "design-project1";
        const encodedProjectId = encodeURIComponent(projectIdForPath);
        const encodedTableId = encodeURIComponent("calculatePremium_1234");

        const mockTable: Partial<SummaryTableView> = {
          id: "calculatePremium_1234",
          name: "calculatePremium",
          tableType: "SimpleRules",
          kind: "Rules",
          file: "Rules.xlsx",
          pos: "A1",
        };

        mockAxios.onGet(`/projects/${encodedProjectId}/tables/${encodedTableId}`).reply(200, mockTable);

        const result = await client.getTable("design-project1", "calculatePremium_1234");
        expect(result.id).toBe("calculatePremium_1234");
      });

      it("should URL-encode table ID", async () => {
        const projectIdForPath = "design-project1";
        const encodedProjectId = encodeURIComponent(projectIdForPath);

        mockAxios.onGet(new RegExp(`/projects/${encodedProjectId.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}/tables/.*`)).reply((config) => {
          expect(config.url).toContain("table%20id");
          return [200, {}];
        });

        await client.getTable("design-project1", "table id");
      });

      it("should handle table not found", async () => {
        const projectIdForPath = "design-project1";
        const encodedProjectId = encodeURIComponent(projectIdForPath);
        const encodedTableId = encodeURIComponent("nonexistent");

        mockAxios.onGet(`/projects/${encodedProjectId}/tables/${encodedTableId}`).reply(404);

        await expect(
          client.getTable("design-project1", "nonexistent")
        ).rejects.toThrow();
      });

      it("should always request the authoritative raw source", async () => {
        const projectIdForPath = "design-project1";
        const encodedProjectId = encodeURIComponent(projectIdForPath);
        const encodedTableId = encodeURIComponent("calculatePremium_1234");

        mockAxios.onGet(`/projects/${encodedProjectId}/tables/${encodedTableId}`).reply((config) => {
          expect(config.params).toEqual({ raw: true });
          return [200, { id: "calculatePremium_1234" }];
        });

        await client.getTable("design-project1", "calculatePremium_1234");
      });

      it("should pass startRow/maxRows/styles query params for a raw slice with styles", async () => {
        const projectIdForPath = "design-project1";
        const encodedProjectId = encodeURIComponent(projectIdForPath);
        const encodedTableId = encodeURIComponent("calculatePremium_1234");

        mockAxios.onGet(`/projects/${encodedProjectId}/tables/${encodedTableId}`).reply((config) => {
          expect(config.params).toEqual({ raw: true, startRow: 10, maxRows: 50, styles: true });
          return [200, { id: "calculatePremium_1234", source: [], totalRows: 200 }];
        });

        const result = await client.getTable("design-project1", "calculatePremium_1234", {
          startRow: 10,
          maxRows: 50,
          styles: true,
        });
        expect(result.totalRows).toBe(200);
      });

      it("should omit unset raw-view options and a false styles flag from the query", async () => {
        const projectIdForPath = "design-project1";
        const encodedProjectId = encodeURIComponent(projectIdForPath);
        const encodedTableId = encodeURIComponent("calculatePremium_1234");

        mockAxios.onGet(`/projects/${encodedProjectId}/tables/${encodedTableId}`).reply((config) => {
          expect(config.params).toEqual({ raw: true, maxRows: 25 });
          return [200, { id: "calculatePremium_1234", source: [] }];
        });

        await client.getTable("design-project1", "calculatePremium_1234", { maxRows: 25, styles: false });
      });
    });

    describe("updateTable", () => {
      it("should update table with new data", async () => {
        const projectIdForPath = "design-project1";
        const encodedProjectId = encodeURIComponent(projectIdForPath);
        const encodedTableId = encodeURIComponent("calculatePremium_1234");

        const tableView = {
          id: "calculatePremium_1234",
          name: "calculatePremium",
          tableType: "RawSource" as const,
          kind: "Rules" as const,
          source: [[{ value: "Rules void calculatePremium()" }]],
        };

        // updateTable returns void (204 No Content)
        mockAxios.onPut(`/projects/${encodedProjectId}/tables/${encodedTableId}`).reply(204);

        await client.updateTable("design-project1", "calculatePremium_1234", tableView);
        expect(mockAxios.history.put.length).toBe(1);
      });
    });
  });

  describe("File Operations", () => {

    describe("downloadFile", () => {
      it("should download file with full path from list_tables", async () => {
        const fileContent = Buffer.from("test file content");
        // File path includes project name directory (as returned by list_tables)
        // Forward slashes are NOT encoded in the path
        mockAxios.onGet("/projects/design-project1/files/project1/Rules.xlsx").reply(200, fileContent);

        const result = await client.downloadFile("design-project1", "project1/Rules.xlsx");
        expect(result).toEqual(fileContent);
      });

      it("should use provided filename as-is when given just filename", async () => {
        const fileContent = Buffer.from("test file content");
        // Current client contract uses provided path as-is for plain filenames.
        mockAxios.onGet("/projects/design-project1/files/Corporate%20Rating.xlsx").reply(200, fileContent);

        const result = await client.downloadFile("design-project1", "Corporate Rating.xlsx");
        expect(result).toEqual(fileContent);
      });

      it("should handle file paths with spaces correctly", async () => {
        const fileContent = Buffer.from("test file content");
        // Path segments are encoded separately, slashes preserved
        mockAxios.onGet("/projects/design-project1/files/project1/My%20Rules.xlsx").reply(200, fileContent);

        const result = await client.downloadFile("design-project1", "project1/My Rules.xlsx");
        expect(result).toEqual(fileContent);
      });

      it("should download specific version", async () => {
        // Mock the file download with version parameter
        mockAxios.onGet("/projects/design-project1/files/project1/Rules.xlsx", { params: { version: "abc123" } })
          .reply(200, Buffer.from("old content"));

        const result = await client.downloadFile("design-project1", "project1/Rules.xlsx", "abc123");
        expect(result.toString()).toBe("old content");
      });

      it("should handle file not found with helpful error", async () => {
        // Both paths will return 404
        mockAxios.onGet("/projects/design-project1/files/NonExistent.xlsx").reply(404);
        mockAxios.onGet("/projects/design-project1/files/project1/NonExistent.xlsx").reply(404);

        await expect(
          client.downloadFile("design-project1", "NonExistent.xlsx")
        ).rejects.toThrow(/File not found.*Tried paths/);
      });

      it("should handle 400 error with helpful message", async () => {
        mockAxios.onGet("/projects/design-project1/files/Bad.xlsx").reply(400);

        await expect(
          client.downloadFile("design-project1", "Bad.xlsx")
        ).rejects.toThrow(/Invalid file path.*400 Bad Request.*exact 'file' field value from list_tables/);
      });
    });
  });

  describe("Deployment", () => {
    describe("deployProject", () => {
      it("should deploy project to production repository", async () => {
        // deployProject uses /deployments endpoint with projectId in body
        const projectIdForPath = "design-project1";

        mockAxios.onPost("/deployments").reply((config) => {
          const data = JSON.parse(config.data);
          expect(data.projectId).toBe(projectIdForPath);
          expect(data.deploymentName).toBe("project1");
          expect(data.productionRepositoryId).toBe("production-deploy");
          return [200];
        });

        await client.deployProject({
          projectId: "design-project1",
          deploymentName: "project1",
          productionRepositoryId: "production-deploy",
        });
      });
    });

    describe("listDeployments", () => {
      it("should fetch list of deployments", async () => {
        const mockDeployments = [
          { name: "deployment1", status: "active" },
          { name: "deployment2", status: "inactive" },
        ];

        mockAxios.onGet("/deployments").reply(200, mockDeployments);

        const result = await client.listDeployments();
        expect(result.length).toBe(2);
      });

      it("forwards repository and project filters", async () => {
        mockAxios.onGet("/deployments", {
          params: { repository: "production", project: "InsuranceRules" },
        }).reply(200, []);

        await client.listDeployments({ repository: "production", project: "InsuranceRules" });
        expect(mockAxios.history.get).toHaveLength(1);
      });
    });
  });

  describe("Error Handling", () => {
    it("should handle 401 unauthorized", async () => {
      mockAxios.onGet("/repos").reply(401);

      await expect(client.listRepositories()).rejects.toThrow();
    });

    it("should handle 403 forbidden", async () => {
      mockAxios.onGet("/repos").reply(403);

      await expect(client.listRepositories()).rejects.toThrow();
    });

    it("should handle 500 server error", async () => {
      mockAxios.onGet("/repos").reply(500);

      await expect(client.listRepositories()).rejects.toThrow();
    });

    it("should handle timeout errors", async () => {
      mockAxios.onGet("/repos").timeout();

      await expect(client.listRepositories()).rejects.toThrow();
    });

    it("should handle malformed JSON responses", async () => {
      mockAxios.onGet("/repos").reply(200, "not json");

      // Should not throw during request, axios handles parsing
      await client.listRepositories();
    });
  });

  describe("Test Execution Session", () => {
    // Helper: encoded project path for "design-project1"
    const projectIdForPath = "design-project1";
    const encodedProjectId = encodeURIComponent(projectIdForPath);
    const projectPath = `/projects/${encodedProjectId}`;

    // Minimal project stub returned by getProject (needed by startProjectTests)
    const mockOpenProject = {
      id: "design:project1:hash123",
      name: "project1",
      repository: "design",
      status: "OPENED",
      path: "project1",
      modifiedBy: "admin",
      modifiedAt: "2024-01-01T00:00:00Z",
    };

    // Minimal test execution summary returned by /tests/summary
    const mockSummary: Partial<Types.TestsExecutionSummary> = {
      executionTimeMs: 42,
      numberOfTests: 5,
      numberOfFailures: 1,
      testCases: [
        {
          name: "TestTable",
          tableId: "test_table_abc",
          executionTimeMs: 10,
          numberOfTests: 3,
          numberOfFailures: 0,
          testUnits: [],
        },
        {
          name: "OtherTest",
          tableId: "other_table_xyz",
          executionTimeMs: 32,
          numberOfTests: 2,
          numberOfFailures: 1,
          testUnits: [],
        },
      ],
      pageNumber: 0,
      pageSize: 50,
      numberOfElements: 2,
    };

    /**
     * Start a test session via startProjectTests and return the response.
     * Registers the necessary mocks for getProject and /tests/run.
     */
    const startSession = async (tableId?: string) => {
      // getProject mock — project is already OPENED
      mockAxios.onGet(projectPath).reply(200, mockOpenProject);

      // /tests/run mock — reply with a Set-Cookie header so the client stores it
      mockAxios.onPost(`${projectPath}/tests/run`).reply(
        200,
        { status: "ok" },
        { "Set-Cookie": "JSESSIONID=test-session-123; Path=/" }
      );

      return client.startProjectTests("design-project1", tableId ? { tableId } : undefined);
    };

    it("should start tests with tableId and pass tableId as query param", async () => {
      mockAxios.onGet(projectPath).reply(200, mockOpenProject);

      mockAxios.onPost(`${projectPath}/tests/run`).reply((config) => {
        expect(config.params).toBeDefined();
        expect(config.params.tableId).toBe("my_table_42");
        return [200, { status: "ok" }, { "Set-Cookie": "JSESSIONID=sess1; Path=/" }];
      });

      const result = await client.startProjectTests("design-project1", { tableId: "my_table_42" });
      expect(result.status).toBe("started");
      expect(result.tableId).toBe("my_table_42");
    });

    it("passes fromModule to the current tests API", async () => {
      mockAxios.onGet(projectPath).reply(200, mockOpenProject);
      mockAxios.onPost(`${projectPath}/tests/run`).reply((config) => {
        expect(config.params).toEqual({ tableId: "my_table_42", fromModule: "Rules" });
        return [200, { status: "ok" }, { "Set-Cookie": "JSESSIONID=sess2; Path=/" }];
      });

      await client.startProjectTests("design-project1", {
        tableId: "my_table_42",
        fromModule: "Rules",
      });
    });

    it("should reuse stored headers in getTestResultsSummary after starting with tableId", async () => {
      await startSession("specific_table_99");

      // Now getTestResultsSummary should find the stored headers
      mockAxios.onGet(`${projectPath}/tests/summary`).reply((config) => {
        // The stored Cookie header should be forwarded
        expect(config.headers?.["Cookie"]).toBe("JSESSIONID=test-session-123");
        return [200, mockSummary];
      });

      const summary = await client.getTestResultsSummary("design-project1");
      assertResultReady(summary);
      expect(summary.numberOfTests).toBe(5);
      expect(summary.numberOfFailures).toBe(1);
      expect(summary.numberOfPassed).toBe(4);
    });

    it("should pass unpaged=true in getTestResultsSummary", async () => {
      await startSession("specific_table_99");

      mockAxios.onGet(`${projectPath}/tests/summary`, { params: { unpaged: true } }).reply(200, mockSummary);

      const summary = await client.getTestResultsSummary("design-project1", { unpaged: true });
      assertResultReady(summary);
      expect(summary.numberOfTests).toBe(5);
    });

    it("should reuse stored headers in getTestResults after starting with tableId", async () => {
      await startSession("specific_table_99");

      mockAxios.onGet(`${projectPath}/tests/summary`).reply((config) => {
        expect(config.headers?.["Cookie"]).toBe("JSESSIONID=test-session-123");
        return [200, mockSummary];
      });

      const results = await client.getTestResults("design-project1");
      assertResultReady(results);
      expect(results.testCases).toHaveLength(2);
    });

    it("should pass unpaged=true in getTestResults", async () => {
      await startSession("specific_table_99");

      mockAxios.onGet(`${projectPath}/tests/summary`, { params: { unpaged: true } }).reply(200, mockSummary);

      const results = await client.getTestResults("design-project1", { unpaged: true });
      assertResultReady(results);
      expect(results.testCases).toHaveLength(2);
    });

    it("should reuse stored headers in getTestResultsByTable after starting with tableId", async () => {
      await startSession("test_table_abc");

      // getTestResultsByTable iterates pages via getTestResults
      mockAxios.onGet(`${projectPath}/tests/summary`).reply((config) => {
        expect(config.headers?.["Cookie"]).toBe("JSESSIONID=test-session-123");
        expect(config.params).toEqual({ page: 0, size: 50 });
        return [200, mockSummary];
      });

      const results = await client.getTestResultsByTable("design-project1", "test_table_abc");
      assertResultReady(results);
      // Only the matching testCase should be returned
      expect(results.testCases).toHaveLength(1);
      expect(results.testCases[0].tableId).toBe("test_table_abc");
    });

    it("should overwrite previous session when starting new tests for the same project", async () => {
      // --- First session ---
      mockAxios.onGet(projectPath).reply(200, mockOpenProject);
      mockAxios.onPost(`${projectPath}/tests/run`).replyOnce(
        200,
        { status: "ok" },
        { "Set-Cookie": "JSESSIONID=session-AAA; Path=/" }
      );
      await client.startProjectTests("design-project1", { tableId: "table_a" });

      // --- Second session (overwrites first) ---
      mockAxios.onPost(`${projectPath}/tests/run`).replyOnce(
        200,
        { status: "ok" },
        { "Set-Cookie": "JSESSIONID=session-BBB; Path=/" }
      );
      await client.startProjectTests("design-project1", { tableId: "table_b" });

      // getTestResultsSummary should use the SECOND session's cookie
      mockAxios.onGet(`${projectPath}/tests/summary`).reply((config) => {
        expect(config.headers?.["Cookie"]).toBe("JSESSIONID=session-BBB");
        return [200, mockSummary];
      });

      const summary = await client.getTestResultsSummary("design-project1");
      assertResultReady(summary);
      expect(summary.numberOfTests).toBe(5);
    });

    it("should throw when getTestResultsSummary is called without starting tests", async () => {
      await expect(
        client.getTestResultsSummary("design-project1")
      ).rejects.toThrow(/No test execution session found/);
    });

    it("should throw when getTestResults is called without starting tests", async () => {
      await expect(
        client.getTestResults("design-project1")
      ).rejects.toThrow(/No test execution session found/);
    });

    it("should start tests without tableId and still allow retrieving results", async () => {
      await startSession(); // no tableId

      mockAxios.onGet(`${projectPath}/tests/summary`).reply((config) => {
        expect(config.headers?.["Cookie"]).toBe("JSESSIONID=test-session-123");
        return [200, mockSummary];
      });

      const summary = await client.getTestResultsSummary("design-project1");
      assertResultReady(summary);
      expect(summary.numberOfTests).toBe(5);
    });

    it("should auto-open closed project before starting tests", async () => {
      const closedProject = { ...mockOpenProject, status: "CLOSED" };
      mockAxios.onGet(projectPath).reply(200, closedProject);

      // openProject calls ensureNotLocalRepository → getProject, then PATCH
      mockAxios.onPatch(projectPath).reply(204);

      mockAxios.onPost(`${projectPath}/tests/run`).reply(
        200,
        { status: "ok" },
        { "Set-Cookie": "JSESSIONID=opened-session; Path=/" }
      );

      const result = await client.startProjectTests("design-project1");
      expect(result.projectWasOpened).toBe(true);

      // Session should still be usable
      mockAxios.onGet(`${projectPath}/tests/summary`).reply((config) => {
        expect(config.headers?.["Cookie"]).toBe("JSESSIONID=opened-session");
        return [200, mockSummary];
      });

      const summary = await client.getTestResultsSummary("design-project1");
      assertResultReady(summary);
      expect(summary.numberOfTests).toBe(5);
    });

    it("should not call openProject for local repository; start tests directly", async () => {
      const localProjectId = "local-project1";
      const localProjectPath = `/projects/${encodeURIComponent(localProjectId)}`;

      const mockLocalProject = {
        id: "local:project1",
        name: "Local Project",
        repository: "local",
        status: "CLOSED",
        path: "Local Project",
        modifiedBy: "admin",
        modifiedAt: "2024-01-01T00:00:00Z",
      };

      mockAxios.onGet(localProjectPath).reply(200, mockLocalProject);
      mockAxios.onPost(`${localProjectPath}/tests/run`).reply(
        200,
        { status: "ok" },
        { "Set-Cookie": "JSESSIONID=local-session; Path=/" }
      );

      const result = await client.startProjectTests(localProjectId);

      expect(result.status).toBe("started");
      expect(result.projectId).toBe(localProjectId);
      expect(result.projectWasOpened).toBe(false);
      expect(mockAxios.history.patch).toHaveLength(0);
    });
  });

  describe("URL Encoding", () => {
    it("should encode special characters in repository names", async () => {
      mockAxios.onGet(/repos\/.*\/branches/).reply((config) => {
        expect(config.url).toMatch(/my%20special%20repo/);
        return [200, []];
      });

      await client.listBranches("my special repo");
    });

    it("should encode special characters in project names", async () => {
      // buildProjectPath URL-encodes the projectId, so spaces become %20 (hyphens are
      // left literal by encodeURIComponent). The full path must be exactly encoded;
      // this assertion fails if encodeURIComponent is dropped.
      mockAxios.onGet(/\/projects\/.*/).reply((config) => {
        expect(config.url).toBe("/projects/design-Example%201%20-%20Bank%20Rating");
        return [200, {}];
      });

      await client.getProject("design-Example 1 - Bank Rating");
    });

    it("should encode special characters in file names", async () => {
      mockAxios.onGet(/files\/.*/).reply((config) => {
        expect(config.url).toMatch(/My%20Rules%20%231.xlsx/);
        return [200, Buffer.from("")];
      });

      await client.downloadFile("design-project1", "My Rules #1.xlsx");
    });
  });

  describe("Project Creation & Repository Files", () => {
    it("createProjectFromZip PUTs multipart to /repos/{repo}/projects/{name} and returns the revision", async () => {
      let capturedContentType: string | undefined;
      mockAxios.onPut("/repos/design/projects/Offer-CW").reply((config) => {
        capturedContentType = (config.headers?.["Content-Type"] ?? config.headers?.["content-type"]) as string;
        return [200, { revision: "abc123", branch: "main" }];
      });

      const result = await client.createProjectFromZip(
        "design",
        "Offer-CW",
        Buffer.from("PK-zip-bytes"),
        { comment: "Initial commit", branch: "feature/rates" }
      );

      expect(result).toEqual({ revision: "abc123", branch: "main" });
      expect(capturedContentType).toMatch(/^multipart\/form-data; boundary=/);
      const body = (mockAxios.history.put[0].data as { getBuffer: () => Buffer }).getBuffer().toString("utf-8");
      expect(body).toContain('name="branch"');
      expect(body).toContain("feature/rates");
    });

    it("createProjectFromZip includes the comment field in the multipart body", async () => {
      mockAxios.onPut("/repos/design/projects/Offer-CW").reply(200, { revision: "r1" });

      await client.createProjectFromZip("design", "Offer-CW", Buffer.from("zip"), { comment: "Hello audit" });

      // form-data with only Buffer/string parts exposes getBuffer().
      const form = mockAxios.history.put[0].data as unknown as { getBuffer: () => Buffer };
      const body = form.getBuffer().toString("utf-8");
      expect(body).toContain('name="template"; filename="template.zip"');
      expect(body).toContain('name="comment"');
      expect(body).toContain("Hello audit");
    });

    it("copyProject uses Studio's server-side project-copy endpoint", async () => {
      mockAxios.onPost("/repos/design/projects/Offer-CW/from-project").reply((config) => {
        expect(JSON.parse(config.data)).toEqual({
          sourceProject: "mapped:Offer-US:opaque-id",
          comment: "Copy rates",
          branch: "feature/rates",
        });
        return [200, { revision: "copy123", branch: "feature/rates" }];
      });

      await expect(client.copyProject(
        "design",
        "Offer-CW",
        "mapped:Offer-US:opaque-id",
        { comment: "Copy rates", branch: "feature/rates" },
      )).resolves.toEqual({ revision: "copy123", branch: "feature/rates" });
    });

  });

  describe("Session continuity (firstRequestGate)", () => {
    it("shares one JSESSIONID across concurrent calls when the bootstrap request issues no cookie", async () => {
      // Studio reality: GET /repos issues NO Set-Cookie; GET /projects issues a fresh
      // JSESSIONID for any request that arrives without one. If the turn's first request
      // (the bootstrap) lands on /repos, naive gating releases all waiting siblings
      // cookie-less and each opens its own studio session. The gate must keep
      // serializing until a cookie actually lands.
      let sessionsIssued = 0;
      const cookiesSentToProjects: Array<string | undefined> = [];

      mockAxios.onGet("/repos").reply(200, [{ id: "design", name: "Design" }]); // no Set-Cookie

      mockAxios.onGet(/\/projects\/[^/]+\/status$/).reply((config) => {
        const cookie = (config.headers?.Cookie ?? config.headers?.cookie) as string | undefined;
        cookiesSentToProjects.push(cookie);
        if (cookie && cookie.includes("JSESSIONID=")) {
          return [200, { compileState: "ok" }]; // reuse existing session, no new cookie
        }
        sessionsIssued += 1;
        return [200, { compileState: "ok" }, { "set-cookie": [`JSESSIONID=SESS-${sessionsIssued}; Path=/`] }];
      });

      // listRepositories() (→ /repos, first in array) bootstraps; two concurrent status calls follow.
      await Promise.all([
        client.listRepositories(),
        client.getProjectStatus("p1"),
        client.getProjectStatus("p2"),
      ]);

      // Exactly one session is ever issued, and only one /projects request went out cookie-less.
      expect(sessionsIssued).toBe(1);
      const cookieless = cookiesSentToProjects.filter((c) => !c || !c.includes("JSESSIONID=")).length;
      expect(cookieless).toBe(1);
    });
  });

  describe("Project Files (BETA)", () => {
    describe("readProjectFile", () => {
      it("returns file bytes plus content-type/disposition headers and forwards version/branch", async () => {
        let seenParams: Record<string, unknown> | undefined;
        mockAxios.onGet("/projects/p1/files/rules/Model.xlsx").reply((config) => {
          seenParams = config.params;
          return [200, "hello-bytes", {
            "content-type": "text/plain",
            "content-disposition": "attachment; filename=Model.xlsx",
          }];
        });

        const res = await client.readProjectFile("p1", "rules/Model.xlsx", {
          version: "abc123",
          branch: "main",
        });

        expect(res.data.toString("utf-8")).toBe("hello-bytes");
        expect(res.contentType).toBe("text/plain");
        expect(res.contentDisposition).toMatch(/attachment/i);
        expect(seenParams).toMatchObject({ version: "abc123", branch: "main" });
      });

      it("lists the project root with a trailing-slash URL when path is empty", async () => {
        const listing = JSON.stringify([{ path: "Bank Rating.xlsx", name: "Bank Rating.xlsx", type: "file" }]);
        let url = "";
        mockAxios.onGet(/\/projects\/p1\/files\/?/).reply((config) => {
          url = config.url || "";
          return [200, listing, { "content-type": "application/json" }];
        });

        const res = await client.readProjectFile("p1", "", { recursive: true, viewMode: "FLAT" });

        expect(url).toBe("/projects/p1/files/");
        expect(res.contentType).toBe("application/json");
        expect(res.contentDisposition).toBe("");
        expect(JSON.parse(res.data.toString("utf-8"))[0].name).toBe("Bank Rating.xlsx");
      });

      it("percent-encodes path segments but preserves '/' separators", async () => {
        let url = "";
        mockAxios.onGet(/\/projects\/p1\/files\//).reply((config) => {
          url = config.url || "";
          return [200, "x", { "content-type": "application/octet-stream", "content-disposition": "attachment" }];
        });

        await client.readProjectFile("p1", "a b/c.xml");

        expect(url).toBe("/projects/p1/files/a%20b/c.xml");
      });

      it("rejects '.'/'..' path-traversal segments before issuing a request", async () => {
        let called = false;
        mockAxios.onAny().reply(() => {
          called = true;
          return [200, ""];
        });

        await expect(client.readProjectFile("p1", "rules/../../etc/passwd")).rejects.toThrow(/project-relative|not allowed/);
        await expect(client.deleteProjectFile("p1", "..")).rejects.toThrow(/project-relative|not allowed/);
        expect(called).toBe(false);
      });

      it("rejects '.'/'..' in copy/move body paths and search 'from' (defense-in-depth)", async () => {
        let called = false;
        mockAxios.onAny().reply(() => {
          called = true;
          return [200, []];
        });

        await expect(
          client.copyProjectFile("p1", { sourcePath: "a.xlsx", destinationPath: "../b.xlsx" })
        ).rejects.toThrow(/project-relative|not allowed/);
        await expect(
          client.copyProjectFile("p1", { sourcePath: "../a.xlsx", destinationPath: "b.xlsx" })
        ).rejects.toThrow(/project-relative|not allowed/);
        await expect(
          client.moveProjectFile("p1", { sourcePath: "a.xlsx", destinationPath: "sub/../../x.xlsx" })
        ).rejects.toThrow(/project-relative|not allowed/);
        await expect(
          client.searchProjectFiles("p1", { from: "../secrets" })
        ).rejects.toThrow(/project-relative|not allowed/);

        expect(called).toBe(false);
      });

      it("serializes extensions as a comma-separated query param", async () => {
        let seenParams: Record<string, unknown> | undefined;
        mockAxios.onGet(/\/projects\/p1\/files\/?/).reply((config) => {
          seenParams = config.params;
          return [200, "[]", { "content-type": "application/json" }];
        });

        await client.readProjectFile("p1", "", { extensions: ["xlsx", "xml"] });

        expect(seenParams?.extensions).toBe("xlsx,xml");
      });
    });

    describe("writeProjectFile / updateProjectFile", () => {
      it("writeProjectFile POSTs octet-stream bytes with createFolders/branch (create) and returns metadata", async () => {
        let captured: { headers?: Record<string, unknown>; params?: Record<string, unknown>; data?: unknown } = {};
        mockAxios.onPost("/projects/p1/files/docs/readme.md").reply((config) => {
          captured = config;
          return [201, { name: "readme.md", size: 5 }];
        });

        const meta = await client.writeProjectFile("p1", "docs/readme.md", Buffer.from("hello"), {
          createFolders: true,
          branch: "main",
        });

        const ct = (captured.headers?.["Content-Type"] ?? captured.headers?.["content-type"]) as string;
        expect(ct).toContain("octet-stream");
        // conflictPolicy is NOT sent on POST — the backend ignores it for single files (overwrite = PUT).
        expect(captured.params).toEqual({ createFolders: true, branch: "main" });
        expect(Buffer.from(captured.data as Buffer).toString("utf-8")).toBe("hello");
        expect(meta).toEqual({ name: "readme.md", size: 5 });
      });

      it("updateProjectFile PUTs octet-stream bytes (overwrite) with branch", async () => {
        let captured: { headers?: Record<string, unknown>; params?: Record<string, unknown>; data?: unknown } = {};
        mockAxios.onPut("/projects/p1/files/docs/readme.md").reply((config) => {
          captured = config;
          return [204];
        });

        await client.updateProjectFile("p1", "docs/readme.md", Buffer.from("v2"), { branch: "main" });

        const ct = (captured.headers?.["Content-Type"] ?? captured.headers?.["content-type"]) as string;
        expect(ct).toContain("octet-stream");
        expect(captured.params).toEqual({ branch: "main" });
        expect(Buffer.from(captured.data as Buffer).toString("utf-8")).toBe("v2");
      });
    });

    describe("deleteProjectFile", () => {
      it("DELETEs with the branch query param", async () => {
        let seenParams: Record<string, unknown> | undefined;
        mockAxios.onDelete("/projects/p1/files/old.txt").reply((config) => {
          seenParams = config.params;
          return [204];
        });

        await client.deleteProjectFile("p1", "old.txt", { branch: "dev" });

        expect(seenParams).toEqual({ branch: "dev" });
      });

      it("omits query params when no branch is given", async () => {
        let seenParams: Record<string, unknown> | undefined = { sentinel: true };
        mockAxios.onDelete("/projects/p1/files/old.txt").reply((config) => {
          seenParams = config.params;
          return [204];
        });

        await client.deleteProjectFile("p1", "old.txt");

        expect(seenParams).toBeUndefined();
      });
    });

    describe("searchProjectFiles", () => {
      it("POSTs the FileSearchQuery body and returns the FsNode array", async () => {
        const nodes = [{ path: "rules/M.xlsx", name: "M.xlsx", type: "file" }];
        let body: unknown;
        let params: Record<string, unknown> | undefined;
        mockAxios.onPost("/projects/p1/file-search").reply((config) => {
          body = JSON.parse(config.data);
          params = config.params;
          return [200, nodes];
        });

        const res = await client.searchProjectFiles(
          "p1",
          { pattern: "**/*.xlsx", content: "premium", type: "FILE" },
          { branch: "main", fields: "path,name" }
        );

        expect(body).toEqual({ pattern: "**/*.xlsx", content: "premium", type: "FILE" });
        expect(params).toMatchObject({ branch: "main", fields: "path,name" });
        expect(res).toEqual(nodes);
      });
    });

    describe("getProjectAgentContext", () => {
      const ancestorsNodes = [
        { path: "foo/Project-1/AGENTS.md", name: "AGENTS.md", type: "file", basePath: "foo/Project-1", size: 7, lastModified: "2026-06-12T10:47:05Z", content: "project" },
        { path: "foo/AGENTS.md", name: "AGENTS.md", type: "file", basePath: "foo", size: 0, lastModified: "2026-06-12T10:47:06Z", content: "" },
      ];

      it("issues a fixed ANCESTORS search from AGENTS.md and returns path+content nearest-first", async () => {
        let body: Record<string, unknown> = {};
        mockAxios.onPost("/projects/p1/file-search").reply((config) => {
          body = JSON.parse(config.data);
          return [200, ancestorsNodes];
        });

        const res = await client.getProjectAgentContext("p1");

        // Direction + start path are fixed (not caller-controllable).
        expect(body).toEqual({ scope: "ANCESTORS", from: "AGENTS.md" });
        // Nearest-first, carrying only path/content (+ size/lastModified passthrough).
        expect(res).toEqual([
          { path: "foo/Project-1/AGENTS.md", content: "project", size: 7, lastModified: "2026-06-12T10:47:05Z" },
          { path: "foo/AGENTS.md", content: "", size: 0, lastModified: "2026-06-12T10:47:06Z" },
        ]);
      });

      it("does NOT call getProject (no enrichment round-trip)", async () => {
        mockAxios.onPost("/projects/p1/file-search").reply(200, ancestorsNodes);
        // No GET /projects/p1 mock registered: a call would 404 and surface here.
        await client.getProjectAgentContext("p1");
        expect(mockAxios.history.get.find((r) => r.url === "/projects/p1")).toBeUndefined();
      });

      it("starts the walk from a sub-folder when 'folder' is given", async () => {
        let body: Record<string, unknown> = {};
        mockAxios.onPost("/projects/p1/file-search").reply((config) => {
          body = JSON.parse(config.data);
          return [200, ancestorsNodes];
        });

        await client.getProjectAgentContext("p1", { folder: "/rules/pricing/" });

        // Leading/trailing slashes trimmed; '/AGENTS.md' appended.
        expect(body).toEqual({ scope: "ANCESTORS", from: "rules/pricing/AGENTS.md" });
      });

      it("threads the 'branch' option to the file-search query params", async () => {
        let params: Record<string, unknown> | undefined;
        mockAxios.onPost("/projects/p1/file-search").reply((config) => {
          params = config.params;
          return [200, ancestorsNodes];
        });

        await client.getProjectAgentContext("p1", { branch: "release" });
        expect(params).toMatchObject({ branch: "release" });
      });

      it("defaults missing content to an empty string", async () => {
        mockAxios.onPost("/projects/p1/file-search").reply(200, [
          { path: "foo/AGENTS.md", name: "AGENTS.md", type: "file", basePath: "foo" },
        ]);

        const res = await client.getProjectAgentContext("p1");
        expect(res).toEqual([{ path: "foo/AGENTS.md", content: "", size: undefined, lastModified: undefined }]);
      });

      it("returns an empty array when no AGENTS.md exists anywhere", async () => {
        mockAxios.onPost("/projects/p1/file-search").reply(200, []);
        await expect(client.getProjectAgentContext("p1")).resolves.toEqual([]);
      });
    });

    describe("copyProjectFile / moveProjectFile", () => {
      it("copies with a {sourcePath,destinationPath} body and branch param", async () => {
        let body: unknown;
        let params: Record<string, unknown> | undefined;
        mockAxios.onPost("/projects/p1/file-copy").reply((config) => {
          body = JSON.parse(config.data);
          params = config.params;
          return [201];
        });

        await client.copyProjectFile("p1", { sourcePath: "a.xlsx", destinationPath: "b.xlsx" }, { branch: "main" });

        expect(body).toEqual({ sourcePath: "a.xlsx", destinationPath: "b.xlsx" });
        expect(params).toEqual({ branch: "main" });
      });

      it("moves with a {sourcePath,destinationPath} body", async () => {
        let body: unknown;
        mockAxios.onPost("/projects/p1/file-move").reply((config) => {
          body = JSON.parse(config.data);
          return [204];
        });

        await client.moveProjectFile("p1", { sourcePath: "a.xlsx", destinationPath: "sub/a.xlsx" });

        expect(body).toEqual({ sourcePath: "a.xlsx", destinationPath: "sub/a.xlsx" });
      });

      it("normalizes body paths (strips leading slash) but does NOT percent-encode them", async () => {
        let copyBody: Record<string, unknown> = {};
        let searchBody: Record<string, unknown> = {};
        mockAxios.onPost("/projects/p1/file-copy").reply((config) => {
          copyBody = JSON.parse(config.data);
          return [201];
        });
        mockAxios.onPost("/projects/p1/file-search").reply((config) => {
          searchBody = JSON.parse(config.data);
          return [200, []];
        });

        // Leading slash is stripped; the space is preserved (NOT %20) — body paths are raw JSON.
        await client.copyProjectFile("p1", { sourcePath: "/rules/My File.xlsx", destinationPath: "/out/Copy File.xlsx" });
        expect(copyBody).toEqual({ sourcePath: "rules/My File.xlsx", destinationPath: "out/Copy File.xlsx" });

        await client.searchProjectFiles("p1", { from: "/rules" });
        expect(searchBody.from).toBe("rules");
      });
    });
  });
});

// ===========================================================================
// Additional OpenLClient method coverage (migrated from the former
// openl-client.test.ts). A separate describe with its own client/mock setup;
// it exercises the public methods the suite above leaves untested.
// ===========================================================================
describe("OpenLClient — additional method coverage", () => {
  let client: OpenLClient;
  let mockAxios: MockAdapter;

  beforeEach(() => {
    const config: OpenLConfig = {
      baseUrl: "http://localhost:8080",
      personalAccessToken: "openl_pat_test",
    };
    client = new OpenLClient(config);
    // @ts-ignore - Access private axiosInstance for mocking
    mockAxios = new MockAdapter(client.axiosInstance);
  });

  afterEach(() => {
    mockAxios.reset();
    mockAxios.restore();
  });

  describe("Repository Management (gap)", () => {
    describe("getRepositoryFeatures", () => {
      it("reads the feature flags embedded in the current /repos response", async () => {
        mockAxios.onGet("/repos").reply(200, [{
          aclId: "design",
          id: "design",
          name: "Design",
          features: { branches: true, mappedFolders: true, searchable: false },
        }]);

        const result = await client.getRepositoryFeatures("design");
        expect(result).toEqual({ branches: true, mappedFolders: true, searchable: false });
      });

      it("fails when /repos does not contain the requested id", async () => {
        mockAxios.onGet("/repos").reply(200, []);

        await expect(client.getRepositoryFeatures("missing")).rejects.toThrow(/not found/);
      });
    });

    describe("listDeployRepositories", () => {
      it("GETs /production-repos (NOT /repos) and returns the array", async () => {
        const repos: Types.Repository[] = [
          { id: "production-deploy", name: "Production Deployment" },
        ];
        mockAxios.onGet("/production-repos").reply(200, repos);

        const result = await client.listDeployRepositories();
        expect(result).toEqual(repos);
        // Must not hit the design-repos endpoint.
        expect(mockAxios.history.get.map((r) => r.url)).toEqual(["/production-repos"]);
      });

      it("does NOT cache — a second call hits the network again", async () => {
        mockAxios.onGet("/production-repos").reply(200, [{ id: "p", name: "P" }]);

        await client.listDeployRepositories();
        await client.listDeployRepositories();

        expect(mockAxios.history.get.filter((r) => r.url === "/production-repos")).toHaveLength(2);
      });
    });

    describe("getProductionRepositoryIdByName", () => {
      const repos: Types.Repository[] = [
        { id: "production-deploy", name: "Production Deployment" },
        { id: "staging-deploy", name: "Staging Deployment" },
      ];

      it("resolves an exact production-repo display name to its id", async () => {
        mockAxios.onGet("/production-repos").reply(200, repos);

        const id = await client.getProductionRepositoryIdByName("Production Deployment");
        expect(id).toBe("production-deploy");
      });

      it("throws listing available names when the name is unknown", async () => {
        mockAxios.onGet("/production-repos").reply(200, repos);

        await expect(
          client.getProductionRepositoryIdByName("Nope")
        ).rejects.toThrow(/"Nope" not found.*Production Deployment, Staging Deployment/);
      });
    });

    describe("getProjectRevisions", () => {
      const page: Types.PageResponse<Types.ProjectRevision> = {
        content: [
          {
            revisionNo: "abc123",
            createdAt: "2026-01-01T00:00:00Z",
            fullComment: "Initial",
            deleted: false,
            technicalRevision: false,
          },
        ],
        pageNumber: 0,
        pageSize: 50,
        numberOfElements: 1,
        total: 1,
      };

      it("uses the project-level history URL and forwards filter params", async () => {
        let seenUrl = "";
        let seenParams: Record<string, unknown> | undefined;
        const revisionProjectId = "design:Insurance Rules:abc/123";
        mockAxios.onGet(`/projects/${encodeURIComponent(revisionProjectId)}/history`).reply((config) => {
          seenUrl = config.url || "";
          seenParams = config.params;
          return [200, page];
        });

        const result = await client.getProjectRevisions(revisionProjectId, {
          search: "premium",
          techRevs: false,
          page: 0,
          size: 50,
        });

        expect(seenUrl).toBe(`/projects/${encodeURIComponent(revisionProjectId)}/history`);
        expect(seenParams).toEqual({ search: "premium", techRevs: false, page: 0, size: 50 });
        expect(result.content[0].revisionNo).toBe("abc123");
      });

      it("forwards a non-page-aligned revision offset without converting it to a page", async () => {
        let seenParams: Record<string, unknown> | undefined;
        mockAxios.onGet(`/projects/${encodeURIComponent("design:InsuranceRules")}/history`).reply((config) => {
          seenParams = config.params;
          return [200, page];
        });

        await client.getProjectRevisions("design:InsuranceRules", { offset: 25, size: 50 });

        expect(seenParams).toEqual({ offset: 25, size: 50 });
      });

      it("prefers an explicit page when called directly with both pagination forms", async () => {
        let seenParams: Record<string, unknown> | undefined;
        mockAxios.onGet(`/projects/${encodeURIComponent("design:InsuranceRules")}/history`).reply((config) => {
          seenParams = config.params;
          return [200, page];
        });

        await client.getProjectRevisions("design:InsuranceRules", { page: 2, offset: 25, size: 50 });

        expect(seenParams).toEqual({ page: 2, size: 50 });
      });
    });

  });

  describe("Project lifecycle (gap)", () => {
    const projectId = "design-project1";
    const projectPath = `/projects/${encodeURIComponent(projectId)}`;
    const designProject = (status: string) => ({
      id: "design:project1:hash",
      name: "project1",
      repository: "design",
      status,
      path: "project1",
      modifiedBy: "admin",
      modifiedAt: "2026-01-01T00:00:00Z",
    });

    describe("deleteProject", () => {
      it("DELETEs /projects/{id} (no preceding GET / local-repo guard)", async () => {
        mockAxios.onDelete(projectPath).reply(204);

        await expect(client.deleteProject(projectId)).resolves.toBeUndefined();
        expect(mockAxios.history.delete).toHaveLength(1);
        expect(mockAxios.history.get).toHaveLength(0);
      });
    });

    describe("listProjectBranches", () => {
      it("preserves the omitted project default and passes repository scope", async () => {
        const scopes: unknown[] = [];
        mockAxios.onGet(`${projectPath}/branches`).reply((config) => {
          scopes.push(config.params?.scope);
          return [200, []];
        });

        await client.listProjectBranches(projectId);
        await client.listProjectBranches(projectId, "repository");

        expect(scopes).toEqual([undefined, "repository"]);
      });
    });

    describe("deleteProjectBranch", () => {
      it("rejects traversal segments before issuing a request", async () => {
        await expect(client.deleteProjectBranch(projectId, "feature/../../main")).rejects.toThrow(
          /project-relative|not allowed/,
        );
        expect(mockAxios.history.delete).toHaveLength(0);
      });
    });

    describe("closeProject", () => {
      it("PATCHes status CLOSED with the comment after confirming the repo is not local", async () => {
        mockAxios.onGet(projectPath).reply(200, designProject("OPENED"));
        let body: Record<string, unknown> = {};
        mockAxios.onPatch(projectPath).reply((config) => {
          body = JSON.parse(config.data);
          return [204];
        });

        const ok = await client.closeProject(projectId, { comment: "Done for now" });
        expect(ok).toBe(true);
        expect(body).toMatchObject({ status: "CLOSED", comment: "Done for now" });
      });

      it("refuses to close a project in the local repository", async () => {
        const localId = "local-myproject";
        mockAxios.onGet(`/projects/${encodeURIComponent(localId)}`).reply(200, {
          ...designProject("OPENED"),
          id: "local:myproject:hash",
          repository: "local",
        });

        await expect(client.closeProject(localId)).rejects.toThrow(
          /local repository.*not connected to a remote Git/i
        );
        expect(mockAxios.history.patch).toHaveLength(0);
      });
    });

    describe("saveProject", () => {
      it("short-circuits with 'nothing to save' and issues NO PATCH when the project is not EDITING", async () => {
        mockAxios.onGet(projectPath).reply(200, designProject("OPENED"));

        const result = await client.saveProject(projectId, "irrelevant comment");
        expect(result.success).toBe(true);
        expect(result.message).toMatch(/nothing to save/i);
        expect(mockAxios.history.patch).toHaveLength(0);
      });

      it("requires a non-empty comment when the project IS EDITING", async () => {
        mockAxios.onGet(projectPath).reply(200, designProject("EDITING"));

        await expect(client.saveProject(projectId, "   ")).rejects.toThrow(
          /comment is required for save/i
        );
        expect(mockAxios.history.patch).toHaveLength(0);
      });

      it("PATCHes the trimmed comment when EDITING without calling the removed validation endpoint", async () => {
        mockAxios.onGet(projectPath).reply(200, designProject("EDITING"));
        let body: Record<string, unknown> = {};
        mockAxios.onPatch(projectPath).reply((config) => {
          body = JSON.parse(config.data);
          return [204];
        });

        const result = await client.saveProject(projectId, "  Add premium rule  ");
        expect(result.success).toBe(true);
        expect(body).toEqual({ comment: "Add premium rule" });
        // closeAfterSave was not requested -> no status field.
        expect(body.status).toBeUndefined();
        expect(mockAxios.history.get.map((request) => request.url)).toEqual([projectPath]);
      });

      it("adds status CLOSED to the PATCH body when closeAfterSave is set", async () => {
        mockAxios.onGet(projectPath).reply(200, designProject("EDITING"));
        let body: Record<string, unknown> = {};
        mockAxios.onPatch(projectPath).reply((config) => {
          body = JSON.parse(config.data);
          return [204];
        });

        await client.saveProject(projectId, "Commit and close", { closeAfterSave: true });
        expect(body).toEqual({ comment: "Commit and close", status: "CLOSED" });
      });

    });

    describe("createBranch", () => {
      it("POSTs {branch, revision} to /projects/{id}/branches", async () => {
        let body: Record<string, unknown> = {};
        mockAxios.onPost(`${projectPath}/branches`).reply((config) => {
          body = JSON.parse(config.data);
          return [201];
        });

        const ok = await client.createBranch(projectId, "feature/new-rules", "abc123");
        expect(ok).toBe(true);
        expect(body).toEqual({ branch: "feature/new-rules", revision: "abc123" });
      });
    });
  });

  describe("Local change history", () => {
    describe("getProjectLocalChanges", () => {
      it("GETs the explicitly addressed project and module history", async () => {
        let params: Record<string, unknown> | undefined;
        const items: Types.ProjectHistoryItem[] = [
          { id: "history-1", current: true, modifiedOn: "2026-01-01T00:00:00Z" },
        ];
        mockAxios.onGet("/projects/design%3AProject/local-history").reply((config) => {
          params = config.params;
          return [200, items];
        });

        const result = await client.getProjectLocalChanges("design:Project", "Main Rules");
        expect(result).toEqual(items);
        expect(params).toEqual({ module: "Main Rules" });
      });
    });

    describe("restoreProjectLocalChange", () => {
      it("POSTs the selected version as JSON for the explicitly addressed project and module", async () => {
        let body: unknown;
        let contentType: string | undefined;
        let params: Record<string, unknown> | undefined;
        mockAxios.onPost("/projects/design%3AProject/local-history/restore").reply((config) => {
          body = JSON.parse(config.data);
          contentType = (config.headers?.["Content-Type"] ?? config.headers?.["content-type"]) as string;
          params = config.params;
          return [204];
        });

        await client.restoreProjectLocalChange("design:Project", "Main Rules", "hist-42");
        expect(body).toEqual({ version: "hist-42" });
        expect(contentType).toContain("application/json");
        expect(params).toEqual({ module: "Main Rules" });
      });
    });
  });

  describe("Table writes (gap)", () => {
    const projectId = "design-project1";
    const projectPath = `/projects/${encodeURIComponent(projectId)}`;

    describe("createProjectTable (BETA contract)", () => {
      it("POSTs the current create-table fields and returns the created metadata", async () => {
        let body: Record<string, unknown> = {};
        const created: Partial<Types.TableMetadata> = { id: "newTable_999", name: "newTable" };
        mockAxios.onPost(`${projectPath}/tables`).reply((config) => {
          body = JSON.parse(config.data);
          return [201, created];
        });

        const request: Types.CreateNewTableRequest = {
          moduleName: "Rules",
          modulePath: "rules/Rules.xlsx",
          sheetName: "Sheet1",
          table: { id: "", name: "newTable", tableType: "RawSource", kind: "Rules", source: [[{ value: "Rules void newTable()" }]] },
        };
        const result = await client.createProjectTable(projectId, request);

        expect(body).toEqual({
          moduleName: "Rules",
          modulePath: "rules/Rules.xlsx",
          sheetName: "Sheet1",
          table: { id: "", name: "newTable", tableType: "RawSource", kind: "Rules", source: [[{ value: "Rules void newTable()" }]] },
        });
        expect(result.id).toBe("newTable_999");
      });
    });

    describe("appendProjectTable", () => {
      it("POSTs the append payload to /tables/{id}/lines and returns undefined on a 204 in-place write", async () => {
        const tableId = "Customer_1234";
        let body: Record<string, unknown> = {};
        mockAxios.onPost(`${projectPath}/tables/${encodeURIComponent(tableId)}/lines`).reply((config) => {
          body = JSON.parse(config.data);
          return [204];
        });

        const appendData: Types.RawTableAppend = {
          tableType: "RawSource",
          rows: [[{ value: "email" }, { value: "String" }]],
        };
        const newId = await client.appendProjectTable(projectId, tableId, appendData);

        expect(body).toEqual(appendData);
        expect(newId).toBeUndefined();
      });

      it("returns the relocated table id from the 200 response body when the write moved the table", async () => {
        const tableId = "Customer_1234";
        mockAxios
          .onPost(`${projectPath}/tables/${encodeURIComponent(tableId)}/lines`)
          .reply(200, { id: "Customer_5678" });

        const appendData: Types.RawTableAppend = {
          tableType: "RawSource",
          rows: [[{ value: "SAFE" }, { value: 1000 }]],
        };
        const newId = await client.appendProjectTable(projectId, tableId, appendData);
        expect(newId).toBe("Customer_5678");
      });

      it("falls back to the Location header for the relocated id when the body carries none", async () => {
        const tableId = "Customer_1234";
        mockAxios
          .onPost(`${projectPath}/tables/${encodeURIComponent(tableId)}/lines`)
          .reply(200, {}, { location: "/projects/design-project1/tables/Customer_relocated%20id" });

        const newId = await client.appendProjectTable(projectId, tableId, {
          tableType: "RawSource",
          rows: [[{ value: 1 }]],
        });
        // Location segment is URL-decoded.
        expect(newId).toBe("Customer_relocated id");
      });
    });

    describe("editTableSource", () => {
      it("POSTs the action to /tables/{id}/actions and returns undefined on a 204 in-place edit", async () => {
        const tableId = "Customer_1234";
        let url = "";
        let body: Record<string, unknown> = {};
        mockAxios.onPost(`${projectPath}/tables/${encodeURIComponent(tableId)}/actions`).reply((config) => {
          url = config.url || "";
          body = JSON.parse(config.data);
          return [204];
        });

        const action: Types.RawTableSourceAction = {
          operation: "update",
          target: { type: "cell", row: 2, column: 1, value: "X" },
        };
        const newId = await client.editTableSource(projectId, tableId, action);

        expect(url).toBe(`${projectPath}/tables/${encodeURIComponent(tableId)}/actions`);
        expect(body).toEqual(action);
        expect(newId).toBeUndefined();
      });

      it("returns the relocated table id from the 200 response body when the edit moved the table", async () => {
        const tableId = "Customer_1234";
        mockAxios
          .onPost(`${projectPath}/tables/${encodeURIComponent(tableId)}/actions`)
          .reply(200, { id: "Customer_5678" });

        const newId = await client.editTableSource(projectId, tableId, {
          operation: "insert",
          target: { type: "row", position: 1 },
        });
        expect(newId).toBe("Customer_5678");
      });

      it("URL-encodes the table id in the actions path", async () => {
        const tableId = "Customer 1234";
        let url = "";
        mockAxios.onPost(`${projectPath}/tables/${encodeURIComponent(tableId)}/actions`).reply((config) => {
          url = config.url || "";
          return [204];
        });

        await client.editTableSource(projectId, tableId, {
          operation: "delete",
          target: { type: "row", position: 3 },
        });
        expect(url).toBe(`${projectPath}/tables/Customer%201234/actions`);
      });
    });

    describe("deleteTable", () => {
      it("DELETEs /tables/{id} and resolves on a 204", async () => {
        const tableId = "Customer_1234";
        let url = "";
        let method = "";
        mockAxios.onDelete(`${projectPath}/tables/${encodeURIComponent(tableId)}`).reply((config) => {
          url = config.url || "";
          method = config.method || "";
          return [204];
        });

        await expect(client.deleteTable(projectId, tableId)).resolves.toBeUndefined();
        expect(method).toBe("delete");
        expect(url).toBe(`${projectPath}/tables/${encodeURIComponent(tableId)}`);
      });

      it("URL-encodes the table id in the delete path", async () => {
        const tableId = "Customer 1234";
        let url = "";
        mockAxios.onDelete(`${projectPath}/tables/${encodeURIComponent(tableId)}`).reply((config) => {
          url = config.url || "";
          return [204];
        });

        await client.deleteTable(projectId, tableId);
        expect(url).toBe(`${projectPath}/tables/Customer%201234`);
      });
    });
  });

  describe("Deployment (gap)", () => {
    describe("redeployProject", () => {
      it("POSTs {projectId, comment} to /deployments/{deploymentId} (id URL-encoded)", async () => {
        let seenUrl = "";
        let body: Record<string, unknown> = {};
        mockAxios.onPost(/\/deployments\//).reply((config) => {
          seenUrl = config.url || "";
          body = JSON.parse(config.data);
          return [204];
        });

        const request: Types.RedeployProjectRequest = {
          projectId: "design-project1",
          comment: "Redeploy latest",
        };
        await client.redeployProject("deploy 001", request);

        expect(seenUrl).toBe("/deployments/deploy%20001");
        expect(body).toEqual({ projectId: "design-project1", comment: "Redeploy latest" });
      });
    });
  });

  describe("Health (gap)", () => {
    describe("healthCheck", () => {
      it("reports healthy + serverReachable when /repos responds", async () => {
        mockAxios.onGet("/repos").reply(200, [{ id: "design", name: "Design" }]);

        const health = await client.healthCheck();
        expect(health.status).toBe("healthy");
        expect(health.serverReachable).toBe(true);
        expect(health.baseUrl).toMatch(/\/rest$/);
        expect(health.authMethod).toContain("Personal Access Token");
        expect(health.error).toBeUndefined();
      });

      it("reports unhealthy with a sanitized error when /repos fails", async () => {
        mockAxios.onGet("/repos").reply(500);

        const health = await client.healthCheck();
        expect(health.status).toBe("unhealthy");
        expect(health.serverReachable).toBe(false);
        expect(typeof health.error).toBe("string");
        expect(health.error!.length).toBeGreaterThan(0);
      });
    });
  });

  describe("Session & auth accessors (gap)", () => {
    it("setSessionCookie installs a JSESSIONID that the request interceptor then sends", async () => {
      client.setSessionCookie("restored-123");
      expect(client.getSessionCookie()).toBe("restored-123");

      let cookie: string | undefined;
      mockAxios.onGet("/repos").reply((config) => {
        cookie = (config.headers?.Cookie ?? config.headers?.cookie) as string | undefined;
        return [200, []];
      });

      await client.listRepositories();
      expect(cookie).toContain("JSESSIONID=restored-123");
    });

    it("setSessionCookie(null) clears the stored cookie", () => {
      client.setSessionCookie("abc");
      client.setSessionCookie(null);
      expect(client.getSessionCookie()).toBeNull();
    });

    it("getAuthorizationHeader exposes the Token header for non-axios consumers (STOMP/WS handshake)", () => {
      const header = client.getAuthorizationHeader();
      expect(header).toBe("Token openl_pat_test");
    });
  });

  describe("Trace Debug API (BETA)", () => {
    const projectId = "p1";
    const projectPath = `/projects/${encodeURIComponent(projectId)}`;

    const suspendedStack: Types.DebugStackView = {
      status: "suspended",
      frames: [
        {
          index: 0,
          depth: 1,
          uri: "P/Rules.xlsx?sheet=Main&range=B2:D8",
          tableId: "calc_42",
          name: "CalcRule",
          kind: "spreadsheet",
          active: true,
          completed: false,
          error: false,
        },
      ],
    };

    describe("startTrace", () => {
      it("POSTs to /trace with tableId in the query, the inputJson serialized as JSON body, and returns the initial stack", async () => {
        let seenUrl = "";
        let body: unknown;
        let contentType: string | undefined;
        mockAxios.onPost(/\/projects\/p1\/trace\?/).reply((config) => {
          seenUrl = config.url || "";
          body = config.data;
          contentType = (config.headers?.["Content-Type"] ?? config.headers?.["content-type"]) as string;
          return [200, suspendedStack];
        });

        const stack = await client.startTrace({
          projectId,
          tableId: "calc_42",
          inputJson: { params: { age: 30 } },
        });

        expect(seenUrl).toContain(`${projectPath}/trace?`);
        expect(seenUrl).toContain("tableId=calc_42");
        expect(JSON.parse(body as string)).toEqual({ params: { age: 30 } });
        expect(contentType).toBe("application/json");
        expect(stack.status).toBe("suspended");
        expect(stack.frames[0].tableId).toBe("calc_42");
      });

      it("threads current trace controls into the query string and sends no body when inputJson is omitted", async () => {
        let seenUrl = "";
        let body: unknown;
        mockAxios.onPost(/\/projects\/p1\/trace\?/).reply((config) => {
          seenUrl = config.url || "";
          body = config.data;
          return [200, { status: "completed", frames: [] }];
        });

        await client.startTrace({
          projectId,
          tableId: "MyTest",
          testRanges: "1-3,5",
          stopAtEntry: false,
          profiling: true,
          detailedTitles: true,
          breakOnErrors: false,
          includeTree: false,
          profileTop: 30,
        });

        expect(seenUrl).toContain("tableId=MyTest");
        // URLSearchParams encodes the comma in "1-3,5".
        expect(seenUrl).toContain("testRanges=1-3%2C5");
        expect(seenUrl).toContain("stopAtEntry=false");
        expect(seenUrl).toContain("profiling=true");
        expect(seenUrl).toContain("detailedTitles=true");
        expect(seenUrl).toContain("breakOnErrors=false");
        expect(seenUrl).toContain("includeTree=false");
        expect(seenUrl).toContain("profileTop=30");
        expect(body).toBeUndefined();
      });
    });

    describe("getTraceStatus / getTraceStack", () => {
      it("GETs /trace/status and returns the lightweight status", async () => {
        mockAxios.onGet(`${projectPath}/trace/status`).reply(200, { status: "running" });

        const status = await client.getTraceStatus(projectId);
        expect(status).toEqual({ status: "running" });
      });

      it("GETs /trace/stack and returns the frames, forwarding view/includeTree/profileTop", async () => {
        let params: Record<string, unknown> | undefined;
        mockAxios.onGet(`${projectPath}/trace/stack`).reply((config) => {
          params = config.params;
          return [200, suspendedStack];
        });

        const stack = await client.getTraceStack(projectId, { view: "compact", includeTree: false, profileTop: 10 });
        expect(stack.frames).toHaveLength(1);
        expect(stack.frames[0].name).toBe("CalcRule");
        expect(params).toEqual({ view: "compact", includeTree: "false", profileTop: "10" });
      });
    });

    describe("getTraceTreeChildren", () => {
      it("GETs /trace/tree/children, forwarding uri/instance/step/offset/limit as (string) query params, and returns the page", async () => {
        let params: Record<string, unknown> | undefined;
        const page: Types.TreeChildrenView = {
          children: [
            {
              uri: "P/Rules.xlsx?sheet=Rules&range=A1:B4",
              name: "AgeFactor",
              kind: "decisionTable",
              instance: 0,
              durationMillis: 2,
              selfMillis: 2,
              steps: [{ ref: "R1C0", label: "$Factor", status: "executed", childrenTotal: 0 }],
              notRetained: 5,
            },
          ],
          total: 4,
        };
        mockAxios.onGet(`${projectPath}/trace/tree/children`).reply((config) => {
          params = config.params;
          return [200, page];
        });

        const result = await client.getTraceTreeChildren(projectId, {
          uri: "P/Rules.xlsx?sheet=Main&range=B2:D8",
          instance: 3,
          step: "R1C0",
          offset: 100,
          limit: 50,
        });

        // Every value is a string and passed via axios `params` so the URI's own
        // ?sheet=…&range=… is percent-encoded, not merged into the request path.
        expect(params).toEqual({
          uri: "P/Rules.xlsx?sheet=Main&range=B2:D8",
          instance: "3",
          step: "R1C0",
          offset: "100",
          limit: "50",
        });
        expect(result.total).toBe(4);
        expect(result.children[0].notRetained).toBe(5);
        expect(result.children[0].steps[0].childrenTotal).toBe(0);
      });

      it("omits offset/limit from the query when not given (backend defaults apply)", async () => {
        let params: Record<string, unknown> | undefined;
        mockAxios.onGet(`${projectPath}/trace/tree/children`).reply((config) => {
          params = config.params;
          return [200, { children: [], total: 0 }];
        });

        await client.getTraceTreeChildren(projectId, { uri: "P/R.xlsx", instance: 0, step: "R1C0" });

        expect(params).toEqual({ uri: "P/R.xlsx", instance: "0", step: "R1C0" });
      });
    });

    describe("traceStep", () => {
      it("POSTs /trace/step with the step type and view as query params and returns the new stack", async () => {
        let params: Record<string, unknown> | undefined;
        mockAxios.onPost(`${projectPath}/trace/step`).reply((config) => {
          params = config.params;
          return [200, suspendedStack];
        });

        const stack = await client.traceStep(projectId, "into", {
          view: "compact",
          includeTree: false,
          profileTop: 10,
        });
        expect(params).toEqual({ type: "into", view: "compact", includeTree: "false", profileTop: "10" });
        expect(stack.status).toBe("suspended");
      });
    });

    describe("traceResume", () => {
      it("POSTs /trace/resume and resolves on 202 without a body", async () => {
        mockAxios.onPost(`${projectPath}/trace/resume`).reply(202);

        await expect(client.traceResume(projectId)).resolves.toBeUndefined();
        expect(mockAxios.history.post).toHaveLength(1);
      });
    });

    describe("getTraceFrameVariables", () => {
      it("GETs /trace/frames/{index}/variables forwarding the fields projection", async () => {
        let params: Record<string, unknown> | undefined;
        const variables: Types.DebugFrameVariables = {
          parameters: [{ name: "age", description: "Integer", value: 30 }],
          steps: [],
          errors: [],
        };
        mockAxios.onGet(`${projectPath}/trace/frames/2/variables`).reply((config) => {
          params = config.params;
          return [200, variables];
        });

        const result = await client.getTraceFrameVariables(projectId, 2, "decision,ruleNames");
        expect(params).toEqual({ fields: "decision,ruleNames" });
        expect(result.parameters[0].name).toBe("age");
      });

      it("omits the fields param when no projection is given", async () => {
        let params: Record<string, unknown> | undefined;
        mockAxios.onGet(`${projectPath}/trace/frames/0/variables`).reply((config) => {
          params = config.params;
          return [200, { parameters: [], steps: [], errors: [] }];
        });

        await client.getTraceFrameVariables(projectId, 0);
        expect(params).toBeUndefined();
      });

      it("maps includeSchema on the frame-variables query", async () => {
        let params: Record<string, unknown> | undefined;
        mockAxios.onGet(`${projectPath}/trace/frames/0/variables`).reply((config) => {
          params = config.params;
          return [200, { parameters: [], steps: [], errors: [] }];
        });

        await client.getTraceFrameVariables(projectId, 0, undefined, true);
        expect(params).toEqual({ includeSchema: true });
      });
    });

    describe("getTraceFrameHighlights", () => {
      it("GETs /trace/frames/{index}/highlights and returns the A1-keyed overlay", async () => {
        const highlights: Types.CellHighlight[] = [{ cell: "C5", state: "current" }];
        mockAxios.onGet(`${projectPath}/trace/frames/1/highlights`).reply(200, highlights);

        const result = await client.getTraceFrameHighlights(projectId, 1);
        expect(result).toEqual(highlights);
      });
    });

    describe("breakpoints", () => {
      it("GETs /trace/breakpoints and returns the active keys", async () => {
        mockAxios.onGet(`${projectPath}/trace/breakpoints`).reply(200, ["MyDT#rule"]);

        const result = await client.getTraceBreakpoints(projectId);
        expect(result).toEqual(["MyDT#rule"]);
      });

      it("PUTs /trace/breakpoints with the uris wrapped in a BreakpointsRequest body", async () => {
        let body: unknown;
        mockAxios.onPut(`${projectPath}/trace/breakpoints`).reply((config) => {
          body = config.data;
          return [204];
        });

        await client.setTraceBreakpoints(projectId, ["CalcRule", "uri#R0C1"]);
        expect(JSON.parse(body as string)).toEqual({ uris: ["CalcRule", "uri#R0C1"] });
      });

      it("GETs /trace/breakpoint-tables and returns the targets", async () => {
        const targets: Types.BreakpointTableView[] = [{ name: "CalcRule", kind: "spreadsheet" }];
        mockAxios.onGet(`${projectPath}/trace/breakpoint-tables`).reply(200, targets);

        const result = await client.getTraceBreakpointTables(projectId);
        expect(result).toEqual(targets);
      });
    });

    describe("getTraceParameter", () => {
      it("GETs /trace/parameters/{parameterId} forwarding the fields projection, omitting it when not given", async () => {
        const fieldsSeen: Array<string | undefined> = [];
        const param: Types.TraceParameterValue = {
          name: "premium",
          description: "computed premium",
          value: 1000,
        };
        mockAxios.onGet(`${projectPath}/trace/parameters/5`).reply((config) => {
          fieldsSeen.push(config.params?.fields);
          return [200, param];
        });

        const result = await client.getTraceParameter(projectId, 5, "name,description,value");
        expect(result).toEqual(param);
        expect(fieldsSeen[0]).toBe("name,description,value");

        await client.getTraceParameter(projectId, 5);
        expect(fieldsSeen[1]).toBeUndefined();
      });

      it("maps includeSchema on the trace-parameter query", async () => {
        let params: Record<string, unknown> | undefined;
        mockAxios.onGet(`${projectPath}/trace/parameters/5`).reply((config) => {
          params = config.params;
          return [200, { name: "premium", description: "computed premium", schema: {} }];
        });

        await client.getTraceParameter(projectId, 5, undefined, true);
        expect(params).toEqual({ includeSchema: true });
      });
    });

    describe("watches", () => {
      it("PUTs /trace/watches with the cells wrapped in a WatchesRequest body", async () => {
        let body: unknown;
        mockAxios.onPut(`${projectPath}/trace/watches`).reply((config) => {
          body = config.data;
          return [204];
        });

        await client.setTraceWatches(projectId, ["$VehiclePriceFactor", "$AgeFactor"]);
        expect(JSON.parse(body as string)).toEqual({ cells: ["$VehiclePriceFactor", "$AgeFactor"] });
      });

      it("GETs /trace/watch, forwarding the fields projection, and returns the collected series", async () => {
        let fields: string | undefined;
        const watch: Types.WatchView = {
          series: [
            {
              name: "$VehiclePriceFactor",
              table: "VehiclePremiumCalculation",
              points: [
                { instance: 0, value: { name: "$VehiclePriceFactor", description: "Double", value: 1.0 } },
                { instance: 1, ref: "R7C1", value: { name: "$VehiclePriceFactor", description: "Double", value: 83.372 } },
              ],
            },
          ],
        };
        mockAxios.onGet(`${projectPath}/trace/watch`).reply((config) => {
          fields = config.params?.fields;
          return [200, watch];
        });

        const result = await client.getTraceWatch(projectId, "series(points(value(value)))");
        expect(result.series[0].points[1].value?.value).toBe(83.372);
        expect(fields).toBe("series(points(value(value)))");

        await client.getTraceWatch(projectId);
        expect(fields).toBeUndefined();
      });

      it("maps includeSchema on the trace-watch query", async () => {
        let params: Record<string, unknown> | undefined;
        mockAxios.onGet(`${projectPath}/trace/watch`).reply((config) => {
          params = config.params;
          return [200, { series: [] }];
        });

        await client.getTraceWatch(projectId, undefined, true);
        expect(params).toEqual({ includeSchema: true });
      });
    });

    describe("stopTrace", () => {
      it("DELETEs /trace", async () => {
        mockAxios.onDelete(`${projectPath}/trace`).reply(204);

        await expect(client.stopTrace(projectId)).resolves.toBeUndefined();
        expect(mockAxios.history.delete).toHaveLength(1);
        expect(mockAxios.history.delete[0].url).toBe(`${projectPath}/trace`);
      });
    });
  });
});
