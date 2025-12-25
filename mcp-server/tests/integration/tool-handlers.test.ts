/**
 * Integration tests for MCP Tool Handlers
 * Tests tool execution through the MCP server with real OpenL client
 */

import { describe, it, expect, beforeAll, afterEach } from "@jest/globals";
import MockAdapter from "axios-mock-adapter";
import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { OpenLClient } from "../../src/client.js";
import { executeTool, registerAllTools } from "../../src/tool-handlers.js";
import type { OpenLConfig, Project, Repository, Table } from "../../src/types.js";

describe("Tool Handler Integration Tests", () => {
  let client: OpenLClient;
  let mockAxios: MockAdapter;
  let server: Server;

  beforeAll(() => {
    const config: OpenLConfig = {
      baseUrl: "http://localhost:8080/webstudio/rest",
      username: "admin",
      password: "admin",
    };

    client = new OpenLClient(config);
    // @ts-ignore - Access private axiosInstance for mocking
    mockAxios = new MockAdapter(client.axiosInstance);

    // Create a mock server instance for tool registration
    server = new Server(
      {
        name: "test-server",
        version: "1.0.0",
      },
      {
        capabilities: {},
      }
    );

    // Register all tools before running tests
    registerAllTools(server, client);
  });

  afterEach(() => {
    mockAxios.reset();
  });

  describe("Repository Tools", () => {
    it("should execute openl_list_repositories", async () => {
      const mockRepos: Repository[] = [
        { id: "design", name: "Design Repository" },
        { id: "production", name: "Production Repository" },
      ];

      mockAxios.onGet("/repos").reply(200, mockRepos);

      const result = await executeTool("openl_list_repositories", {}, client);

      expect(result).toHaveProperty("content");
      expect(Array.isArray(result.content)).toBe(true);
      expect(result.content[0].type).toBe("text");
    });

    it("should execute openl_list_branches", async () => {
      mockAxios.onGet("/repos/design/branches").reply(200, ["main", "development"]);

      const result = await executeTool("openl_list_branches", {
        repository: "design",
      }, client);

      expect(result.content[0].type).toBe("text");
      const text = result.content[0].text;
      expect(text).toContain("main");
      expect(text).toContain("development");
    });
  });

  describe("Project Tools", () => {
    it("should execute openl_list_projects", async () => {
      const mockProjects: Partial<Project>[] = [
        {
          projectId: "design-project1",
          projectName: "Project 1",
          repository: "design",
          status: "OPENED",
        },
      ];

      mockAxios.onGet("/projects", { params: { repository: "design" } }).reply(200, mockProjects);

      const result = await executeTool("openl_list_projects", {
        repository: "design",
      }, client);

      expect(result.content[0].type).toBe("text");
      expect(result.content[0].text).toContain("Project 1");
    });

    it("should execute openl_get_project", async () => {
      const mockProject: Partial<Project> = {
        projectId: "design-project1",
        projectName: "project1",
        repository: "design",
        status: "OPENED",
      };

      // getProject uses buildProjectPath which converts to base64 and uses /projects/{base64Id}
      mockAxios.onGet(/\/projects\/.*/).reply(200, mockProject);

      const result = await executeTool("openl_get_project", {
        projectId: "design-project1",
      }, client);

      expect(result.content[0].type).toBe("text");
      expect(result.content[0].text).toContain("project1");
    });

    it("should execute openl_update_project_status", async () => {
      // update_project_status uses buildProjectPath which converts to base64
      mockAxios.onPut(/\/projects\/.*/).reply(200, {
        projectId: "design-project1",
        status: "CLOSED",
      });

      const result = await executeTool("openl_update_project_status", {
        projectId: "design-project1",
        status: "CLOSED",
      }, client);

      expect(result.content[0].type).toBe("text");
      expect(result.content[0].text).toContain("CLOSED");
    });
  });

  describe("Table Tools", () => {
    it("should execute openl_list_tables", async () => {
      const mockTables: Partial<Table>[] = [
        {
          id: "calculatePremium_1234",
          name: "calculatePremium",
          tableType: "SimpleRules",
        },
      ];

      // list_tables uses buildProjectPath
      mockAxios.onGet(/\/projects\/.*\/tables/).reply(200, mockTables);

      const result = await executeTool("openl_list_tables", {
        projectId: "design-project1",
      }, client);

      expect(result.content[0].type).toBe("text");
      expect(result.content[0].text).toContain("calculatePremium");
    });

    it("should execute openl_get_table", async () => {
      const mockTable: Partial<Table> = {
        id: "calculatePremium_1234",
        name: "calculatePremium",
        tableType: "SimpleRules",
      };

      // get_table uses buildProjectPath
      mockAxios.onGet(/\/projects\/.*\/tables\/calculatePremium_1234/).reply(200, mockTable);

      const result = await executeTool("openl_get_table", {
        projectId: "design-project1",
        tableId: "calculatePremium_1234",
      }, client);

      expect(result.content[0].type).toBe("text");
      expect(result.content[0].text).toContain("calculatePremium");
    });

    it("should execute openl_create_rule", async () => {
      // create_rule uses buildProjectPath
      mockAxios.onPost(/\/projects\/.*\/tables/).reply(201, {
        id: "newRule_1234",
        name: "newRule",
        tableType: "SimpleRules",
      });

      const result = await executeTool("openl_create_rule", {
        projectId: "design-project1",
        name: "newRule",
        tableType: "SimpleRules",
        returnType: "double",
      }, client);

      expect(result.content[0].type).toBe("text");
      expect(result.content[0].text).toContain("newRule");
    });
  });

  describe("File Tools", () => {
    it("should execute openl_upload_file", async () => {
      // upload_file uses buildProjectPath
      mockAxios.onPost(/\/projects\/.*\/files\/Rules\.xlsx/).reply(200, {
        success: true,
      });

      const fileContent = Buffer.from("test content").toString("base64");

      const result = await executeTool("openl_upload_file", {
        projectId: "design-project1",
        fileName: "Rules.xlsx",
        fileContent,
      }, client);

      expect(result.content[0].type).toBe("text");
      expect(result.content[0].text).toContain("success");
    });

    it("should execute openl_download_file", async () => {
      const fileBuffer = Buffer.from("test file content");
      // download_file uses buildProjectPath
      mockAxios.onGet(/\/projects\/.*\/files\/Rules\.xlsx/).reply(200, fileBuffer);

      const result = await executeTool("openl_download_file", {
        projectId: "design-project1",
        fileName: "Rules.xlsx",
      }, client);

      expect(result.content[0].type).toBe("text");
      // Response should contain base64 encoded content
      expect(result.content[0].text).toBeDefined();
    });
  });

  describe("Response Format Variants", () => {
    it("should support json response format", async () => {
      mockAxios.onGet("/repos").reply(200, [
        { id: "design", name: "Design" },
      ]);

      const result = await executeTool("openl_list_repositories", {
        response_format: "json",
      }, client);

      const text = result.content[0].text;
      expect(() => JSON.parse(text)).not.toThrow();

      const data = JSON.parse(text);
      expect(data).toHaveProperty("data");
    });

    it("should support markdown_concise response format", async () => {
      mockAxios.onGet("/projects", { params: { repository: "design" } }).reply(200, [
        { projectId: "design-p1", status: "OPENED" },
        { projectId: "design-p2", status: "CLOSED" },
      ]);

      const result = await executeTool("openl_list_projects", {
        repository: "design",
        response_format: "markdown_concise",
      }, client);

      const text = result.content[0].text;
      expect(text).toContain("Found");
      expect(text.length).toBeLessThan(500); // Should be concise
    });

    it("should support markdown_detailed response format", async () => {
      mockAxios.onGet("/projects", { params: { repository: "design" } }).reply(200, [
        { projectId: "design-p1", status: "OPENED" },
      ]);

      const result = await executeTool("openl_list_projects", {
        repository: "design",
        response_format: "markdown_detailed",
      }, client);

      const text = result.content[0].text;
      expect(text).toContain("Summary");
      expect(text).toContain("Retrieved");
    });
  });

  describe("Destructive Operation Confirmation", () => {
    it("should require confirmation for openl_revert_version", async () => {
      await expect(
        executeTool("openl_revert_version", {
          projectId: "design-project1",
          targetVersion: "abc123",
          // Missing confirm: true
        }, client)
      ).rejects.toThrow(/confirm/);
    });

    it("should execute openl_revert_version with confirmation", async () => {
      // revert_version needs multiple API calls: get version, validate, revert
      mockAxios.onGet(/\/projects\/.*\/versions\/abc123/).reply(200, {
        version: "abc123",
        content: {},
      });
      mockAxios.onGet(/\/projects\/.*\/validation/).reply(200, {
        valid: true,
        errors: [],
      });
      mockAxios.onPost(/\/projects\/.*\/revert/).reply(200, {
        version: "new-commit",
      });

      const result = await executeTool("openl_revert_version", {
        projectId: "design-project1",
        targetVersion: "abc123",
        confirm: true,
      }, client);

      expect(result.content[0].type).toBe("text");
      expect(result.content[0].text).toContain("success");
    });

    it("should require confirmation for openl_deploy_project", async () => {
      await expect(
        executeTool("openl_deploy_project", {
          projectName: "project1",
          repository: "design",
          deploymentRepository: "production",
          // Missing confirm: true
        }, client)
      ).rejects.toThrow(/confirm/);
    });

    it("should execute openl_deploy_project with confirmation", async () => {
      // deploy_project uses /deployments endpoint
      mockAxios.onPost("/deployments").reply(200, {
        success: true,
        deploymentName: "project1",
      });

      const result = await executeTool("openl_deploy_project", {
        projectName: "project1",
        repository: "design",
        deploymentRepository: "production",
        confirm: true,
      }, client);

      expect(result.content[0].type).toBe("text");
      expect(result.content[0].text).toContain("success");
    });

    it("should require confirmation for discardChanges in openl_update_project_status", async () => {
      // Mock the project fetch that happens before status update
      mockAxios.onGet(/\/projects\/.*/).reply(200, {
        projectId: "design-project1",
        status: "EDITING", // Project has unsaved changes
      });

      await expect(
        executeTool("openl_update_project_status", {
          projectId: "design-project1",
          status: "CLOSED",
          discardChanges: true,
          // Missing confirm: true
        }, client)
      ).rejects.toThrow(/confirm/);
    });
  });

  describe("Pagination", () => {
    it("should support pagination parameters", async () => {
      const mockProjects = Array.from({ length: 100 }, (_, i) => ({
        projectId: `design-p${i}`,
        status: "OPENED",
      }));

      mockAxios.onGet("/projects", { params: { repository: "design" } }).reply(200, mockProjects);

      const result = await executeTool("openl_list_projects", {
        repository: "design",
        limit: 10,
        offset: 0,
        response_format: "json",
      }, client);

      const data = JSON.parse(result.content[0].text);
      expect(data.pagination).toBeDefined();
      expect(data.pagination.limit).toBe(10);
      expect(data.pagination.offset).toBe(0);
      expect(data.pagination.has_more).toBe(true);
      expect(data.pagination.next_offset).toBe(10);
    });

    it("should enforce maximum limit of 200", async () => {
      await expect(
        executeTool("openl_list_projects", {
          repository: "design",
          limit: 300, // Exceeds max
        }, client)
      ).rejects.toThrow(/limit must be <= 200/);
    });

    it("should enforce minimum limit of 1", async () => {
      await expect(
        executeTool("openl_list_projects", {
          repository: "design",
          limit: 0,
        }, client)
      ).rejects.toThrow(/limit must be positive/);
    });
  });

  describe("Error Handling", () => {
    it("should return actionable error for missing projectId", async () => {
      await expect(
        executeTool("openl_get_project", {
          // Missing projectId
        }, client)
      ).rejects.toThrow(/Missing required argument: projectId/);
      await expect(
        executeTool("openl_get_project", {}, client)
      ).rejects.toThrow(/openl_list_projects/);
    });

    it("should return actionable error for invalid response_format", async () => {
      await expect(
        executeTool("openl_list_projects", {
          repository: "design",
          response_format: "xml" as any,
        }, client)
      ).rejects.toThrow(/markdown_concise.*markdown_detailed/);
    });

    it("should handle network errors gracefully", async () => {
      mockAxios.onGet("/repos").networkError();

      await expect(
        executeTool("openl_list_repositories", {}, client)
      ).rejects.toThrow();
    });
  });
});
