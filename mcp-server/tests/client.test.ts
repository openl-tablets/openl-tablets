/**
 * Unit tests for client.ts
 * Tests OpenL API client methods with mocked HTTP responses
 */

import { describe, it, expect, beforeEach, afterEach } from "@jest/globals";
import MockAdapter from "axios-mock-adapter";
import { OpenLClient } from "../src/client.js";
import type { OpenLConfig, Repository, Project, Table } from "../src/types.js";

describe("OpenLClient", () => {
  let client: OpenLClient;
  let mockAxios: MockAdapter;

  beforeEach(() => {
    const config: OpenLConfig = {
      baseUrl: "http://localhost:8080/webstudio/rest",
      username: "admin",
      password: "admin",
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
    it("should create client with basic config", () => {
      const config: OpenLConfig = {
        baseUrl: "http://localhost:8080/webstudio/rest",
      };
      const testClient = new OpenLClient(config);
      expect(testClient.getBaseUrl()).toBe("http://localhost:8080/webstudio/rest");
    });

    it("should set auth method when using basic auth", () => {
      const config: OpenLConfig = {
        baseUrl: "http://localhost:8080",
        username: "admin",
        password: "admin",
      };
      const testClient = new OpenLClient(config);
      expect(testClient.getAuthMethod()).toContain("Basic");
    });

    it("should set auth method when using API key", () => {
      const config: OpenLConfig = {
        baseUrl: "http://localhost:8080",
        apiKey: "test-key",
      };
      const testClient = new OpenLClient(config);
      expect(testClient.getAuthMethod()).toContain("API Key");
    });

    it("should handle custom timeout", () => {
      const config: OpenLConfig = {
        baseUrl: "http://localhost:8080",
        timeout: 60000,
      };
      const testClient = new OpenLClient(config);
      expect(testClient).toBeDefined();
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
  });

  describe("Project Management", () => {
    describe("listProjects", () => {
      it("should fetch list of projects", async () => {
        const mockProjects: Partial<Project>[] = [
          {
            projectId: "design-project1",
            projectName: "Project 1",
            repository: "design",
            status: "OPENED",
          },
          {
            projectId: "design-project2",
            projectName: "Project 2",
            repository: "design",
            status: "CLOSED",
          },
        ];

        mockAxios.onGet("/design").reply(200, mockProjects);

        const result = await client.listProjects({ repository: "design" });
        expect(result.length).toBe(2);
        expect(result[0].projectId).toBe("design-project1");
      });

      it("should filter by status", async () => {
        const openProjects: Partial<Project>[] = [
          { projectId: "design-p1", status: "OPENED" },
        ];

        mockAxios.onGet("/design", { params: { status: "OPENED" } }).reply(200, openProjects);

        const result = await client.listProjects({
          repository: "design",
          status: "OPENED"
        });
        expect(result.length).toBe(1);
      });

      it("should filter by tag", async () => {
        mockAxios.onGet(/\/design.*tag=v1.0/).reply(200, []);

        await client.listProjects({
          repository: "design",
          tag: "v1.0"
        });

        expect(mockAxios.history.get.length).toBe(1);
      });
    });

    describe("getProject", () => {
      it("should fetch project by ID", async () => {
        const mockProject: Partial<Project> = {
          projectId: "design-project1",
          projectName: "project1",
          repository: "design",
          status: "OPENED",
        };

        mockAxios.onGet("/design/project1").reply(200, mockProject);

        const result = await client.getProject("design-project1");
        expect(result.projectId).toBe("design-project1");
      });

      it("should parse projectId with hyphen separator", async () => {
        mockAxios.onGet("/design/InsuranceRules").reply(200, {});

        await client.getProject("design-InsuranceRules");
        expect(mockAxios.history.get[0].url).toBe("/design/InsuranceRules");
      });

      it("should handle project not found", async () => {
        mockAxios.onGet("/design/nonexistent").reply(404);

        await expect(client.getProject("design-nonexistent")).rejects.toThrow();
      });

      it("should throw error for invalid projectId format", async () => {
        await expect(client.getProject("invalid")).rejects.toThrow(/Invalid project ID format/);
      });
    });

    describe("updateProjectStatus", () => {
      it("should update project status", async () => {
        const updatedProject: Partial<Project> = {
          projectId: "design-project1",
          status: "CLOSED",
        };

        mockAxios.onPut("/design/project1").reply(200, updatedProject);

        const result = await client.updateProjectStatus("design-project1", {
          status: "CLOSED",
        });
        expect(result.status).toBe("CLOSED");
      });

      it("should send comment when provided", async () => {
        mockAxios.onPut("/design/project1").reply((config) => {
          const data = JSON.parse(config.data);
          expect(data.comment).toBe("Closing project");
          return [200, {}];
        });

        await client.updateProjectStatus("design-project1", {
          status: "CLOSED",
          comment: "Closing project",
        });
      });

      it("should send discardChanges flag", async () => {
        mockAxios.onPut("/design/project1").reply((config) => {
          const data = JSON.parse(config.data);
          expect(data.discardChanges).toBe(true);
          return [200, {}];
        });

        await client.updateProjectStatus("design-project1", {
          status: "CLOSED",
          discardChanges: true,
        });
      });

      it("should switch branches", async () => {
        mockAxios.onPut("/design/project1").reply((config) => {
          const data = JSON.parse(config.data);
          expect(data.branch).toBe("development");
          return [200, {}];
        });

        await client.updateProjectStatus("design-project1", {
          branch: "development",
        });
      });
    });
  });

  describe("Table Management", () => {
    describe("listTables", () => {
      it("should fetch list of tables", async () => {
        const mockTables: Partial<Table>[] = [
          {
            id: "calculatePremium_1234",
            name: "calculatePremium",
            tableType: "SimpleRules",
          },
          {
            id: "validatePolicy_5678",
            name: "validatePolicy",
            tableType: "Spreadsheet",
          },
        ];

        mockAxios.onGet("/design/project1/tables").reply(200, mockTables);

        const result = await client.listTables("design-project1");
        expect(result.length).toBe(2);
        expect(result[0].name).toBe("calculatePremium");
      });

      it("should filter by table type", async () => {
        mockAxios.onGet("/design/project1/tables", {
          params: { tableType: "SimpleRules" }
        }).reply(200, []);

        await client.listTables("design-project1", { tableType: "SimpleRules" });
        expect(mockAxios.history.get.length).toBe(1);
      });

      it("should filter by name pattern", async () => {
        mockAxios.onGet(/tables.*name=calculate/).reply(200, []);

        await client.listTables("design-project1", { name: "calculate*" });
        expect(mockAxios.history.get.length).toBe(1);
      });

      it("should filter by file", async () => {
        mockAxios.onGet(/tables.*file=Rules.xlsx/).reply(200, []);

        await client.listTables("design-project1", { file: "Rules.xlsx" });
        expect(mockAxios.history.get.length).toBe(1);
      });
    });

    describe("getTable", () => {
      it("should fetch table by ID", async () => {
        const mockTable: Partial<Table> = {
          id: "calculatePremium_1234",
          name: "calculatePremium",
          tableType: "SimpleRules",
        };

        mockAxios.onGet("/design/project1/tables/calculatePremium_1234").reply(200, mockTable);

        const result = await client.getTable("design-project1", "calculatePremium_1234");
        expect(result.id).toBe("calculatePremium_1234");
      });

      it("should URL-encode table ID", async () => {
        mockAxios.onGet(/tables\/.*/).reply((config) => {
          expect(config.url).toContain("table%20id");
          return [200, {}];
        });

        await client.getTable("design-project1", "table id");
      });

      it("should handle table not found", async () => {
        mockAxios.onGet("/design/project1/tables/nonexistent").reply(404);

        await expect(
          client.getTable("design-project1", "nonexistent")
        ).rejects.toThrow();
      });
    });

    describe("updateTable", () => {
      it("should update table with new data", async () => {
        const tableView = {
          id: "calculatePremium_1234",
          name: "calculatePremium",
          rules: [
            { driverType: "SAFE", premium: 1000 },
          ],
        };

        mockAxios.onPut("/design/project1/tables/calculatePremium_1234").reply(200, tableView);

        const result = await client.updateTable("design-project1", "calculatePremium_1234", {
          view: tableView,
        });
        expect(result.id).toBe("calculatePremium_1234");
      });

      it("should send comment when provided", async () => {
        mockAxios.onPut("/design/project1/tables/table1").reply((config) => {
          const data = JSON.parse(config.data);
          expect(data.comment).toBe("Updated rates");
          return [200, {}];
        });

        await client.updateTable("design-project1", "table1", {
          view: {},
          comment: "Updated rates",
        });
      });
    });

    describe("createRule", () => {
      it("should create new rule table", async () => {
        const ruleSpec = {
          name: "calculatePremium",
          tableType: "SimpleRules" as const,
          returnType: "double",
          parameters: [
            { type: "String", name: "driverType" },
            { type: "int", name: "age" },
          ],
        };

        mockAxios.onPost("/design/project1/tables").reply(201, {
          id: "calculatePremium_1234",
          ...ruleSpec,
        });

        const result = await client.createRule("design-project1", ruleSpec);
        expect(result.id).toBe("calculatePremium_1234");
      });

      it("should include file path when provided", async () => {
        mockAxios.onPost("/design/project1/tables").reply((config) => {
          const data = JSON.parse(config.data);
          expect(data.file).toBe("rules/Insurance.xlsx");
          return [201, {}];
        });

        await client.createRule("design-project1", {
          name: "test",
          tableType: "SimpleRules",
          file: "rules/Insurance.xlsx",
        });
      });

      it("should include dimension properties", async () => {
        mockAxios.onPost("/design/project1/tables").reply((config) => {
          const data = JSON.parse(config.data);
          expect(data.properties).toEqual({ state: "CA", lob: "Auto" });
          return [201, {}];
        });

        await client.createRule("design-project1", {
          name: "test",
          tableType: "SimpleRules",
          properties: { state: "CA", lob: "Auto" },
        });
      });
    });
  });

  describe("File Operations", () => {
    describe("uploadFile", () => {
      it("should upload file with base64 content", async () => {
        const buffer = Buffer.from("test file content");
        mockAxios.onPost("/design/project1/files/Rules.xlsx").reply(200, {
          success: true,
        });

        const result = await client.uploadFile("design-project1", "Rules.xlsx", buffer);
        expect(result.success).toBe(true);
      });

      it("should URL-encode file name", async () => {
        mockAxios.onPost(/files\/.*/).reply((config) => {
          expect(config.url).toContain("My%20Rules.xlsx");
          return [200, {}];
        });

        const buffer = Buffer.from("test");
        await client.uploadFile("design-project1", "My Rules.xlsx", buffer);
      });

      it("should include comment when provided", async () => {
        mockAxios.onPost("/design/project1/files/Rules.xlsx").reply((config) => {
          const data = JSON.parse(config.data);
          expect(data.comment).toBe("Updated rates");
          return [200, {}];
        });

        const buffer = Buffer.from("test");
        await client.uploadFile("design-project1", "Rules.xlsx", buffer, "Updated rates");
      });

      it("should handle upload errors", async () => {
        mockAxios.onPost("/design/project1/files/Rules.xlsx").reply(500);

        const buffer = Buffer.from("test");
        await expect(
          client.uploadFile("design-project1", "Rules.xlsx", buffer)
        ).rejects.toThrow();
      });
    });

    describe("downloadFile", () => {
      it("should download file content", async () => {
        const fileContent = Buffer.from("test file content");
        mockAxios.onGet("/design/project1/files/Rules.xlsx").reply(200, fileContent);

        const result = await client.downloadFile("design-project1", "Rules.xlsx");
        expect(result).toEqual(fileContent);
      });

      it("should download specific version", async () => {
        mockAxios.onGet(/files\/Rules.xlsx.*version=abc123/).reply(200, Buffer.from("old content"));

        await client.downloadFile("design-project1", "Rules.xlsx", "abc123");
        expect(mockAxios.history.get.length).toBe(1);
      });

      it("should handle file not found", async () => {
        mockAxios.onGet("/design/project1/files/NonExistent.xlsx").reply(404);

        await expect(
          client.downloadFile("design-project1", "NonExistent.xlsx")
        ).rejects.toThrow();
      });
    });
  });

  describe("Deployment", () => {
    describe("deployProject", () => {
      it("should deploy project to production repository", async () => {
        mockAxios.onPost("/design/project1/deploy").reply(200, {
          success: true,
          deploymentName: "project1",
        });

        const result = await client.deployProject({
          projectId: "design-project1",
          deploymentName: "project1",
          productionRepositoryId: "production-deploy",
        });

        expect(result.success).toBe(true);
      });

      it("should include version when provided", async () => {
        mockAxios.onPost("/design/project1/deploy").reply((config) => {
          const data = JSON.parse(config.data);
          expect(data.version).toBe("abc123");
          return [200, {}];
        });

        await client.deployProject({
          projectId: "design-project1",
          deploymentName: "project1",
          productionRepositoryId: "production-deploy",
          version: "abc123",
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
    });
  });

  describe("Version History", () => {
    describe("getFileHistory", () => {
      it("should fetch file commit history", async () => {
        const mockHistory = [
          { commitHash: "abc123", author: "user1", timestamp: "2024-01-01" },
          { commitHash: "def456", author: "user2", timestamp: "2024-01-02" },
        ];

        mockAxios.onGet("/design/project1/files/Rules.xlsx/history").reply(200, mockHistory);

        const result = await client.getFileHistory({
          projectId: "design-project1",
          filePath: "Rules.xlsx",
        });

        expect(result.length).toBe(2);
      });

      it("should include pagination parameters", async () => {
        mockAxios.onGet(/history.*limit=20.*offset=10/).reply(200, []);

        await client.getFileHistory({
          projectId: "design-project1",
          filePath: "Rules.xlsx",
          limit: 20,
          offset: 10,
        });

        expect(mockAxios.history.get.length).toBe(1);
      });
    });

    describe("getProjectHistory", () => {
      it("should fetch project commit history", async () => {
        const mockHistory = [
          { commitHash: "abc123", files: ["Rules.xlsx"] },
          { commitHash: "def456", files: ["Data.xlsx"] },
        ];

        mockAxios.onGet("/design/project1/history").reply(200, mockHistory);

        const result = await client.getProjectHistory({
          projectId: "design-project1",
        });

        expect(result.length).toBe(2);
      });

      it("should filter by branch", async () => {
        mockAxios.onGet(/history.*branch=development/).reply(200, []);

        await client.getProjectHistory({
          projectId: "design-project1",
          branch: "development",
        });

        expect(mockAxios.history.get.length).toBe(1);
      });
    });

    describe("revertVersion", () => {
      it("should revert project to specific version", async () => {
        mockAxios.onPost("/design/project1/revert").reply(200, {
          success: true,
          commitHash: "new-commit",
        });

        const result = await client.revertVersion({
          projectId: "design-project1",
          targetVersion: "abc123",
        });

        expect(result.success).toBe(true);
      });

      it("should include comment when provided", async () => {
        mockAxios.onPost("/design/project1/revert").reply((config) => {
          const data = JSON.parse(config.data);
          expect(data.comment).toBe("Reverting bad changes");
          return [200, {}];
        });

        await client.revertVersion({
          projectId: "design-project1",
          targetVersion: "abc123",
          comment: "Reverting bad changes",
        });
      });
    });
  });

  describe("Rule Execution", () => {
    describe("executeRule", () => {
      it("should execute rule with input data", async () => {
        const inputData = {
          driverType: "SAFE",
          age: 30,
        };

        mockAxios.onPost("/design/project1/execute/calculatePremium").reply(200, {
          result: 1000.0,
        });

        const result = await client.executeRule({
          projectId: "design-project1",
          ruleName: "calculatePremium",
          inputData,
        });

        expect(result.result).toBe(1000.0);
      });

      it("should handle execution errors", async () => {
        mockAxios.onPost("/design/project1/execute/badRule").reply(400, {
          error: "Invalid parameters",
        });

        await expect(
          client.executeRule({
            projectId: "design-project1",
            ruleName: "badRule",
            inputData: {},
          })
        ).rejects.toThrow();
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

  describe("URL Encoding", () => {
    it("should encode special characters in repository names", async () => {
      mockAxios.onGet(/repos\/.*\/branches/).reply((config) => {
        expect(config.url).toMatch(/my%20special%20repo/);
        return [200, []];
      });

      await client.listBranches("my special repo");
    });

    it("should encode special characters in project names", async () => {
      mockAxios.onGet(/design\/.*/).reply((config) => {
        expect(config.url).toMatch(/Example%201%20-%20Bank%20Rating/);
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
});
