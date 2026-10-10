/**
 * Integration tests for MCP Tool Handlers
 * Tests tool execution through the MCP server with real OpenL client
 */

import { describe, it, expect, beforeAll, beforeEach, afterEach, afterAll } from "@jest/globals";
import MockAdapter from "axios-mock-adapter";
import { OpenLClient } from "../../src/client.js";
import { executeTool, getAllTools, registerAllTools } from "../../src/handlers/index.js";
import { SERVER_INFO } from "../../src/constants.js";
import { versionInfo } from "../../src/build-info.js";
import type { OpenLConfig, ProjectStatusView, ProjectViewModel, RepositoryInfo, SummaryTableView } from "../../src/types.js";
import * as Types from "../../src/types.js";
import { mockRepositories, mockDeployments } from "../mocks/openl-api-mocks.js";
import { FIXTURE_IDS, writeGuidesFixture } from "../mocks/guides-bundle-fixture.js";

// Shared fixture for the tests below that address a project by its colon-form id.
const projectId = "design:insurance-rules:hash123";
const encodeProjectPath = (id: string): string => encodeURIComponent(id);

describe("Tool Handler Integration Tests", () => {
  let client: OpenLClient;
  let mockAxios: MockAdapter;

  beforeAll(() => {
    const config: OpenLConfig = {
      baseUrl: "http://localhost:8080",
      personalAccessToken: "openl_pat_test",
    };

    client = new OpenLClient(config);
    // @ts-ignore - Access private axiosInstance for mocking
    mockAxios = new MockAdapter(client.axiosInstance);

    // Register all tools before running tests
    registerAllTools();
  });

  afterEach(() => {
    mockAxios.reset();
  });

  it("getAllTools() exposes every tool under a bare name (the openl_ prefix is a wire-only concern)", () => {
    const names = getAllTools().map((t) => t.name);
    expect(names.length).toBeGreaterThan(0);
    // The registry never carries the namespace prefix — it is added/stripped only
    // at the MCP protocol boundary (see tests/constants.test.ts).
    expect(names.some((n) => n.startsWith("openl_"))).toBe(false);
    // The bare names are exactly what executeTool / the CLI dispatch on.
    expect(names).toContain("list_repositories");
  });

  describe("Repository Tools", () => {
    it("should execute openl_list_repositories", async () => {
      const mockRepos: RepositoryInfo[] = [
        { id: "design", name: "Design Repository", aclId: "acl-design" },
        { id: "production", name: "Production Repository", aclId: "acl-production" },
      ];

      mockAxios.onGet("/repos").reply(200, mockRepos);

      const result = await executeTool("list_repositories", {}, client);

      expect(result).toHaveProperty("content");
      expect(Array.isArray(result.content)).toBe(true);
      expect(result.content[0].type).toBe("text");
    });

    it("should execute openl_list_branches", async () => {
      // Mock repositories list for getRepositoryIdByName
      const mockRepos: RepositoryInfo[] = [
        { id: "design", name: "Design Repository", aclId: "acl-design" },
      ];
      mockAxios.onGet("/repos").reply(200, mockRepos);

      // Mock branches API call (uses repository ID)
      mockAxios.onGet("/repos/design/branches").reply(200, ["main", "development"]);

      const result = await executeTool("list_branches", {
        repository: "Design Repository", // Use repository name, not ID
      }, client);

      expect(result.content[0].type).toBe("text");
      const text = result.content[0].text;
      expect(text).toContain("main");
      expect(text).toContain("development");
    });

    it("uses the current revision total to derive more pages", async () => {
      mockAxios.onGet(`/projects/${encodeProjectPath(projectId)}/history`, {
        params: { techRevs: false, page: 0, size: 2 },
      }).reply(200, {
        content: [
          {
            revisionNo: "abc123",
            createdAt: "2026-01-01T00:00:00Z",
            fullComment: "First",
            deleted: false,
            technicalRevision: false,
          },
          {
            revisionNo: "def456",
            createdAt: "2026-01-02T00:00:00Z",
            fullComment: "Second",
            deleted: false,
            technicalRevision: false,
          },
        ],
        pageNumber: 0,
        pageSize: 2,
        numberOfElements: 2,
        total: 4,
      });

      const result = await executeTool("repository_project_revisions", {
        projectId,
        page: 0,
        size: 2,
        response_format: "json",
      }, client);

      const response = JSON.parse(result.content[0].text);
      expect(response.pagination).toMatchObject({
        limit: 2,
        offset: 0,
        has_more: true,
        next_offset: 2,
      });
      expect(response.pagination.total_count).toBe(4);
    });

    it("preserves a non-page-aligned revision offset in response metadata", async () => {
      mockAxios.onGet(`/projects/${encodeProjectPath(projectId)}/history`, {
        params: { techRevs: false, offset: 25, size: 50 },
      }).reply(200, {
        content: [],
        pageNumber: 0,
        pageSize: 50,
        numberOfElements: 0,
        total: 25,
      });

      const result = await executeTool("repository_project_revisions", {
        projectId,
        offset: 25,
        size: 50,
        response_format: "json",
      }, client);

      const response = JSON.parse(result.content[0].text);
      expect(response.pagination).toMatchObject({
        limit: 50,
        offset: 25,
        total_count: 25,
        has_more: false,
      });
    });
  });

  describe("Project Tools", () => {
    it("should execute openl_list_projects", async () => {
      // Mock repositories list for getRepositoryIdByName
      const mockRepos: RepositoryInfo[] = [
        { id: "design", name: "Design Repository", aclId: "acl-design" },
      ];
      mockAxios.onGet("/repos").reply(200, mockRepos);

      const mockProjects: Partial<ProjectViewModel>[] = [
        {
          id: "design:Project 1:hash123",
          name: "Project 1",
          repository: "design",
          status: "OPENED",
          path: "Project 1",
          modifiedBy: "admin",
          modifiedAt: "2024-01-01T00:00:00Z",
        },
      ];

      mockAxios.onGet("/projects", { params: { repository: "design", offset: 0, size: 50 } }).reply(200, mockProjects);

      const result = await executeTool("list_projects", {
        repository: "Design Repository", // Use repository name, not ID
      }, client);

      expect(result.content[0].type).toBe("text");
      expect(result.content[0].text).toContain("Project 1");
    });

    it("openl_list_projects returns requested summary and status expansions", async () => {
      mockAxios.onGet("/projects", {
        params: { include: ["summary", "status"], offset: 0, size: 50 },
      }).reply(200, {
        content: [],
        pageNumber: 0,
        pageSize: 50,
        numberOfElements: 0,
        total: 0,
        repositoryCounts: [{ repository: "design", count: 3 }],
        statusCounts: { OPENED: 2, CLOSED: 1 },
        statuses: ["OPENED", "CLOSED"],
        tagCounts: [{ tag: "release", count: 1 }],
      });

      const result = await executeTool("list_projects", {
        include: ["summary", "status"],
        response_format: "json",
      }, client);
      const response = JSON.parse(result.content[0].text);

      expect(response.repositoryCounts).toEqual([{ repository: "design", count: 3 }]);
      expect(response.statusCounts).toEqual({ OPENED: 2, CLOSED: 1 });
      expect(response.statuses).toEqual(["OPENED", "CLOSED"]);
      expect(response.tagCounts).toEqual([{ tag: "release", count: 1 }]);
    });

    it("should execute openl_get_project", async () => {
      // getProject normalizes projectId for request path
      // "design-project1" is used as project ID path segment
      const projectIdForPath = "design-project1";
      const mockProject: Partial<ProjectViewModel> = {
        id: "design:project1:hash123",
        name: "project1",
        repository: "design",
        status: "OPENED",
        path: "project1",
        modifiedBy: "admin",
        modifiedAt: "2024-01-01T00:00:00Z",
      };

      // getProject uses buildProjectPath and calls /projects/{projectIdPath}
      mockAxios.onGet(`/projects/${encodeURIComponent(projectIdForPath)}`).reply(200, mockProject);

      const result = await executeTool("get_project", {
        projectId: "design-project1",
      }, client);

      expect(result.content[0].type).toBe("text");
      expect(result.content[0].text).toContain("project1");
    });

  });

  describe("Table Tools", () => {
    it("should execute openl_list_tables", async () => {
      const mockTables: Partial<SummaryTableView>[] = [
        {
          id: "calculatePremium_1234",
          name: "calculatePremium",
          tableType: "SimpleRules",
          kind: "Rules",
          signature: "double calculatePremium(int age, double amount)",
          returnType: "double",
          file: "Rules.xlsx",
          pos: "A1",
          properties: {
            category: "Premium Calculation",
            version: "1.0",
          },
        },
      ];

      // list_tables uses buildProjectPath
      mockAxios.onGet(/\/projects\/.*\/tables/).reply(200, mockTables);

      const result = await executeTool("list_tables", {
        projectId: "design-project1",
        response_format: "markdown",
      }, client);

      expect(result.content[0].type).toBe("text");
      const text = result.content[0].text;
      expect(text).toContain("calculatePremium");
      // Verify new columns are included in markdown output
      expect(text).toContain("Kind");
      expect(text).toContain("Signature");
      expect(text).toContain("Return Type");
      expect(text).toContain("Properties");
      expect(text).toContain("Rules"); // kind value
      expect(text).toContain("double calculatePremium"); // signature value
      expect(text).toContain("double"); // returnType value
      expect(text).toContain("category"); // properties keys
    });

    it("should execute openl_get_table", async () => {
      const mockTable = {
        id: "calculatePremium_1234",
        name: "calculatePremium",
        tableType: "RawSource",
        kind: "Rules",
        source: [[{ value: "Rules void calculatePremium()" }]],
      };

      // get_table uses buildProjectPath
      let queryParams: Record<string, unknown> | undefined;
      mockAxios.onGet(/\/projects\/.*\/tables\/calculatePremium_1234/).reply((config) => {
        queryParams = config.params as Record<string, unknown>;
        return [200, mockTable];
      });

      const result = await executeTool("get_table", {
        projectId: "design-project1",
        tableId: "calculatePremium_1234",
        styles: false,
      }, client);

      expect(queryParams).toEqual({ raw: true });
      expect(result.content[0].type).toBe("text");
      expect(result.content[0].text).toContain("calculatePremium");
    });

    it("openl_get_table forwards startRow/maxRows/styles to the raw view query and returns the slice", async () => {
      let queryParams: Record<string, unknown> | undefined;
      mockAxios.onGet(/\/projects\/.*\/tables\/calculatePremium_1234/).reply((config) => {
        queryParams = config.params as Record<string, unknown>;
        return [200, {
          id: "calculatePremium_1234",
          name: "calculatePremium",
          tableType: "RawSource",
          source: [[{ cell: "A3", value: "Age", style: { bold: true, background: "#ccccff" } }]],
          totalRows: 120,
        }];
      });

      const result = await executeTool("get_table", {
        projectId: "design-project1",
        tableId: "calculatePremium_1234",
        startRow: 2,
        maxRows: 1,
        styles: true,
        response_format: "json",
      }, client);

      expect(queryParams).toEqual({ raw: true, startRow: 2, maxRows: 1, styles: true });
      const text = result.content[0].text as string;
      expect(text).toContain("\"totalRows\": 120");
      expect(text).toContain("\"background\": \"#ccccff\"");
    });

    it("openl_get_table rejects the removed raw switch before any request is sent", async () => {
      let called = false;
      mockAxios.onGet(/\/projects\/.*\/tables\/.*/).reply(() => {
        called = true;
        return [200, {}];
      });

      await expect(
        executeTool("get_table", {
          projectId: "design-project1",
          tableId: "calculatePremium_1234",
          raw: false,
        }, client)
      ).rejects.toThrow(/Unrecognized key.*raw/s);
      expect(called).toBe(false);
    });

    it("openl_create_project_table sends a complete RawSource table", async () => {
      let body: Record<string, any> = {};
      mockAxios.onPost(/\/projects\/.*\/tables/).reply((config) => {
        body = JSON.parse(config.data);
        return [201, { id: "t1", name: "LoanApplication", tableType: "RawSource", file: "Main.xlsx" }];
      });

      await executeTool("create_project_table", {
        projectId: "p1",
        moduleName: "Main",
        table: { tableType: "RawSource", kind: "Datatype", name: "LoanApplication", source: [[{ value: "Datatype LoanApplication" }]] },
      }, client);

      expect(body.table).toEqual(expect.objectContaining({ tableType: "RawSource", source: expect.any(Array) }));
    });

    it("openl_create_project_table rejects an unknown tableType with an actionable error (no request sent)", async () => {
      let called = false;
      mockAxios.onPost(/\/projects\/.*\/tables/).reply(() => {
        called = true;
        return [201, {}];
      });

      await expect(
        executeTool("create_project_table", {
          projectId: "p1",
          moduleName: "Main",
          table: { tableType: "frobnicate", name: "X" },
        }, client)
      ).rejects.toThrow(/must be exactly "RawSource"/);
      expect(called).toBe(false);
    });

    it("openl_create_project_table rejects a typed table DTO with raw-only guidance", async () => {
      let called = false;
      mockAxios.onPost(/\/projects\/.*\/tables/).reply(() => {
        called = true;
        return [201, {}];
      });

      await expect(
        executeTool("create_project_table", {
          projectId: "p1",
          moduleName: "Main",
          table: { tableType: "SimpleRules", name: "CreditCategory", signature: "String CreditCategory(Integer creditScore)", rules: [] },
        }, client)
      ).rejects.toThrow(/Typed table DTOs are intentionally unsupported/);
      expect(called).toBe(false);
    });

    it("openl_create_project_table rejects a missing tableType", async () => {
      await expect(
        executeTool("create_project_table", {
          projectId: "p1",
          moduleName: "Main",
          table: { name: "X", fields: [] },
        }, client)
      ).rejects.toThrow(/tableType must be exactly "RawSource"/);
    });

    it("openl_create_project_table rejects a missing or blank name before sending", async () => {
      let called = false;
      mockAxios.onPost(/\/projects\/.*\/tables/).reply(() => {
        called = true;
        return [201, {}];
      });

      for (const table of [
        { tableType: "RawSource", source: [] },
        { tableType: "RawSource", name: "   ", source: [] },
      ]) {
        await expect(executeTool("create_project_table", {
          projectId: "p1",
          moduleName: "Main",
          table,
        }, client)).rejects.toThrow(/Invalid arguments for create_project_table/);
      }
      expect(called).toBe(false);
    });

    // --- Request validation for the structured-payload table tools (EPBDS-16110/16112) ---

    it("openl_append_table rejects appendData with no tableType discriminator (no request sent)", async () => {
      let called = false;
      mockAxios.onPost(/\/lines/).reply(() => {
        called = true;
        return [200];
      });

      // Agent bug #1: a payload that omits the required tableType discriminator.
      await expect(
        executeTool("append_table", {
          projectId: "p1",
          tableId: "t1",
          appendData: { rules: [{ "Commission Type": "UDI", "Partner Code": "CIDP_CL_FB" }] },
        }, client)
      ).rejects.toThrow(/tableType must be exactly "RawSource"/);
      expect(called).toBe(false);
    });

    it("openl_append_table accepts appendData sent as a JSON string and forwards a real object", async () => {
      mockAxios.onGet(/\/tables\/t1$/).reply(200, {
        id: "t1", name: "MyData", tableType: "RawSource", kind: "Data", source: [[{}, {}, {}]],
      });
      let postBody: Record<string, any> = {};
      mockAxios.onPost(/\/tables\/t1\/lines/).reply((config) => {
        postBody = JSON.parse(config.data);
        return [200];
      });

      // Agent bug #2: the whole payload arrives as a JSON *string*, not an object.
      const result = await executeTool("append_table", {
        projectId: "p1",
        tableId: "t1",
        appendData: '{"tableType":"RawSource","rows":[[{"value":2035},{"value":"01/01/2035"},{"value":1000}]]}',
      }, client);

      expect(result.content[0].text).toContain("Successfully appended");
      // The stringified payload reached the backend as a parsed object, not a string literal.
      expect(postBody.tableType).toBe("RawSource");
      expect(Array.isArray(postBody.rows)).toBe(true);
    });

    it("openl_append_table reports a precise error when appendData is a malformed JSON string (no request sent)", async () => {
      let called = false;
      mockAxios.onPost(/\/lines/).reply(() => {
        called = true;
        return [200];
      });

      await expect(
        executeTool("append_table", {
          projectId: "p1",
          tableId: "t1",
          appendData: '{"tableType":"RawSource","rows":[',
        }, client)
      ).rejects.toThrow(/not valid JSON/);
      expect(called).toBe(false);
    });

    it("openl_append_table rejects a payload whose shape does not match its tableType (no request sent)", async () => {
      let called = false;
      mockAxios.onPost(/\/lines/).reply(() => {
        called = true;
        return [200];
      });

      // Data appends use rows:[{values}], not rules — the union must catch this.
      await expect(
        executeTool("append_table", {
          projectId: "p1",
          tableId: "t1",
          appendData: { tableType: "Data", rules: [{ a: 1 }] },
        }, client)
      ).rejects.toThrow(/rules|Unrecognized key/);
      expect(called).toBe(false);
    });

    it("openl_append_table rejects a miscased RawSource discriminator", async () => {
      await expect(executeTool("append_table", {
        projectId: "p1", tableId: "t1",
        appendData: { tableType: "rawsource", rows: [[{ value: 1 }]] },
      }, client)).rejects.toThrow(/must be exactly "RawSource"/);
    });

    it("openl_update_table accepts view sent as a JSON string and forwards a real object", async () => {
      let putBody: Record<string, any> = {};
      mockAxios.onPut(/\/tables\/t1$/).reply((config) => {
        putBody = JSON.parse(config.data);
        return [204];
      });
      mockAxios.onGet(/\/tables\/t1$/).reply(200, {
        id: "t1", name: "calc", tableType: "RawSource", kind: "Rules", source: [[{ value: "Rules void calc()" }]],
      });

      const view = { id: "t1", name: "calc", tableType: "RawSource", kind: "Rules", source: [[{ value: "Rules void calc()" }]] };
      const result = await executeTool("update_table", {
        projectId: "p1",
        tableId: "t1",
        view: JSON.stringify(view),
      }, client);

      expect(result.content[0].text).toContain("Successfully updated table");
      expect(putBody.tableType).toBe("RawSource");
    });

    it("openl_get_table multi-values round-trip through a full-table update", async () => {
      const rawTable = {
        id: "t1",
        tableType: "RawSource",
        kind: "Data",
        source: [
          [{ cell: "A1", value: "Data Rating ratings" }],
          [{ cell: "A2", value: "attributes" }],
          [{ cell: "A3", value: "Ratings" }],
          [{ cell: "A4", value: ["MA2", "FA+", "SPA"] }],
        ],
      };
      let putBody: Record<string, any> = {};
      mockAxios.onGet(/\/tables\/t1$/).reply(200, rawTable);
      mockAxios.onPut(/\/tables\/t1$/).reply((config) => {
        putBody = JSON.parse(config.data);
        return [204];
      });

      const getResult = await executeTool("get_table", {
        projectId: "p1", tableId: "t1", response_format: "json",
      }, client);
      const view = JSON.parse(getResult.content[0].text as string).data;
      await executeTool("update_table", { projectId: "p1", tableId: "t1", view }, client);

      expect(putBody.source[3][0].value).toEqual(["MA2", "FA+", "SPA"]);
    });

    it("openl_update_table rejects a windowed RawSource view before it can delete omitted rows", async () => {
      const view = {
        id: "t1",
        name: "calc",
        tableType: "RawSource",
        kind: "Rules",
        source: [[{ value: "Rules void calc()" }]],
        totalRows: 400,
      };

      await expect(executeTool("update_table", {
        projectId: "p1",
        tableId: "t1",
        view,
      }, client)).rejects.toThrow(/windowed RawSource view cannot replace the whole table/);
      expect(mockAxios.history.put).toHaveLength(0);
    });

    it("openl_update_table rejects an incomplete view after totalRows was removed", async () => {
      mockAxios.onGet(/\/tables\/t1$/).reply(200, {
        id: "t1",
        tableType: "RawSource",
        source: [[{ value: "Rules void calc()" }]],
        totalRows: 359,
      });

      await expect(executeTool("update_table", {
        projectId: "p1",
        tableId: "t1",
        view: {
          id: "t1",
          tableType: "RawSource",
          source: Array.from({ length: 5 }, () => [{ value: null }]),
        },
      }, client)).rejects.toThrow(/current table has 359 row\(s\)/);
      expect(mockAxios.history.get[0].params).toMatchObject({ startRow: 0, maxRows: 1 });
      expect(mockAxios.history.put).toHaveLength(0);
    });

    it("openl_update_table rejects mismatched table ids before probing the live table", async () => {
      mockAxios.onGet(/\/tables\/t1$/).reply(200, {
        id: "t1",
        tableType: "RawSource",
        source: [[{ value: "Rules void calc()" }]],
        totalRows: 359,
      });

      await expect(executeTool("update_table", {
        projectId: "p1",
        tableId: "t1",
        view: {
          id: "other-table",
          tableType: "RawSource",
          source: [[{ value: "Rules void calc()" }]],
        },
      }, client)).rejects.toThrow(/Table ID mismatch.*t1.*other-table/);
      expect(mockAxios.history.get).toHaveLength(0);
      expect(mockAxios.history.put).toHaveLength(0);
    });

    it("openl_update_table rejects read-only styles instead of silently ignoring them", async () => {
      await expect(executeTool("update_table", {
        projectId: "p1",
        tableId: "t1",
        view: {
          tableType: "RawSource",
          name: "Driver",
          source: [[{
            value: "Rules void Driver()",
            style: { background: "#4472C4", color: "#FFFFFF", bold: true },
          }]],
        },
      }, client)).rejects.toThrow(/unrecognized (?:field|key).*style/i);
      expect(mockAxios.history.put).toHaveLength(0);
    });

    it("openl_update_table rejects a view that is a plain (non-JSON) string (no request sent)", async () => {
      let called = false;
      mockAxios.onPut(/\/tables\//).reply(() => {
        called = true;
        return [204];
      });

      await expect(
        executeTool("update_table", {
          projectId: "p1",
          tableId: "t1",
          view: "SimpleRules",
        }, client)
      ).rejects.toThrow(/Invalid arguments for update_table/);
      expect(called).toBe(false);
    });

    it("openl_append_table rejects the removed typed Spreadsheet append contract", async () => {
      let called = false;
      mockAxios.onPost(/\/lines/).reply(() => {
        called = true;
        return [200];
      });

      // 2 row headers but only 1 cell row — caught before any backend probe/POST.
      await expect(
        executeTool("append_table", {
          projectId: "p1",
          tableId: "t1",
          appendData: {
            tableType: "Spreadsheet",
            rows: [{ name: "Step1" }, { name: "Step2" }],
            cells: [[{ value: "=1+1" }]],
          },
        }, client)
      ).rejects.toThrow(/Typed table DTOs are intentionally unsupported/);
      expect(called).toBe(false);
    });

    it("openl_append_table rejects typed Spreadsheet rows even without cells", async () => {
      let called = false;
      mockAxios.onPost(/\/lines/).reply(() => {
        called = true;
        return [200];
      });

      await expect(executeTool("append_table", {
        projectId: "p1",
        tableId: "t1",
        appendData: { tableType: "Spreadsheet", rows: [{ name: "Step1" }] },
      }, client)).rejects.toThrow(/Invalid arguments for append_table/);
      expect(called).toBe(false);
    });

    it("openl_append_table appends Spreadsheet workbook rows through RawSource", async () => {
      mockAxios.onGet(/\/tables\/sheet1$/).reply(200, {
        id: "sheet1", name: "Calc", tableType: "RawSource", kind: "Spreadsheet", source: [[{}, {}]],
      });
      let postBody: Record<string, any> = {};
      mockAxios.onPost(/\/tables\/sheet1\/lines/).reply((config) => {
        postBody = JSON.parse(config.data);
        return [200];
      });

      const result = await executeTool("append_table", {
        projectId: "p1",
        tableId: "sheet1",
        appendData: {
          tableType: "RawSource",
          rows: [[{ value: "Step1" }, { value: "=1+1" }], [{ value: "Step2" }, { value: "=2+2" }]],
        },
      }, client);

      expect(result.content[0].text).toContain("Successfully appended 2 raw source row(s)");
      expect(postBody.tableType).toBe("RawSource");
      expect(Array.isArray(postBody.rows)).toBe(true);
    });

    it("openl_delete_table DELETEs the table and reports success", async () => {
      let url = "";
      mockAxios.onDelete(/\/tables\/t1$/).reply((config) => {
        url = config.url || "";
        return [204];
      });

      const result = await executeTool("delete_table", { projectId: "p1", tableId: "t1" }, client);

      expect(url).toMatch(/\/projects\/p1\/tables\/t1$/);
      expect(result.content[0].text).toContain("Successfully deleted table t1");
      expect(result.content[0].text).toContain("openl_project_status");
    });

    it("openl_delete_table requires projectId and tableId (no request sent)", async () => {
      let called = false;
      mockAxios.onDelete(/\/tables\//).reply(() => {
        called = true;
        return [204];
      });

      await expect(
        executeTool("delete_table", { projectId: "p1" }, client),
      ).rejects.toThrow(/Invalid arguments for delete_table.*tableId/s);
      expect(called).toBe(false);
    });
  });

  describe("Table Action Tools (raw source)", () => {
    it("registers one tool per operation×orientation (one-or-many), plus the update/merge tools", () => {
      const names = getAllTools().map((t) => t.name);
      for (const name of [
        "append_table_rows", "append_table_columns",
        "insert_table_rows", "insert_table_columns",
        "delete_table_rows", "delete_table_columns",
        "update_table_row", "update_table_column", "update_table_cell",
        "update_table_range", "merge_table_cells", "unmerge_table_cells",
      ]) {
        expect(names).toContain(name);
      }
      // The old singular row/column tools were folded into the *_rows/*_columns tools.
      for (const gone of [
        "append_table_row", "append_table_column",
        "insert_table_row", "insert_table_column",
        "delete_table_row", "delete_table_column",
      ]) {
        expect(names).not.toContain(gone);
      }
    });

    it("openl_insert_table_rows sends the rows block target even for ONE row", async () => {
      let postBody: Record<string, any> = {};
      mockAxios.onPost(/\/tables\/t1\/actions$/).reply((config) => {
        postBody = JSON.parse(config.data);
        return [204];
      });
      mockAxios.onGet(/\/tables\/t1$/).reply(200, { id: "t1", name: "T", tableType: "RawSource", kind: "Other" });

      const result = await executeTool("insert_table_rows", {
        projectId: "p1",
        tableId: "t1",
        position: 3,
        cells: [[{ value: "A" }, { value: "B" }]],
      }, client);

      // The studio has only a `rows` block target — one row is a one-element block.
      expect(postBody).toEqual({
        operation: "insert",
        target: { type: "rows", position: 3, cells: [[{ value: "A" }, { value: "B" }]] },
      });
      expect(result.content[0].text).toContain("Successfully inserted rows into table t1");
      expect(result.content[0].text).not.toContain("tableIdChanged");
    });

    it("openl_insert_table_rows sends the block insert/rows target for SEVERAL rows", async () => {
      let postBody: Record<string, any> = {};
      mockAxios.onPost(/\/tables\/t1\/actions$/).reply((config) => {
        postBody = JSON.parse(config.data);
        return [204];
      });
      mockAxios.onGet(/\/tables\/t1$/).reply(200, { id: "t1", name: "T", tableType: "RawSource", kind: "Other" });

      await executeTool("insert_table_rows", {
        projectId: "p1", tableId: "t1", position: 2,
        cells: [[{ value: "a" }, { value: "b" }], [{ value: "c" }, { value: "d" }]],
      }, client);

      // Two rows → block `rows` target with the 2D cells array.
      expect(postBody).toEqual({
        operation: "insert",
        target: { type: "rows", position: 2, cells: [[{ value: "a" }, { value: "b" }], [{ value: "c" }, { value: "d" }]] },
      });
    });

    it("openl_append_table_columns requires non-empty cells (omitting them is rejected, no request sent)", async () => {
      let called = false;
      mockAxios.onPost(/\/actions$/).reply(() => {
        called = true;
        return [204];
      });

      await expect(
        executeTool("append_table_columns", { projectId: "p1", tableId: "t1" }, client),
      ).rejects.toThrow(/Invalid arguments for append_table_columns/);
      expect(called).toBe(false);
    });

    it("openl_append_table_columns sends the columns block target even for ONE column", async () => {
      let postBody: Record<string, any> = {};
      mockAxios.onPost(/\/tables\/t1\/actions$/).reply((config) => {
        postBody = JSON.parse(config.data);
        return [204];
      });
      mockAxios.onGet(/\/tables\/t1$/).reply(200, { id: "t1", name: "T", tableType: "RawSource", kind: "Other" });

      await executeTool("append_table_columns", { projectId: "p1", tableId: "t1", cells: [[{ value: "a" }, { value: "b" }]] }, client);

      expect(postBody).toEqual({ operation: "append", target: { type: "columns", cells: [[{ value: "a" }, { value: "b" }]] } });
    });

    it("openl_update_table_cell sends an explicit null value so the cell is cleared", async () => {
      let postBody: Record<string, any> = {};
      mockAxios.onPost(/\/tables\/t1\/actions$/).reply((config) => {
        postBody = JSON.parse(config.data);
        return [204];
      });
      mockAxios.onGet(/\/tables\/t1$/).reply(200, { id: "t1", name: "T", tableType: "RawSource", kind: "Other" });

      await executeTool("update_table_cell", {
        projectId: "p1", tableId: "t1", row: 2, column: 1, value: null,
      }, client);

      expect(postBody).toEqual({
        operation: "update",
        target: { type: "cell", row: 2, column: 1, value: null },
      });
    });

    it("openl_update_table_cell forwards a Studio multi-value array unchanged", async () => {
      let postBody: Record<string, any> = {};
      mockAxios.onPost(/\/tables\/t1\/actions$/).reply((config) => {
        postBody = JSON.parse(config.data);
        return [204];
      });
      mockAxios.onGet(/\/tables\/t1$/).reply(200, {
        id: "t1", name: "T", tableType: "RawSource", kind: "Data",
      });

      await executeTool("update_table_cell", {
        projectId: "p1", tableId: "t1", row: 3, column: 0,
        value: ["MA2", "FA+", "SPA"],
      }, client);

      expect(postBody).toEqual({
        operation: "update",
        target: { type: "cell", row: 3, column: 0, value: ["MA2", "FA+", "SPA"] },
      });
    });

    it("openl_update_table_cell requires value (omitting it is rejected, no request sent)", async () => {
      let called = false;
      mockAxios.onPost(/\/actions$/).reply(() => {
        called = true;
        return [204];
      });

      await expect(
        executeTool("update_table_cell", { projectId: "p1", tableId: "t1", row: 2, column: 1 }, client),
      ).rejects.toThrow(/Invalid arguments for update_table_cell/);
      expect(called).toBe(false);
    });

    it("openl_merge_table_cells POSTs a merge/cells action with the span", async () => {
      let postBody: Record<string, any> = {};
      mockAxios.onPost(/\/tables\/t1\/actions$/).reply((config) => {
        postBody = JSON.parse(config.data);
        return [204];
      });
      mockAxios.onGet(/\/tables\/t1$/).reply(200, { id: "t1", name: "T", tableType: "RawSource", kind: "Other" });

      await executeTool("merge_table_cells", {
        projectId: "p1", tableId: "t1", row: 1, column: 0, rowspan: 2, colspan: 3,
      }, client);

      expect(postBody).toEqual({
        operation: "merge",
        target: { type: "cells", row: 1, column: 0, rowspan: 2, colspan: 3 },
      });
    });

    it("reports the new table id and previousTableId when an edit relocates the table", async () => {
      mockAxios.onPost(/\/tables\/t1\/actions$/).reply(200, { id: "t1_moved" });
      mockAxios.onGet(/\/tables\/t1_moved$/).reply(200, { id: "t1_moved", name: "T", tableType: "RawSource", kind: "Other" });

      const result = await executeTool("delete_table_rows", {
        projectId: "p1", tableId: "t1", position: 5,
      }, client);

      const text = result.content[0].text;
      expect(text).toContain("t1_moved");
      expect(text).toContain("previousTableId");
      expect(text).toContain("t1");
    });

    it("openl_delete_table_rows defaults count to 1 on the rows block target", async () => {
      let postBody: Record<string, any> = {};
      mockAxios.onPost(/\/tables\/t1\/actions$/).reply((config) => {
        postBody = JSON.parse(config.data);
        return [204];
      });
      mockAxios.onGet(/\/tables\/t1$/).reply(200, { id: "t1", name: "T", tableType: "RawSource", kind: "Other" });

      await executeTool("delete_table_rows", { projectId: "p1", tableId: "t1", position: 4 }, client);

      // count omitted → defaults to 1; the studio has only the `rows` block target.
      expect(postBody).toEqual({ operation: "delete", target: { type: "rows", position: 4, count: 1 } });
    });

    it("rejects insert_table_rows without a position (no request sent)", async () => {
      let called = false;
      mockAxios.onPost(/\/actions$/).reply(() => {
        called = true;
        return [204];
      });

      await expect(
        executeTool("insert_table_rows", { projectId: "p1", tableId: "t1", cells: [[{ value: "a" }]] }, client),
      ).rejects.toThrow(/Invalid arguments for insert_table_rows/);
      expect(called).toBe(false);
    });

    it("rejects a negative position before reaching the backend", async () => {
      let called = false;
      mockAxios.onPost(/\/actions$/).reply(() => {
        called = true;
        return [204];
      });

      await expect(
        executeTool("delete_table_rows", { projectId: "p1", tableId: "t1", position: -1 }, client),
      ).rejects.toThrow(/Invalid arguments for delete_table_rows/);
      expect(called).toBe(false);
    });

    it("rejects a 1x1 (no-op) merge before reaching the backend", async () => {
      let called = false;
      mockAxios.onPost(/\/actions$/).reply(() => {
        called = true;
        return [204];
      });

      await expect(
        executeTool("merge_table_cells", { projectId: "p1", tableId: "t1", row: 0, column: 0, rowspan: 1, colspan: 1 }, client),
      ).rejects.toThrow(/more than one cell/);
      expect(called).toBe(false);
    });

    it("rejects a cell with a zero/negative colspan before reaching the backend", async () => {
      let called = false;
      mockAxios.onPost(/\/actions$/).reply(() => {
        called = true;
        return [204];
      });

      await expect(
        executeTool("append_table_rows", { projectId: "p1", tableId: "t1", cells: [[{ value: "a", colspan: 0 }]] }, client),
      ).rejects.toThrow(/Invalid arguments for append_table_rows/);
      expect(called).toBe(false);
    });

    it("rejects deleting the header row at position 0 (backend constraint, no request sent)", async () => {
      let called = false;
      mockAxios.onPost(/\/actions$/).reply(() => {
        called = true;
        return [204];
      });

      await expect(
        executeTool("delete_table_rows", { projectId: "p1", tableId: "t1", position: 0 }, client),
      ).rejects.toThrow(/Invalid arguments for delete_table_rows/);
      expect(called).toBe(false);
    });

    it("openl_delete_table_rows POSTs a delete/rows block action when count >= 2", async () => {
      let postBody: Record<string, any> = {};
      mockAxios.onPost(/\/tables\/t1\/actions$/).reply((config) => {
        postBody = JSON.parse(config.data);
        return [204];
      });
      mockAxios.onGet(/\/tables\/t1$/).reply(200, { id: "t1", name: "T", tableType: "RawSource", kind: "Other" });

      await executeTool("delete_table_rows", { projectId: "p1", tableId: "t1", position: 3, count: 4 }, client);

      expect(postBody).toEqual({ operation: "delete", target: { type: "rows", position: 3, count: 4 } });
    });

    it("openl_update_table_range POSTs an update/range block action anchored at row/column", async () => {
      let postBody: Record<string, any> = {};
      mockAxios.onPost(/\/tables\/t1\/actions$/).reply((config) => {
        postBody = JSON.parse(config.data);
        return [204];
      });
      mockAxios.onGet(/\/tables\/t1$/).reply(200, { id: "t1", name: "T", tableType: "RawSource", kind: "Other" });

      await executeTool("update_table_range", {
        projectId: "p1", tableId: "t1", row: 1, column: 2,
        cells: [[{ value: "x" }, { value: "y" }]],
      }, client);

      expect(postBody).toEqual({
        operation: "update",
        target: { type: "range", row: 1, column: 2, cells: [[{ value: "x" }, { value: "y" }]] },
      });
    });

    it("rejects a 1x1 update range (use update_table_cell)", async () => {
      let called = false;
      mockAxios.onPost(/\/actions$/).reply(() => {
        called = true;
        return [204];
      });

      await expect(
        executeTool("update_table_range", { projectId: "p1", tableId: "t1", row: 0, column: 0, cells: [[{ value: "x" }]] }, client),
      ).rejects.toThrow(/more than one cell/);
      expect(called).toBe(false);
    });
  });

  describe("Backend error parsing", () => {
    const callAndCatch = async (): Promise<Error> => {
      try {
        await executeTool("update_table_cell", { projectId: "p1", tableId: "t1", row: 0, column: 0, value: "x" }, client);
      } catch (e) {
        return e as Error;
      }
      throw new Error("expected the tool to throw");
    };

    it("surfaces a ValidationError's field and global errors in the message", async () => {
      mockAxios.onPost(/\/tables\/t1\/actions$/).reply(400, {
        code: "VALIDATION_ERROR",
        message: "Validation failed",
        fields: [
          { field: "cells", code: "TOO_TALL", message: "column height 6 exceeds table height 5", rejectedValue: [[{ value: "a" }]] },
          { field: "position", message: "must be >= 1", rejectedValue: 0 },
        ],
        errors: [{ code: "G1", message: "the table has no room to grow" }],
      });

      const msg = (await callAndCatch()).message;
      // Top-level message kept as the headline.
      expect(msg).toContain("Validation failed");
      // Each field error surfaces with its field, reason, and rejected value.
      expect(msg).toContain("Field errors:");
      expect(msg).toContain("cells: column height 6 exceeds table height 5");
      expect(msg).toContain("position: must be >= 1 (rejected: 0)");
      // Additional global errors surface too.
      expect(msg).toContain("Additional errors:");
      expect(msg).toContain("the table has no room to grow");
    });

    it("uses the field errors as the headline when the ValidationError has no top-level message", async () => {
      mockAxios.onPost(/\/tables\/t1\/actions$/).reply(400, {
        code: "VALIDATION_ERROR",
        fields: [{ field: "row", message: "must be within the table" }],
      });

      const msg = (await callAndCatch()).message;
      expect(msg).toContain("Field errors:");
      expect(msg).toContain("row: must be within the table");
    });

    it("leaves a simple BaseError message intact (no field section)", async () => {
      mockAxios.onPost(/\/tables\/t1\/actions$/).reply(409, { code: "LOCKED", message: "Project is locked" });

      const msg = (await callAndCatch()).message;
      expect(msg).toContain("Project is locked");
      expect(msg).not.toContain("Field errors");
    });
  });

  describe("Guidance Tools", () => {
    // The guides tools resolve the bundle via OPENL_MCP_GUIDES_DIR — pointed at a
    // fixture so these tests never depend on the real build artifact (CI runs
    // `npm test` before `npm run build`).
    let savedGuidesDir: string | undefined;

    beforeAll(() => {
      savedGuidesDir = process.env.OPENL_MCP_GUIDES_DIR;
      process.env.OPENL_MCP_GUIDES_DIR = writeGuidesFixture();
    });

    afterAll(() => {
      if (savedGuidesDir === undefined) {
        delete process.env.OPENL_MCP_GUIDES_DIR;
      } else {
        process.env.OPENL_MCP_GUIDES_DIR = savedGuidesDir;
      }
    });

    it("registers the tools under their EPBDS-16156 names (agents-md name is gone)", () => {
      const names = getAllTools().map((t) => t.name);
      expect(names).toEqual(
        expect.arrayContaining(["get_started", "list_guides", "get_guides", "get_project_agent_context"]),
      );
      expect(names).not.toContain("get_project_agents_md");
    });

    it("openl_get_started returns the workflow protocol plus a bundle orientation, not an index dump", async () => {
      const result = await executeTool("get_started", {}, client);

      const text = result.content[0].text;
      // The protocol must direct the agent to the other guidance tools.
      expect(text).toContain("openl_get_project_agent_context");
      expect(text).toContain("openl_list_guides");
      expect(text).toContain("openl_get_guides");
      // Orientation reflects the bundle (source ref, spec ids, section groups)...
      expect(text).toContain("fixture-ref");
      expect(text).toContain(FIXTURE_IDS.rulesXml);
      expect(text).toContain("Introduction (1)");
      // ...but stays an orientation: guide ids/titles are NOT dumped.
      expect(text).not.toContain(FIXTURE_IDS.smartRules);
    });

    it("openl_list_guides returns filterable metadata only (no bodies)", async () => {
      const result = await executeTool("list_guides", {
        type: "guide",
        search: "smart",
        response_format: "json",
      }, client);

      const parsed = JSON.parse(result.content[0].text);
      expect(parsed.data).toHaveLength(1);
      expect(parsed.data[0]).toMatchObject({
        id: FIXTURE_IDS.smartRules,
        type: "guide",
        title: "Smart Rules Table",
      });
      expect(parsed.data[0]).not.toHaveProperty("body");
      expect(parsed.pagination).toMatchObject({ total_count: 1 });
    });

    it("openl_get_guides returns the full bodies for the requested ids", async () => {
      const result = await executeTool("get_guides", {
        ids: [FIXTURE_IDS.rulesXml, FIXTURE_IDS.basicConcepts],
      }, client);

      const text = result.content[0].text;
      expect(text).toContain("**Guide `spec/rules.xml`**");
      expect(text).toContain("Every project has one.");
      expect(text).toContain("Tables hold the rules.");
    });

    it("openl_get_guides fails actionably on unknown ids instead of falling back to the index", async () => {
      await expect(
        executeTool("get_guides", { ids: ["spec/rules.xml", "guide/no-such-doc"] }, client),
      ).rejects.toThrow(/Unknown guide id\(s\): guide\/no-such-doc.*openl_list_guides/);
    });

    it("should execute openl_get_project_agent_context (aggregated document, root-first)", async () => {
      mockAxios.onPost(/\/projects\/.*\/file-search/).reply(200, [
        { path: "foo/P1/AGENTS.md", name: "AGENTS.md", type: "file", basePath: "foo/P1", content: "project guidance" },
        { path: "foo/AGENTS.md", name: "AGENTS.md", type: "file", basePath: "foo", content: "root guidance" },
      ]);

      const result = await executeTool("get_project_agent_context", {
        projectId: "design-P1",
      }, client);

      expect(result.content[0].type).toBe("text");
      const text = result.content[0].text;
      expect(text).toContain("*Important note about this document*");
      // Root file precedes the project file (root-first, project last = highest priority).
      expect(text.indexOf("## /foo/AGENTS.md")).toBeLessThan(text.indexOf("## /foo/P1/AGENTS.md"));
      expect(text.indexOf("root guidance")).toBeLessThan(text.indexOf("project guidance"));
      // No guide ids mentioned → no cross-reference section.
      expect(text).not.toContain("### Referenced guides");
    });

    it("lists the bundled guide ids referenced by the AGENTS.md chain", async () => {
      mockAxios.onPost(/\/projects\/.*\/file-search/).reply(200, [
        {
          path: "foo/P1/AGENTS.md", name: "AGENTS.md", type: "file", basePath: "foo/P1",
          content: `Datatypes are documented in ${FIXTURE_IDS.basicConcepts}; also see spec/rules.xml.`,
        },
      ]);

      const result = await executeTool("get_project_agent_context", {
        projectId: "design-P1",
      }, client);

      const text = result.content[0].text;
      expect(text).toContain("### Referenced guides");
      expect(text).toContain("openl_get_guides");
      // Both referenced ids listed, in index order (spec first in the fixture).
      expect(text.indexOf("- `spec/rules.xml`")).toBeLessThan(
        text.indexOf(`- \`${FIXTURE_IDS.basicConcepts}\``),
      );
    });

    it("returns a 'no files' note when the project has no AGENTS.md", async () => {
      mockAxios.onPost(/\/projects\/.*\/file-search/).reply(200, []);

      const result = await executeTool("get_project_agent_context", {
        projectId: "design-P2",
      }, client);

      expect(result.content[0].text).toBe("No AGENTS.md files apply to this project.");
    });
  });

  describe("Diagnostics Tools", () => {
    it("openl_get_version reports the running version, build identity, and runtime without calling Studio", async () => {
      const result = await executeTool("get_version", {}, client);

      const payload = JSON.parse(result.content[0].text) as {
        data: {
          name: string;
          version: string;
          build: { id: string; source: string };
          runtime: { node: string; platform: string; arch: string };
        };
      };
      const { name, version, build, runtime } = payload.data;

      expect(name).toBe(SERVER_INFO.NAME);
      expect(version).toBe(versionInfo().version);
      // The build id always starts with the version; what follows identifies the
      // build within it and depends on whether build metadata shipped (the Maven
      // build tests after `npm run build`, a plain `npm test` may run before it).
      expect(build.id.startsWith(version)).toBe(true);
      expect(["build-metadata", "unavailable"]).toContain(build.source);
      expect(runtime).toEqual({ node: process.version, platform: process.platform, arch: process.arch });
      // Diagnostics must work when the studio is unreachable — no request at all.
      expect(mockAxios.history.get).toHaveLength(0);
      expect(mockAxios.history.post).toHaveLength(0);
    });

    it("openl_get_version exposes only build identity — no configuration or credentials", async () => {
      const text = (await executeTool("get_version", {}, client)).content[0].text;
      const { data } = JSON.parse(text) as { data: Record<string, unknown> };

      // Assert the payload's exact shape rather than banning substrings: a
      // legitimate branch name (`feature/token-diagnostics`) would trip a
      // "must not contain 'token'" check while the payload is perfectly clean.
      expect(Object.keys(data).sort()).toEqual(["build", "name", "runtime", "version"]);
      const buildKeys = Object.keys(data.build as Record<string, unknown>);
      const allowedBuildKeys = ["builtAt", "commit", "commitDate", "commitShort", "dirty", "id", "ref", "source"];
      expect(buildKeys.filter((key) => !allowedBuildKeys.includes(key))).toEqual([]);
      expect(Object.keys(data.runtime as Record<string, unknown>).sort()).toEqual(["arch", "node", "platform"]);
      // ...and none of THIS client's configured values can appear anywhere in it.
      expect(text).not.toContain("localhost:8080");
      expect(text).not.toContain("openl_pat_test");
    });
  });

  describe("Response Format Variants", () => {
    it("should support json response format", async () => {
      mockAxios.onGet("/repos").reply(200, [
        { id: "design", name: "Design" },
      ]);

      const result = await executeTool("list_repositories", {
        response_format: "json",
      }, client);

      const text = result.content[0].text;
      expect(() => JSON.parse(text)).not.toThrow();

      const data = JSON.parse(text);
      expect(data).toHaveProperty("data");
    });

    // Shared project data so concise and detailed renders are compared on the SAME input.
    const formatVariantProjects = [
      {
        id: "design:p1:hash1",
        name: "p1",
        repository: "design",
        status: "OPENED",
        path: "p1",
        modifiedBy: "admin",
        modifiedAt: "2024-01-01T00:00:00Z",
      },
      {
        id: "design:p2:hash2",
        name: "p2",
        repository: "design",
        status: "CLOSED",
        path: "p2",
        modifiedBy: "admin",
        modifiedAt: "2024-01-01T00:00:00Z",
      },
    ];

    async function renderProjects(response_format: "markdown_concise" | "markdown_detailed") {
      const mockRepos: RepositoryInfo[] = [
        { id: "design", name: "Design Repository", aclId: "acl-design" },
      ];
      mockAxios.onGet("/repos").reply(200, mockRepos);
      mockAxios
        .onGet("/projects", { params: { repository: "design", offset: 0, size: 50 } })
        .reply(200, formatVariantProjects);

      const result = await executeTool("list_projects", {
        repository: "Design Repository", // Use repository name, not ID
        response_format,
      }, client);
      return result.content[0].text as string;
    }

    it("should support markdown_concise response format", async () => {
      const concise = await renderProjects("markdown_concise");
      mockAxios.reset();
      const detailed = await renderProjects("markdown_detailed");

      // Concise emits the "Found N project(s)" summary line (toMarkdownConcise) ...
      expect(concise).toContain("Found 2 projects");
      // ... and must NOT include the detail-only Status Breakdown line.
      expect(concise).not.toContain("**Status Breakdown:**");
      // ... and is strictly shorter than the detailed render of the SAME data.
      expect(concise.length).toBeLessThan(detailed.length);
    });

    it("should support markdown_detailed response format", async () => {
      const detailed = await renderProjects("markdown_detailed");
      mockAxios.reset();
      const concise = await renderProjects("markdown_concise");

      // toMarkdownDetailed emits a "**Status Breakdown:** N opened, M closed" line for projects ...
      expect(detailed).toContain("**Status Breakdown:** 1 opened, 1 closed");
      // ... plus the Summary/Retrieved headings, none of which the concise render emits.
      expect(detailed).toContain("**Summary:**");
      expect(detailed).toContain("**Retrieved:**");
      expect(concise).not.toContain("**Status Breakdown:**");
    });
  });

  describe("Destructive Operation Confirmation", () => {
    it("should execute openl_deploy_project", async () => {
      // Mock production repositories list for getProductionRepositoryIdByName
      const mockProdRepos: RepositoryInfo[] = [
        { id: "production", name: "Production Repository", aclId: "acl-production" },
      ];
      mockAxios.onGet("/production-repos").reply(200, mockProdRepos);

      // deploy_project uses /deployments endpoint
      mockAxios.onPost("/deployments").reply(200, {
        success: true,
        deploymentName: "project1",
      });

      const result = await executeTool("deploy_project", {
        projectId: "design-project1",
        deploymentName: "project1",
        productionRepositoryId: "Production Repository", // Use repository name, not ID
        comment: "Deploy test",
      }, client);

      expect(result.content[0].type).toBe("text");
      expect(result.content[0].text).toContain("success");
    });


    it("should execute openl_open_project", async () => {
      const projectIdForPath = "design-project1";
      const encodedProjectId = encodeURIComponent(projectIdForPath);

      mockAxios.onGet(`/projects/${encodedProjectId}`).reply(200, {
        id: "design:project1:hash123",
        name: "project1",
        repository: "design",
        status: "CLOSED",
        path: "project1",
        modifiedBy: "admin",
        modifiedAt: "2024-01-01T00:00:00Z",
      });

      mockAxios.onPatch(`/projects/${encodedProjectId}`, {
        status: "OPENED",
      }).reply(204);

      const result = await executeTool("open_project", {
        projectId: "design-project1",
      }, client);

      expect(result.content[0].type).toBe("text");
      expect(result.content[0].text).toContain("opened");
    });

    it("should execute openl_open_project with branch", async () => {
      const projectIdForPath = "design-project1";
      const encodedProjectId = encodeURIComponent(projectIdForPath);

      mockAxios.onGet(`/projects/${encodedProjectId}`).reply(200, {
        id: "design:project1:hash123",
        name: "project1",
        repository: "design",
        status: "CLOSED",
        path: "project1",
        modifiedBy: "admin",
        modifiedAt: "2024-01-01T00:00:00Z",
      });

      mockAxios.onPatch(`/projects/${encodedProjectId}`, {
        status: "OPENED",
        branch: "develop",
      }).reply(204);

      const result = await executeTool("open_project", {
        projectId: "design-project1",
        branch: "develop",
      }, client);

      expect(result.content[0].type).toBe("text");
      expect(result.content[0].text).toContain("branch");
    });

    it("should execute openl_save_project", async () => {
      const projectIdForPath = "design-project1";
      const encodedProjectId = encodeURIComponent(projectIdForPath);

      // save_project requires project status EDITING and comment
      mockAxios.onGet(`/projects/${encodedProjectId}`).reply(200, {
        id: "design:project1:hash123",
        name: "project1",
        repository: "design",
        status: "EDITING",
        path: "project1",
        modifiedBy: "admin",
        modifiedAt: "2024-01-01T00:00:00Z",
      });


      // Save is done via PATCH /projects/{projectId} with { comment } (204 No Content)
      mockAxios.onPatch(`/projects/${encodedProjectId}`, {
        comment: "Test commit",
      }).reply(204);

      const result = await executeTool("save_project", {
        projectId: "design-project1",
        comment: "Test commit",
      }, client);

      expect(result.content[0].type).toBe("text");
      expect(result.content[0].text).toContain("success");
      expect(result.content[0].text).toContain("Test commit");
    });

    it("should execute openl_save_project with closeAfterSave sends comment and status CLOSED in one PATCH", async () => {
      const projectIdForPath = "design-project1";
      const encodedProjectId = encodeURIComponent(projectIdForPath);

      mockAxios.onGet(`/projects/${encodedProjectId}`).reply(200, {
        id: "design:project1:hash123",
        name: "project1",
        repository: "design",
        status: "EDITING",
        path: "project1",
        modifiedBy: "admin",
        modifiedAt: "2024-01-01T00:00:00Z",
      });

      let patchBody: { comment?: string; status?: string } = {};
      mockAxios.onPatch(`/projects/${encodedProjectId}`).reply((config) => {
        patchBody = config.data ? JSON.parse(config.data) : {};
        return [204];
      });

      const result = await executeTool("save_project", {
        projectId: "design-project1",
        comment: "Save and close",
        closeAfterSave: true,
      }, client);

      expect(result.content[0].type).toBe("text");
      expect(result.content[0].text).toContain("success");
      expect(result.content[0].text).toContain("Save and close");
      expect(patchBody).toEqual({ comment: "Save and close", status: "CLOSED" });
    });

    it("should execute openl_close_project without unsaved changes", async () => {
      const projectIdForPath = "design-project1";
      const encodedProjectId = encodeURIComponent(projectIdForPath);

      // Mock project fetch to check status
      mockAxios.onGet(`/projects/${encodedProjectId}`).reply(200, {
        id: "design:project1:hash123",
        name: "project1",
        repository: "design",
        status: "OPENED", // No unsaved changes
        path: "project1",
        modifiedBy: "admin",
        modifiedAt: "2024-01-01T00:00:00Z",
      });

      // Mock close
      mockAxios.onPatch(`/projects/${encodedProjectId}`, {
        status: "CLOSED",
      }).reply(200);

      const result = await executeTool("close_project", {
        projectId: "design-project1",
      }, client);

      expect(result.content[0].type).toBe("text");
      expect(result.content[0].text).toContain("closed");
    });

    it("should execute openl_close_project with saveChanges", async () => {
      const projectIdForPath = "design-project1";
      const encodedProjectId = encodeURIComponent(projectIdForPath);

      // Mock project fetch to check status
      mockAxios.onGet(`/projects/${encodedProjectId}`).reply(200, {
        id: "design:project1:hash123",
        name: "project1",
        repository: "design",
        status: "EDITING", // Has unsaved changes
        path: "project1",
        modifiedBy: "admin",
        modifiedAt: "2024-01-01T00:00:00Z",
      });


      // saveProject uses PATCH with { comment }; closeProject uses PATCH with { status: "CLOSED" }
      mockAxios.onPatch(`/projects/${encodedProjectId}`).reply((config) => {
        const data = config.data ? JSON.parse(config.data) : {};
        if (data.status === "CLOSED") return [204];
        if (data.comment === "Save before close") return [204];
        return [404];
      });

      const result = await executeTool("close_project", {
        projectId: "design-project1",
        saveChanges: true,
        comment: "Save before close",
      }, client);

      expect(result.content[0].type).toBe("text");
      expect(result.content[0].text).toContain("saved");
      expect(result.content[0].text).toContain("closed");
    });

    it("should execute openl_close_project with discardChanges", async () => {
      const projectIdForPath = "design-project1";
      const encodedProjectId = encodeURIComponent(projectIdForPath);

      // Mock project fetch to check status
      mockAxios.onGet(`/projects/${encodedProjectId}`).reply(200, {
        id: "design:project1:hash123",
        name: "project1",
        repository: "design",
        status: "EDITING", // Has unsaved changes
        path: "project1",
        modifiedBy: "admin",
        modifiedAt: "2024-01-01T00:00:00Z",
      });

      // Mock close (discarding changes)
      mockAxios.onPatch(`/projects/${encodedProjectId}`, {
        status: "CLOSED",
        discardChanges: true,
      }).reply(200);

      const result = await executeTool("close_project", {
        projectId: "design-project1",
        discardChanges: true,
        confirmDiscard: true,
      }, client);

      expect(result.content[0].type).toBe("text");
      expect(result.content[0].text).toContain("discarded");
    });

    it("should require confirmDiscard when closing EDITING with discardChanges", async () => {
      const projectIdForPath = "design-project1";
      const encodedProjectId = encodeURIComponent(projectIdForPath);

      mockAxios.onGet(`/projects/${encodedProjectId}`).reply(200, {
        id: "design:project1:hash123",
        name: "project1",
        repository: "design",
        status: "EDITING",
        path: "project1",
        modifiedBy: "admin",
        modifiedAt: "2024-01-01T00:00:00Z",
      });

      const result = await executeTool("close_project", {
        projectId: "design-project1",
        discardChanges: true,
        // no confirmDiscard
      }, client);

      expect(result.content[0].type).toBe("text");
      expect(result.content[0].text).toContain("confirmationRequired");
      expect(result.content[0].text).toContain("confirmDiscard");
      expect(result.content[0].text).toContain("unsaved changes");
    });

    it("should error when closing project with unsaved changes without saveChanges or discardChanges", async () => {
      const projectIdForPath = "design-project1";
      const encodedProjectId = encodeURIComponent(projectIdForPath);

      // Mock project fetch to check status
      mockAxios.onGet(`/projects/${encodedProjectId}`).reply(200, {
        id: "design:project1:hash123",
        name: "project1",
        repository: "design",
        status: "EDITING", // Has unsaved changes
        path: "project1",
        modifiedBy: "admin",
        modifiedAt: "2024-01-01T00:00:00Z",
      });

      await expect(
        executeTool("close_project", {
          projectId: "design-project1",
          // No saveChanges or discardChanges
        }, client)
      ).rejects.toThrow(/unsaved changes|saveChanges|discardChanges/i);
    });

    it("should error when saveChanges is true but comment is missing", async () => {
      const projectIdForPath = "design-project1";
      const encodedProjectId = encodeURIComponent(projectIdForPath);

      // Mock project fetch to check status
      mockAxios.onGet(`/projects/${encodedProjectId}`).reply(200, {
        id: "design:project1:hash123",
        name: "project1",
        repository: "design",
        status: "EDITING", // Has unsaved changes
        path: "project1",
        modifiedBy: "admin",
        modifiedAt: "2024-01-01T00:00:00Z",
      });

      await expect(
        executeTool("close_project", {
          projectId: "design-project1",
          saveChanges: true,
          // Missing comment
        }, client)
      ).rejects.toThrow(/comment.*required/i);
    });
  });

  describe("Pagination", () => {
    it("should preserve non-page-aligned project and table offsets", async () => {
      mockAxios.onGet("/projects", { params: { offset: 25, size: 50 } }).reply(200, {
        content: [], pageNumber: 0, pageSize: 50, total: 0,
      });
      mockAxios.onGet("/projects/design-project1/tables", { params: { offset: 25, size: 50 } }).reply(200, {
        content: [], pageNumber: 0, pageSize: 50, total: 0,
      });

      await expect(executeTool("list_projects", {
        limit: 50,
        offset: 25,
      }, client)).resolves.toBeDefined();

      await expect(executeTool("list_tables", {
        projectId: "design-project1",
        limit: 50,
        offset: 25,
      }, client)).resolves.toBeDefined();
    });

    it("should support pagination parameters", async () => {
      // Mock repositories list for getRepositoryIdByName
      const mockRepos: RepositoryInfo[] = [
        { id: "design", name: "Design Repository", aclId: "acl-design" },
      ];
      mockAxios.onGet("/repos").reply(200, mockRepos);

      const mockProjects = Array.from({ length: 100 }, (_, i) => ({
        id: `design:p${i}:hash${i}`,
        name: `p${i}`,
        repository: "design",
        status: "OPENED",
        path: `p${i}`,
        modifiedBy: "admin",
        modifiedAt: "2024-01-01T00:00:00Z",
      }));

      mockAxios.onGet("/projects", { params: { repository: "design", offset: 0, size: 10 } }).reply(200, mockProjects);

      const result = await executeTool("list_projects", {
        repository: "Design Repository", // Use repository name, not ID
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

    it("should list all backend-paginated projects across multiple pages", async () => {
      const allProjects = Array.from({ length: 120 }, (_, i) => ({
        id: `design:p${i}:hash${i}`,
        name: `p${i}`,
        repository: "design",
        status: "CLOSED",
        path: `p${i}`,
        modifiedBy: "admin",
        modifiedAt: "2024-01-01T00:00:00Z",
      }));
      mockAxios.onGet("/projects").reply((config) => {
        const offset = Number(config.params.offset);
        const pageSize = Number(config.params.size);
        const pageNumber = Math.floor(offset / pageSize);
        const content = allProjects.slice(offset, offset + pageSize);
        return [200, {
          content,
          pageNumber,
          pageSize,
          numberOfElements: content.length,
          total: allProjects.length,
        }];
      });

      const first = await executeTool("list_projects", {
        limit: 50,
        offset: 0,
        response_format: "markdown",
      }, client);
      const second = await executeTool("list_projects", {
        limit: 50,
        offset: 50,
        response_format: "json",
      }, client);
      const third = await executeTool("list_projects", {
        limit: 50,
        offset: 100,
        response_format: "json",
      }, client);

      expect(first.content[0].text).toContain("Total: 120");
      expect(first.content[0].text).toContain("More results available (next offset: 50)");

      const secondPage = JSON.parse(second.content[0].text);
      expect(secondPage.data).toHaveLength(50);
      expect(secondPage.data[0].name).toBe("p50");
      expect(secondPage.pagination).toMatchObject({
        limit: 50,
        offset: 50,
        total_count: 120,
        has_more: true,
        next_offset: 100,
      });

      const thirdPage = JSON.parse(third.content[0].text);
      expect(thirdPage.data).toHaveLength(20);
      expect(thirdPage.data[0].name).toBe("p100");
      expect(thirdPage.pagination).toMatchObject({
        limit: 50,
        offset: 100,
        total_count: 120,
        has_more: false,
      });
    });

    it("should enforce maximum limit of 200", async () => {
      // Mock repositories list for getRepositoryIdByName
      const mockRepos: RepositoryInfo[] = [
        { id: "design", name: "Design Repository", aclId: "acl-design" },
      ];
      mockAxios.onGet("/repos").reply(200, mockRepos);

      await expect(
        executeTool("list_projects", {
          repository: "Design Repository", // Use repository name, not ID
          limit: 300, // Exceeds max
        }, client)
      ).rejects.toThrow(/Invalid arguments for list_projects.*limit/s);
    });

    it("should enforce minimum limit of 1", async () => {
      // Mock repositories list for getRepositoryIdByName
      const mockRepos: RepositoryInfo[] = [
        { id: "design", name: "Design Repository", aclId: "acl-design" },
      ];
      mockAxios.onGet("/repos").reply(200, mockRepos);

      await expect(
        executeTool("list_projects", {
          repository: "Design Repository", // Use repository name, not ID
          limit: 0,
        }, client)
      ).rejects.toThrow(/Invalid arguments for list_projects.*limit/s);
    });
  });

  describe("Error Handling", () => {
    it("rejects unknown arguments before a handler can call the backend", async () => {
      await expect(
        executeTool("list_repositories", { limti: 10 }, client)
      ).rejects.toThrow(/Invalid arguments for list_repositories.*limti/s);
      expect(mockAxios.history.get).toHaveLength(0);
    });

    it.each([
      ["project_status", { projectId: "p1", timeoutMs: 600_001 }, /timeoutMs/],
      ["read_project_file", { projectId: "p1", offset: -1 }, /offset/],
      ["get_test_results", { projectId: "p1", page: 0, offset: 0 }, /mutually exclusive/],
      ["append_table", {
        projectId: "p1",
        tableId: "t1",
        appendData: { tableType: "Datatype", fields: [{ name: "age", type: "Integer", requried: true }] },
      }, /tableType must be exactly "RawSource"/],
    ])("enforces the published %s constraints at runtime", async (toolName, args, message) => {
      await expect(executeTool(toolName, args, client)).rejects.toThrow(message);
      expect(mockAxios.history.get).toHaveLength(0);
    });

    it("should return actionable error for missing projectId", async () => {
      await expect(
        executeTool("get_project", {
          // Missing projectId
        }, client)
      ).rejects.toThrow(/Invalid arguments for get_project.*projectId/s);
      await expect(
        executeTool("get_project", {}, client)
      ).rejects.toThrow(/openl_list_projects/);
    });

    it("should return actionable error for invalid response_format", async () => {
      // Mock repositories list for getRepositoryIdByName
      const mockRepos: RepositoryInfo[] = [
        { id: "design", name: "Design Repository", aclId: "acl-design" },
      ];
      mockAxios.onGet("/repos").reply(200, mockRepos);

      await expect(
        executeTool("list_projects", {
          repository: "Design Repository", // Use repository name, not ID
          response_format: "xml" as any,
        }, client)
      ).rejects.toThrow(/markdown_concise.*markdown_detailed/);
    });
  });

  describe("Test Execution Tools", () => {
    const projectIdForPath = "design-project1";
    const encodedProjectId = encodeURIComponent(projectIdForPath);

    beforeEach(() => {
      // Mock project fetch for auto-open functionality
      mockAxios.onGet(`/projects/${encodedProjectId}`).reply(200, {
        id: "design:project1:hash123",
        name: "project1",
        repository: "design",
        status: "OPENED",
        path: "project1",
        modifiedBy: "admin",
        modifiedAt: "2024-01-01T00:00:00Z",
      });
    });

    describe("start_project_tests", () => {
      it("should execute openl_start_project_tests and store session headers", async () => {
        // Mock /tests/run endpoint - returns 202 with session headers
        const sessionHeaders = {
          "x-test-execution-id": "test-session-123",
          "set-cookie": ["JSESSIONID=abc123; Path=/"],
        } as any;

        mockAxios.onPost(`/projects/${encodedProjectId}/tests/run`).reply(202, {
          status: "accepted",
        }, sessionHeaders);

        const result = await executeTool("start_project_tests", {
          projectId: "design-project1",
        }, client);

        expect(result.content[0].type).toBe("text");
        expect(result.content[0].text).toContain("started");

        // Verify that headers were stored by checking that subsequent calls work
        // This is verified indirectly through get_test_results tests
      });

      it("should execute openl_start_project_tests with tableId", async () => {
        const sessionHeaders = {
          "x-test-execution-id": "test-session-456",
          "set-cookie": ["JSESSIONID=def456; Path=/"],
        } as any;

        mockAxios.onPost(`/projects/${encodedProjectId}/tests/run`, undefined, {
          params: { tableId: "Test_calculatePremium_1234" },
        }).reply(202, {
          status: "accepted",
        }, sessionHeaders);

        const result = await executeTool("start_project_tests", {
          projectId: "design-project1",
          tableId: "Test_calculatePremium_1234",
        }, client);

        expect(result.content[0].type).toBe("text");
        expect(result.content[0].text).toContain("started");
      });

      it("should auto-open project if closed", async () => {
        // Mock project as closed
        mockAxios.onGet(`/projects/${encodedProjectId}`).reply(200, {
          id: "design:project1:hash123",
          name: "project1",
          repository: "design",
          status: "CLOSED",
          path: "project1",
          modifiedBy: "admin",
          modifiedAt: "2024-01-01T00:00:00Z",
        });

        // Mock project open
        mockAxios.onPatch(`/projects/${encodedProjectId}`, {
          status: "OPENED",
        }).reply(200);

        const sessionHeaders = {
          "x-test-execution-id": "test-session-789",
        };

        mockAxios.onPost(`/projects/${encodedProjectId}/tests/run`).reply(202, {
          status: "accepted",
        }, sessionHeaders);

        const result = await executeTool("start_project_tests", {
          projectId: "design-project1",
        }, client);

        expect(result.content[0].text).toContain("automatically opened");
      });

      it("reads the results in the session the run started in", async () => {
        mockAxios.onPost(`/projects/${encodedProjectId}/tests/run`).reply(202, {
          status: "accepted",
        }, { "set-cookie": ["JSESSIONID=xyz789; Path=/"] });

        await executeTool("start_project_tests", {
          projectId: "design-project1",
        }, client);

        mockAxios.onGet(/\/projects\/design-project1\/tests\/summary/).reply((config) => {
          expect(config.headers).toHaveProperty("Cookie", "JSESSIONID=xyz789");
          expect(config.headers).toHaveProperty("Accept", "application/json");

          return [200, {
            testCases: [],
            executionTimeMs: 100,
            numberOfTests: 0,
            numberOfFailures: 0,
          }];
        });

        await executeTool("get_test_results_summary", {
          projectId: "design-project1",
        }, client);
      });
    });

    describe("get_test_results_summary", () => {
      it("waits through 202 notReady for every test-result view", async () => {
        mockAxios.onPost(`/projects/${encodedProjectId}/tests/run`).reply(202, undefined, {
          "x-test-execution-id": "test-session-not-ready",
        });
        await executeTool("start_project_tests", {
          projectId: "design-project1",
        }, client);

        let resultReads = 0;
        mockAxios.onGet(/\/projects\/design-project1\/tests\/summary/).reply(() => {
          resultReads += 1;
          if (resultReads % 2 === 1) {
            return [202, { status: "notReady" }];
          }
          return [200, {
            testCases: [{
              name: "Test_calculatePremium",
              tableId: "Test_calculatePremium_1234",
              executionTimeMs: 10,
              numberOfTests: 2,
              numberOfFailures: 0,
              testUnits: [],
            }],
            executionTimeMs: 10,
            numberOfTests: 2,
            numberOfFailures: 0,
            pageNumber: 0,
            pageSize: 50,
            numberOfElements: 1,
          }];
        });

        const summary = await executeTool("get_test_results_summary", {
          projectId: "design-project1",
          response_format: "json",
        }, client);
        const full = await executeTool("get_test_results", {
          projectId: "design-project1",
          response_format: "json",
        }, client);
        const byTable = await executeTool("get_test_results_by_table", {
          projectId: "design-project1",
          tableId: "Test_calculatePremium_1234",
          response_format: "json",
        }, client);

        for (const response of [summary, full, byTable]) {
          expect(JSON.parse(response.content[0].text).data).toMatchObject({
            numberOfTests: 2,
            numberOfFailures: 0,
          });
        }
        expect(resultReads).toBe(6);
      });

      it("stops waiting when the MCP request is cancelled", async () => {
        mockAxios.onPost(`/projects/${encodedProjectId}/tests/run`).reply(202, undefined, {
          "x-test-execution-id": "test-session-cancelled",
        });
        await executeTool("start_project_tests", {
          projectId: "design-project1",
        }, client);

        const controller = new AbortController();
        mockAxios.onGet(/\/projects\/design-project1\/tests\/summary/).reply(() => {
          controller.abort();
          return [202, { status: "notReady" }];
        });

        await expect(executeTool("get_test_results_summary", {
          projectId: "design-project1",
        }, client, { signal: controller.signal })).rejects.toThrow(/aborted/i);
      });

      it("should execute openl_get_test_results_summary reading the results as JSON", async () => {
        // First, start test execution
        const sessionHeaders = {
          "x-test-execution-id": "test-session-summary",
          "set-cookie": ["JSESSIONID=summary123; Path=/"],
        } as any;

        mockAxios.onPost(`/projects/${encodedProjectId}/tests/run`).reply(202, {
          status: "accepted",
        }, sessionHeaders);

        await executeTool("start_project_tests", {
          projectId: "design-project1",
        }, client);

        // Now get test results summary
        const mockSummary: Types.TestsExecutionSummary = {
          testCases: [],
          executionTimeMs: 250.5,
          numberOfTests: 10,
          numberOfFailures: 2,
        };

        mockAxios.onGet(/\/projects\/design-project1\/tests\/summary/).reply((config) => {
          expect(config.headers).toHaveProperty("Accept", "application/json");
          return [200, mockSummary];
        });

        const result = await executeTool("get_test_results_summary", {
          projectId: "design-project1",
          response_format: "markdown",
        }, client);

        expect(result.content[0].type).toBe("text");
        const text = result.content[0].text;
        expect(text).toContain("Test Results Summary");
        expect(text).toContain("10"); // Total tests
        expect(text).toContain("8"); // Passed (10 - 2)
        expect(text).toContain("2"); // Failed
      });

      it("should error when no test session exists", async () => {
        // Client checks for headers first and throws before API call
        // But error gets wrapped by tool handler, so just check that error is thrown
        await expect(
          executeTool("get_test_results_summary", {
            projectId: "design-project1",
          }, client)
        ).rejects.toThrow();
      });

      it("should support failures parameter", async () => {
        // Start test execution
        mockAxios.onPost(`/projects/${encodedProjectId}/tests/run`).reply(202, {
          status: "accepted",
        }, { "x-test-execution-id": "test-session-failures" });

        await executeTool("start_project_tests", {
          projectId: "design-project1",
        }, client);

        // Get summary with failures parameter
        mockAxios.onGet(`/projects/${encodedProjectId}/tests/summary`, {
          params: { failures: 5 },
        }).reply(200, {
          testCases: [],
          executionTimeMs: 100,
          numberOfTests: 10,
          numberOfFailures: 2,
        });

        const result = await executeTool("get_test_results_summary", {
          projectId: "design-project1",
          failures: 5,
        }, client);

        expect(result.content[0].type).toBe("text");
      });

      it("should pass unpaged=true parameter", async () => {
        mockAxios.onPost(`/projects/${encodedProjectId}/tests/run`).reply(202, {
          status: "accepted",
        }, { "x-test-execution-id": "test-session-summary-unpaged" });

        await executeTool("start_project_tests", {
          projectId: "design-project1",
        }, client);

        mockAxios.onGet(`/projects/${encodedProjectId}/tests/summary`, {
          params: { failures: 5, unpaged: true },
        }).reply(200, {
          testCases: [],
          executionTimeMs: 100,
          numberOfTests: 10,
          numberOfFailures: 2,
        });

        const result = await executeTool("get_test_results_summary", {
          projectId: "design-project1",
          unpaged: true,
        }, client);

        expect(result.content[0].type).toBe("text");
      });
    });

    describe("get_test_results", () => {
      it("should execute openl_get_test_results reading the results as JSON", async () => {
        // Start test execution
        const sessionHeaders = {
          "x-test-execution-id": "test-session-results",
          "set-cookie": ["JSESSIONID=results456; Path=/"],
        } as any;

        mockAxios.onPost(`/projects/${encodedProjectId}/tests/run`).reply(202, {
          status: "accepted",
        }, sessionHeaders);

        await executeTool("start_project_tests", {
          projectId: "design-project1",
        }, client);

        // Get full test results
        const mockResults: Types.TestsExecutionSummary = {
          testCases: [
            {
              name: "Test_calculatePremium",
              tableId: "Test_calculatePremium_1234",
              executionTimeMs: 50,
              numberOfTests: 5,
              numberOfFailures: 0,
              testUnits: [],
            },
            {
              name: "Test_calculateDiscount",
              tableId: "Test_calculateDiscount_5678",
              executionTimeMs: 30,
              numberOfTests: 3,
              numberOfFailures: 1,
              testUnits: [{
                id: "discount-case-3",
                status: "TR_NEQ",
                executionTimeMs: 10,
                testAssertions: [{
                  description: "Discount",
                  expectedValue: 0.2,
                  actualValue: 0.1,
                  status: "TR_NEQ",
                }],
              }],
            },
          ],
          executionTimeMs: 80,
          numberOfTests: 8,
          numberOfFailures: 1,
          pageNumber: 0,
          pageSize: 50,
          numberOfElements: 2,
        };

        mockAxios.onGet(/\/projects\/design-project1\/tests\/summary/).reply((config) => {
          expect(config.headers).toHaveProperty("Cookie", "JSESSIONID=results456");
          expect(config.headers).toHaveProperty("Accept", "application/json");
          return [200, mockResults];
        });

        const result = await executeTool("get_test_results", {
          projectId: "design-project1",
          response_format: "markdown",
        }, client);

        expect(result.content[0].type).toBe("text");
        const text = result.content[0].text;
        expect(text).toContain("Test Results");
        expect(text).toContain("Test_calculatePremium");
        expect(text).toContain("Test_calculateDiscount");
        expect(text).toContain("PASSED");
        expect(text).toContain("FAILED");
        expect(text).toContain("discount-case-3");
        expect(text).toContain("| Discount | `0.2` | `0.1` | ❌ FAILED |");
      });

      it("should error when no test session exists", async () => {
        // Client checks for headers first and throws before API call
        // But error gets wrapped by tool handler, so just check that error is thrown
        await expect(
          executeTool("get_test_results", {
            projectId: "design-project1",
          }, client)
        ).rejects.toThrow();
      });

      it("should report the final page without a next offset", async () => {
        // Start test execution
        mockAxios.onPost(`/projects/${encodedProjectId}/tests/run`).reply(202, {
          status: "accepted",
        }, { "x-test-execution-id": "test-session-pagination" });

        await executeTool("start_project_tests", {
          projectId: "design-project1",
        }, client);

        // Get results with pagination
        const mockResults: Types.TestsExecutionSummary = {
          testCases: [],
          executionTimeMs: 100,
          numberOfTests: 75,
          numberOfFailures: 5,
          pageNumber: 1,
          pageSize: 50,
          numberOfElements: 25,
        };

        mockAxios.onGet(`/projects/${encodedProjectId}/tests/summary`, {
          params: { failures: 5, page: 1, size: 50 },
        }).reply(200, mockResults);

        const result = await executeTool("get_test_results", {
          projectId: "design-project1",
          page: 1,
          size: 50,
          response_format: "json",
        }, client);

        expect(result.content[0].type).toBe("text");
        const response = JSON.parse(result.content[0].text);
        expect(response.pagination).toMatchObject({
          limit: 50,
          offset: 50,
          has_more: false,
        });
        expect(response.pagination).not.toHaveProperty("total_count");
        expect(response.pagination).not.toHaveProperty("next_offset");
      });

      it("should calculate offset correctly from pageNumber and pageSize", async () => {
        // Start test execution
        mockAxios.onPost(`/projects/${encodedProjectId}/tests/run`).reply(202, {
          status: "accepted",
        }, { "x-test-execution-id": "test-session-offset" });

        await executeTool("start_project_tests", {
          projectId: "design-project1",
        }, client);

        const mockResults: Types.TestsExecutionSummary = {
          testCases: [],
          executionTimeMs: 100,
          numberOfTests: 100,
          numberOfFailures: 5,
          pageNumber: 2,
          pageSize: 25,
          numberOfElements: 25,
        };

        mockAxios.onGet(`/projects/${encodedProjectId}/tests/summary`, {
          params: { failures: 5, page: 2, size: 25 },
        }).reply(200, mockResults);

        const result = await executeTool("get_test_results", {
          projectId: "design-project1",
          page: 2,
          size: 25,
          response_format: "markdown",
        }, client);

        expect(result.content[0].type).toBe("text");
        // Verify offset is calculated as pageNumber * pageSize = 2 * 25 = 50
        // Pagination shows "Showing items 51-75" (offset+1 to offset+limit)
        const text = result.content[0].text;
        expect(text).toContain("51"); // First item should be 51 (offset 50 + 1)
      });

      it("should support failuresOnly parameter", async () => {
        // Start test execution
        mockAxios.onPost(`/projects/${encodedProjectId}/tests/run`).reply(202, {
          status: "accepted",
        }, { "x-test-execution-id": "test-session-failures-only" });

        await executeTool("start_project_tests", {
          projectId: "design-project1",
        }, client);

        mockAxios.onGet(`/projects/${encodedProjectId}/tests/summary`, {
          params: { failuresOnly: true, failures: 5 },
        }).reply(200, {
          testCases: [],
          executionTimeMs: 100,
          numberOfTests: 5,
          numberOfFailures: 5,
        });

        const result = await executeTool("get_test_results", {
          projectId: "design-project1",
          failuresOnly: true,
        }, client);

        expect(result.content[0].type).toBe("text");
      });

      it("should pass unpaged=true parameter", async () => {
        mockAxios.onPost(`/projects/${encodedProjectId}/tests/run`).reply(202, {
          status: "accepted",
        }, { "x-test-execution-id": "test-session-results-unpaged" });

        await executeTool("start_project_tests", {
          projectId: "design-project1",
        }, client);

        mockAxios.onGet(`/projects/${encodedProjectId}/tests/summary`, {
          params: { failures: 5, unpaged: true },
        }).reply(200, {
          testCases: [],
          executionTimeMs: 100,
          numberOfTests: 10,
          numberOfFailures: 2,
          pageNumber: 0,
          pageSize: 10,
          numberOfElements: 10,
        });

        const result = await executeTool("get_test_results", {
          projectId: "design-project1",
          unpaged: true,
        }, client);

        expect(result.content[0].type).toBe("text");
      });
    });

    describe("get_test_results_by_table", () => {
      it("should execute openl_get_test_results_by_table reading the results as JSON", async () => {
        // Start test execution
        const sessionHeaders = {
          "x-test-execution-id": "test-session-by-table",
          "set-cookie": ["JSESSIONID=bytable789; Path=/"],
        } as any;

        mockAxios.onPost(`/projects/${encodedProjectId}/tests/run`).reply(202, {
          status: "accepted",
        }, sessionHeaders);

        await executeTool("start_project_tests", {
          projectId: "design-project1",
        }, client);

        // Get results - mock should return results for page 0, empty for page 1+
        const allResults: Types.TestsExecutionSummary = {
          testCases: [
            {
              name: "Test_calculatePremium",
              tableId: "Test_calculatePremium_1234",
              executionTimeMs: 50,
              numberOfTests: 5,
              numberOfFailures: 0,
              testUnits: [],
            },
            {
              name: "Test_calculateDiscount",
              tableId: "Test_calculateDiscount_5678",
              executionTimeMs: 30,
              numberOfTests: 3,
              numberOfFailures: 1,
              testUnits: [],
            },
          ],
          executionTimeMs: 80,
          numberOfTests: 8,
          numberOfFailures: 1,
          pageNumber: 0,
          pageSize: 50,
          numberOfElements: 2,
        };

        mockAxios.onGet(`/projects/${encodedProjectId}/tests/summary`).reply((config) => {
          expect(config.headers).toHaveProperty("Cookie", "JSESSIONID=bytable789");
          expect(config.headers).toHaveProperty("Accept", "application/json");

          // Return empty results for pages > 0 to stop pagination
          const page = config.params?.page ?? 0;
          if (page > 0) {
            return [200, {
              testCases: [],
              executionTimeMs: 80,
              numberOfTests: 8,
              numberOfFailures: 1,
              pageNumber: page,
              pageSize: 50,
              numberOfElements: 0,
            }];
          }

          return [200, allResults];
        });

        const result = await executeTool("get_test_results_by_table", {
          projectId: "design-project1",
          tableId: "Test_calculatePremium_1234",
        }, client);

        expect(result.content[0].type).toBe("text");
        const text = result.content[0].text;
        // Should only contain results for the specified table
        expect(text).toContain("Test_calculatePremium");
        expect(text).not.toContain("Test_calculateDiscount");
      });

      it("should error when no test session exists", async () => {
        // Client checks for headers first and throws before API call
        // But error gets wrapped by tool handler, so just check that error is thrown
        await expect(
          executeTool("get_test_results_by_table", {
            projectId: "design-project1",
            tableId: "Test_calculatePremium_1234",
          }, client)
        ).rejects.toThrow();
      });

      it("should error when tableId is missing", async () => {
        await expect(
          executeTool("get_test_results_by_table", {
            projectId: "design-project1",
            // Missing tableId
          }, client)
          ).rejects.toThrow(/Invalid arguments for get_test_results_by_table.*tableId/s);
      });

      it("should support pagination parameters", async () => {
        // Start test execution
        mockAxios.onPost(`/projects/${encodedProjectId}/tests/run`).reply(202, {
          status: "accepted",
        }, { "x-test-execution-id": "test-session-by-table-pagination" });

        await executeTool("start_project_tests", {
          projectId: "design-project1",
        }, client);

        const allResults: Types.TestsExecutionSummary = {
          testCases: [
            {
              name: "Test_calculatePremium",
              tableId: "Test_calculatePremium_1234",
              executionTimeMs: 50,
              numberOfTests: 5,
              numberOfFailures: 0,
              testUnits: [],
            },
          ],
          executionTimeMs: 50,
          numberOfTests: 5,
          numberOfFailures: 0,
          pageNumber: 0,
          pageSize: 50,
        };

        mockAxios.onGet(`/projects/${encodedProjectId}/tests/summary`, {
          params: { failures: 5, page: 0, size: 50 },
        }).reply(200, allResults);

        const result = await executeTool("get_test_results_by_table", {
          projectId: "design-project1",
          tableId: "Test_calculatePremium_1234",
          page: 0,
          size: 50,
        }, client);

        expect(result.content[0].type).toBe("text");
      });
    });

    describe("Project Files (BETA) Tools", () => {
      it("openl_read_project_file returns UTF-8 text content verbatim", async () => {
        mockAxios.onGet("/projects/p1/files/readme.md").reply(200, "# Title\nbody", {
          "content-type": "text/markdown",
          "content-disposition": "attachment; filename=readme.md",
        });

        const result = await executeTool("read_project_file", {
          projectId: "p1",
          path: "readme.md",
        }, client);

        expect(result.content[0].text).toBe("# Title\nbody");
      });

      it("openl_read_project_file base64-encodes binary content with metadata", async () => {
        const binary = Buffer.from([0x00, 0x01, 0x02, 0xff, 0x00]);
        mockAxios.onGet("/projects/p1/files/data.bin").reply(200, binary, {
          "content-type": "application/octet-stream",
          "content-disposition": "attachment",
        });

        const result = await executeTool("read_project_file", {
          projectId: "p1",
          path: "data.bin",
          response_format: "json",
        }, client);

        const parsed = JSON.parse(result.content[0].text);
        expect(parsed.data.encoding).toBe("base64");
        expect(parsed.data.byteLength).toBe(5);
        expect(parsed.data.content).toBe(binary.toString("base64"));
      });

      it("openl_read_project_file keeps arbitrary MCP binary content in a lossless text envelope", async () => {
        const binary = Buffer.from([0x00, 0x01, 0x02, 0xff]);
        mockAxios.onGet("/projects/p1/files/data.bin").reply(200, binary, {
          "content-type": "application/octet-stream",
          "content-disposition": "attachment",
        });

        const result = await executeTool("read_project_file", {
          projectId: "p1",
          path: "data.bin",
        }, client, { signal: new AbortController().signal });

        expect(JSON.parse(result.content[0].text).data).toMatchObject({
          path: "data.bin",
          mimeType: "application/octet-stream",
          byteLength: 4,
          returnedBytes: 4,
          content: binary.toString("base64"),
        });
        expect(result.content).toHaveLength(1);
        expect(result.structuredContent).toBeUndefined();
      });

      it("openl_read_project_file applies a client-side byte range", async () => {
        mockAxios.onGet("/projects/p1/files/nums.txt").reply(200, "0123456789", {
          "content-type": "text/plain",
          "content-disposition": "attachment",
        });

        const result = await executeTool("read_project_file", {
          projectId: "p1",
          path: "nums.txt",
          offset: 2,
          length: 3,
        }, client);

        expect(result.content[0].text).toBe("234");
      });

      it("openl_read_project_file caps oversized text at 25K and appends a continuation cursor", async () => {
        const big = "A".repeat(30000); // > 25K chars
        mockAxios.onGet("/projects/p1/files/big.txt").reply(200, big, {
          "content-type": "text/plain",
          "content-disposition": "attachment",
        });

        const result = await executeTool("read_project_file", { projectId: "p1", path: "big.txt" }, client);
        const text = result.content[0].text;
        expect(text.startsWith("A".repeat(100))).toBe(true);
        // First 25000 chars of content + a continuation note pointing at the next byte offset.
        expect(text).toContain("continue with offset=25000");
        expect(text.length).toBeLessThan(25000 + 300);
      });

      it("openl_read_project_file returns a folder listing as JSON", async () => {
        const listing = JSON.stringify([
          { path: "a.xlsx", name: "a.xlsx", type: "file" },
          { path: "sub", name: "sub", type: "folder" },
        ]);
        mockAxios.onGet(/\/projects\/p1\/files\/?/).reply(200, listing, {
          "content-type": "application/json",
        });

        const result = await executeTool("read_project_file", {
          projectId: "p1",
          path: "",
          recursive: true,
          response_format: "json",
        }, client);

        const parsed = JSON.parse(result.content[0].text);
        expect(Array.isArray(parsed.data)).toBe(true);
        expect(parsed.data).toHaveLength(2);
      });

      it("openl_read_project_file returns file metadata for view=meta", async () => {
        const meta = JSON.stringify({ path: "a.xlsx", name: "a.xlsx", type: "file", size: 100 });
        mockAxios.onGet("/projects/p1/files/a.xlsx").reply(200, meta, {
          "content-type": "application/json",
        });

        const result = await executeTool("read_project_file", {
          projectId: "p1",
          path: "a.xlsx",
          view: "meta",
          response_format: "json",
        }, client);

        const parsed = JSON.parse(result.content[0].text);
        expect(parsed.data.size).toBe(100);
      });

      it("openl_write_project_file decodes base64 and reports an uncommitted working-copy write", async () => {
        let captured: { data?: unknown; params?: Record<string, unknown> } = {};
        mockAxios.onPost("/projects/p1/files/docs/new.md").reply((config) => {
          captured = config;
          return [201, {}];
        });

        const result = await executeTool("write_project_file", {
          projectId: "p1",
          path: "docs/new.md",
          content: Buffer.from("hello").toString("base64"),
          encoding: "base64",
          response_format: "json",
        }, client);

        expect(Buffer.from(captured.data as Buffer).toString("utf-8")).toBe("hello");
        // createFolders default (true) is materialized by the handler.
        expect(captured.params?.createFolders).toBe(true);

        const parsed = JSON.parse(result.content[0].text);
        expect(parsed.data.success).toBe(true);
        expect(parsed.data.bytesWritten).toBe(5);
        expect(parsed.data.committed).toBe(false);
      });

      it("openl_write_project_file advertises and accepts a JSON Schema base64 blob", async () => {
        const tool = getAllTools().find((candidate) => candidate.name === "write_project_file");
        const alternatives = tool?.inputSchema.anyOf as Array<Record<string, unknown>>;
        expect(tool?.inputSchema.type).toBe("object");
        expect(alternatives).toHaveLength(3);

        const blobAlternative = alternatives.find((alternative) =>
          Object.hasOwn(alternative.properties as object, "blob"));
        const blobProperties = blobAlternative?.properties as Record<string, Record<string, unknown>>;
        expect(blobProperties.blob).toMatchObject({
          type: "string",
          contentEncoding: "base64",
          contentMediaType: "application/octet-stream",
        });
        expect(blobAlternative?.required).toEqual(expect.arrayContaining(["blob"]));
        expect(blobProperties).not.toHaveProperty("content");
        expect(alternatives.every((alternative) => alternative.additionalProperties === false)).toBe(true);

        let written: Buffer | undefined;
        mockAxios.onPost("/projects/p1/files/rules/model.xlsx").reply((config) => {
          written = config.data as Buffer;
          return [201, {}];
        });
        const bytes = Buffer.from([0x50, 0x4b, 0x03, 0x04]);
        await executeTool("write_project_file", {
          projectId: "p1",
          path: "rules/model.xlsx",
          blob: bytes.toString("base64"),
        }, client);

        expect(written).toEqual(bytes);
      });

      it("openl_write_project_file accepts whitespace-wrapped legacy base64 content", async () => {
        let written: Buffer | undefined;
        mockAxios.onPost("/projects/p1/files/rules/model.xlsx").reply((config) => {
          written = config.data as Buffer;
          return [201, {}];
        });
        const bytes = Buffer.from([0x50, 0x4b, 0x03, 0x04, 0xff, 0x00]);
        const base64 = bytes.toString("base64");
        const wrapped = `${base64.slice(0, 4)}\n${base64.slice(4)}\n`;

        await executeTool("write_project_file", {
          projectId: "p1",
          path: "rules/model.xlsx",
          content: wrapped,
          encoding: "base64",
        }, client);

        expect(written).toEqual(bytes);
      });

      it("openl_write_project_file rejects invalid or ambiguous binary inputs", async () => {
        await expect(executeTool("write_project_file", {
          projectId: "p1",
          path: "bad.bin",
          blob: "not base64!",
        }, client)).rejects.toThrow(/base64/i);

        await expect(executeTool("write_project_file", {
          projectId: "p1",
          path: "ambiguous.bin",
          content: "text",
          blob: Buffer.from("bytes").toString("base64"),
        }, client)).rejects.toThrow(/invalid arguments/i);

        await expect(executeTool("write_project_file", {
          projectId: "p1",
          path: "missing.bin",
        }, client)).rejects.toThrow(/invalid arguments/i);
        expect(mockAxios.history.post).toHaveLength(0);
      });

      it("openl_write_project_file with 'message' commits the write (save) and reports committed:true", async () => {
        mockAxios.onPost("/projects/p1/files/docs/x.md").reply(201, {});
        // saveProject(): GET project (EDITING, design) -> PATCH commit
        mockAxios.onGet("/projects/p1").reply(200, { id: "p1", name: "P", status: "EDITING", repository: "design" });
        let patched: Record<string, unknown> = {};
        mockAxios.onPatch("/projects/p1").reply((config) => {
          patched = JSON.parse(config.data);
          return [204];
        });

        const result = await executeTool("write_project_file", {
          projectId: "p1",
          path: "docs/x.md",
          content: "hello",
          message: "add docs/x.md",
          response_format: "json",
        }, client);

        expect(patched.comment).toBe("add docs/x.md"); // committed via save (PATCH)
        const parsed = JSON.parse(result.content[0].text);
        expect(parsed.data.committed).toBe(true);
        expect(parsed.data.message).toContain("committed");
      });

      it("openl_delete_project_file deletes and reports success", async () => {
        mockAxios.onDelete("/projects/p1/files/old.txt").reply(204);

        const result = await executeTool("delete_project_file", {
          projectId: "p1",
          path: "old.txt",
          response_format: "json",
        }, client);

        const parsed = JSON.parse(result.content[0].text);
        expect(parsed.data.success).toBe(true);
        expect(parsed.data.path).toBe("old.txt");
      });

      it("openl_search_project_files builds the query body and returns matches", async () => {
        const nodes = [{ path: "rules/config.xml", name: "config.xml", type: "file" }];
        let body: Record<string, unknown> = {};
        mockAxios.onPost("/projects/p1/file-search").reply((config) => {
          body = JSON.parse(config.data);
          return [200, nodes];
        });

        const result = await executeTool("search_project_files", {
          projectId: "p1",
          pattern: "**/*.xml",
          content: "premium",
          response_format: "json",
        }, client);

        expect(body).toEqual({ pattern: "**/*.xml", content: "premium" });
        const parsed = JSON.parse(result.content[0].text);
        expect(parsed.data).toHaveLength(1);
        expect(parsed.data[0].name).toBe("config.xml");
      });

      it("openl_search_project_files paginates the match set client-side (limit/offset)", async () => {
        const nodes = Array.from({ length: 5 }, (_, i) => ({ path: `f${i}.xlsx`, name: `f${i}.xlsx`, type: "file" }));
        mockAxios.onPost("/projects/p1/file-search").reply(200, nodes);

        const page1 = JSON.parse((await executeTool("search_project_files", {
          projectId: "p1", pattern: "**/*.xlsx", limit: 2, offset: 0, response_format: "json",
        }, client)).content[0].text);
        expect(page1.data).toHaveLength(2);
        expect(page1.data[0].name).toBe("f0.xlsx");
        expect(page1.pagination).toMatchObject({ limit: 2, offset: 0, total_count: 5, has_more: true });

        const page3 = JSON.parse((await executeTool("search_project_files", {
          projectId: "p1", pattern: "**/*.xlsx", limit: 2, offset: 4, response_format: "json",
        }, client)).content[0].text);
        expect(page3.data).toHaveLength(1);
        expect(page3.data[0].name).toBe("f4.xlsx");
        expect(page3.pagination).toMatchObject({ total_count: 5, has_more: false });
      });

      it("openl_copy_project_file copies and reports source/destination", async () => {
        let body: Record<string, unknown> = {};
        mockAxios.onPost("/projects/p1/file-copy").reply((config) => {
          body = JSON.parse(config.data);
          return [201];
        });

        const result = await executeTool("copy_project_file", {
          projectId: "p1",
          sourcePath: "rules/M.xlsx",
          destinationPath: "rules/M-copy.xlsx",
          response_format: "json",
        }, client);

        expect(body).toEqual({ sourcePath: "rules/M.xlsx", destinationPath: "rules/M-copy.xlsx" });
        const parsed = JSON.parse(result.content[0].text);
        expect(parsed.data.success).toBe(true);
        expect(parsed.data.destinationPath).toBe("rules/M-copy.xlsx");
      });

      it("openl_move_project_file moves and reports source/destination", async () => {
        let body: Record<string, unknown> = {};
        mockAxios.onPost("/projects/p1/file-move").reply((config) => {
          body = JSON.parse(config.data);
          return [204];
        });

        const result = await executeTool("move_project_file", {
          projectId: "p1",
          sourcePath: "rules/M.xlsx",
          destinationPath: "legacy/M.xlsx",
          response_format: "json",
        }, client);

        expect(body).toEqual({ sourcePath: "rules/M.xlsx", destinationPath: "legacy/M.xlsx" });
        const parsed = JSON.parse(result.content[0].text);
        expect(parsed.data.success).toBe(true);
        expect(parsed.data.sourcePath).toBe("rules/M.xlsx");
      });

      it("openl_read_project_file forwards version and branch query params", async () => {
        let seenParams: Record<string, unknown> | undefined;
        mockAxios.onGet("/projects/p1/files/a.xlsx").reply((config) => {
          seenParams = config.params;
          return [200, "x", { "content-type": "text/plain", "content-disposition": "attachment" }];
        });

        await executeTool("read_project_file", {
          projectId: "p1",
          path: "a.xlsx",
          version: "rev1",
          branch: "dev",
        }, client);

        expect(seenParams).toMatchObject({ version: "rev1", branch: "dev" });
      });

      it("openl_read_project_file with encoding='utf-8' returns binary-ish bytes as raw text (no base64 envelope)", async () => {
        const bytes = Buffer.from([0x41, 0x00, 0x42]); // 'A', NUL, 'B' -> looksBinary true
        mockAxios.onGet("/projects/p1/files/data.bin").reply(200, bytes, {
          "content-type": "application/octet-stream",
          "content-disposition": "attachment",
        });

        const result = await executeTool("read_project_file", {
          projectId: "p1",
          path: "data.bin",
          encoding: "utf-8",
        }, client);

        expect(result.content[0].text).toBe(bytes.toString("utf-8"));
        expect(result.content[0].text).not.toContain('"encoding":"base64"');
      });

      it("openl_read_project_file does not truncate a large base64 binary payload", async () => {
        // 30 KB of NUL bytes -> base64 ~40 KB, well past the 25k markdown cap.
        const big = Buffer.alloc(30000, 0);
        mockAxios.onGet("/projects/p1/files/big.bin").reply(200, big, {
          "content-type": "application/octet-stream",
          "content-disposition": "attachment",
        });

        // Default JSON must carry the complete base64 envelope without applying
        // the normal text-response character limit.
        const result = await executeTool("read_project_file", {
          projectId: "p1",
          path: "big.bin",
        }, client);

        const parsed = JSON.parse(result.content[0].text);
        expect(parsed.data.encoding).toBe("base64");
        expect(parsed.data.byteLength).toBe(30000);
        expect(Buffer.from(parsed.data.content, "base64").length).toBe(30000);
      });

      it("openl_read_project_file download=true returns a base64 ZIP and passes download=true", async () => {
        const zip = Buffer.from("PKfakezip");
        let seenParams: Record<string, unknown> | undefined;
        mockAxios.onGet(/\/projects\/p1\/files\//).reply((config) => {
          seenParams = config.params;
          return [200, zip, { "content-type": "application/zip", "content-disposition": "attachment; filename=rules.zip" }];
        });

        const result = await executeTool("read_project_file", {
          projectId: "p1",
          path: "rules/",
          download: true,
          response_format: "json",
        }, client);

        expect(seenParams?.download).toBe("true");
        const parsed = JSON.parse(result.content[0].text);
        expect(parsed.data.encoding).toBe("base64");
        expect(parsed.data.content).toBe(zip.toString("base64"));
      });

      it("openl_read_project_file auto-encoding boundary: 1/10 control chars stays text, 2/10 becomes base64", async () => {
        const oneCtrl = Buffer.concat([Buffer.from("A".repeat(9)), Buffer.from([0x01])]); // 1/10 -> text
        mockAxios.onGet("/projects/p1/files/a.txt").reply(200, oneCtrl, {
          "content-type": "application/octet-stream",
          "content-disposition": "attachment",
        });
        const textResult = await executeTool("read_project_file", { projectId: "p1", path: "a.txt" }, client);
        expect(textResult.content[0].text).toBe(oneCtrl.toString("utf-8"));

        mockAxios.reset();
        const twoCtrl = Buffer.concat([Buffer.from("A".repeat(8)), Buffer.from([0x01, 0x01])]); // 2/10 -> binary
        mockAxios.onGet("/projects/p1/files/b.txt").reply(200, twoCtrl, {
          "content-type": "application/octet-stream",
          "content-disposition": "attachment",
        });
        const binResult = await executeTool("read_project_file", { projectId: "p1", path: "b.txt", response_format: "json" }, client);
        expect(JSON.parse(binResult.content[0].text).data.encoding).toBe("base64");
      });

      it("openl_write_project_file create POSTs (no conflictPolicy param) and forwards branch", async () => {
        let captured: { params?: Record<string, unknown> } = {};
        mockAxios.onPost("/projects/p1/files/docs/x.md").reply((config) => {
          captured = config;
          return [201, {}];
        });

        const result = await executeTool("write_project_file", {
          projectId: "p1",
          path: "docs/x.md",
          content: "hi",
          branch: "dev",
          response_format: "json",
        }, client);

        // conflictPolicy must NOT be sent to the backend (it ignores it for single files).
        expect(captured.params).not.toHaveProperty("conflictPolicy");
        expect(captured.params?.branch).toBe("dev");
        const parsed = JSON.parse(result.content[0].text);
        expect(parsed.data.action).toBe("created");
        expect(parsed.data.branch).toBe("dev");
      });

      it("openl_write_project_file conflictPolicy=OVERWRITE replaces an existing file via PUT", async () => {
        let putBody = "";
        mockAxios.onPost("/projects/p1/files/docs/x.md").reply(409, { message: "already exists" });
        mockAxios.onPut("/projects/p1/files/docs/x.md").reply((config) => {
          putBody = Buffer.from(config.data as Buffer).toString("utf-8");
          return [204];
        });

        const result = await executeTool("write_project_file", {
          projectId: "p1",
          path: "docs/x.md",
          content: "v2-overwrite",
          conflictPolicy: "OVERWRITE",
          response_format: "json",
        }, client);

        expect(putBody).toBe("v2-overwrite");
        const parsed = JSON.parse(result.content[0].text);
        expect(parsed.data.success).toBe(true);
        expect(parsed.data.action).toBe("overwritten");
      });

      it("openl_write_project_file conflictPolicy=SKIP leaves an existing file unchanged (no PUT)", async () => {
        let putCalled = false;
        mockAxios.onPost("/projects/p1/files/docs/x.md").reply(409, { message: "already exists" });
        mockAxios.onPut("/projects/p1/files/docs/x.md").reply(() => {
          putCalled = true;
          return [204];
        });

        const result = await executeTool("write_project_file", {
          projectId: "p1",
          path: "docs/x.md",
          content: "v2",
          conflictPolicy: "SKIP",
          response_format: "json",
        }, client);

        expect(putCalled).toBe(false);
        const parsed = JSON.parse(result.content[0].text);
        expect(parsed.data.success).toBe(true);
        expect(parsed.data.skipped).toBe(true);
        expect(parsed.data.written).toBe(false);
      });

      it("openl_write_project_file rejects invalid base64 content before calling the API", async () => {
        let called = false;
        mockAxios.onPost("/projects/p1/files/x.bin").reply(() => {
          called = true;
          return [201, {}];
        });

        await expect(
          executeTool("write_project_file", {
            projectId: "p1",
            path: "x.bin",
            content: "not valid base64 @@@!!!",
            encoding: "base64",
          }, client)
        ).rejects.toThrow(/base64/i);
        expect(called).toBe(false);
      });

      it("openl_write_project_file surfaces a 409 conflict as an actionable error", async () => {
        mockAxios.onPost("/projects/p1/files/exists.md").reply(409, { message: "already exists" });

        await expect(
          executeTool("write_project_file", { projectId: "p1", path: "exists.md", content: "x" }, client)
        ).rejects.toThrow(/already exists|conflictPolicy/);
      });

      it("openl_search_project_files forwards extensions/type/scope=ANCESTORS/recursive/from/version", async () => {
        let body: Record<string, unknown> = {};
        mockAxios.onPost("/projects/p1/file-search").reply((config) => {
          body = JSON.parse(config.data);
          return [200, []];
        });

        await executeTool("search_project_files", {
          projectId: "p1",
          extensions: ["xlsx", "xml"],
          type: "FOLDER",
          scope: "ANCESTORS",
          recursive: true,
          from: "rules",
          version: "abc123",
        }, client);

        expect(body).toEqual({
          extensions: ["xlsx", "xml"],
          type: "FOLDER",
          scope: "ANCESTORS",
          recursive: true,
          from: "rules",
          version: "abc123",
        });
      });

      it("openl_copy_project_file surfaces a 409 destination conflict as an actionable error", async () => {
        mockAxios.onPost("/projects/p1/file-copy").reply(409, { message: "exists" });

        await expect(
          executeTool("copy_project_file", {
            projectId: "p1",
            sourcePath: "a.xlsx",
            destinationPath: "b.xlsx",
          }, client)
        ).rejects.toThrow(/already exists|different destinationPath/);
      });

      it("openl_move_project_file surfaces a 409 destination conflict as an actionable error", async () => {
        mockAxios.onPost("/projects/p1/file-move").reply(409, { message: "exists" });

        await expect(
          executeTool("move_project_file", {
            projectId: "p1",
            sourcePath: "a.xlsx",
            destinationPath: "b.xlsx",
          }, client)
        ).rejects.toThrow(/already exists|different destinationPath/);
      });

      it("openl_read_project_file rejects a path-traversal attempt", async () => {
        await expect(
          executeTool("read_project_file", { projectId: "p1", path: "rules/../../etc/passwd" }, client)
        ).rejects.toThrow(/project-relative|not allowed/);
      });

      it("openl_read_project_file requires projectId", async () => {
        await expect(
          executeTool("read_project_file", { path: "x" }, client)
        ).rejects.toThrow(/projectId/);
      });
    });
  });
});

// These suites were migrated from the former tests/mcp-server.test.ts. They use
// their OWN client/mockAxios so they stay isolated from the session state that
// the "Test Execution Tools" suite above stores on its shared client.
describe("Tool Handler Integration Tests — status, edits, creation & trace", () => {
  let client: OpenLClient;
  let mockAxios: MockAdapter;

  beforeAll(() => {
    const config: OpenLConfig = {
      baseUrl: "http://localhost:8080",
      personalAccessToken: "openl_pat_test",
    };

    client = new OpenLClient(config);
    // @ts-ignore Access private axios instance for mocking in integration tests
    mockAxios = new MockAdapter(client.axiosInstance);

    registerAllTools();
  });

  beforeEach(() => {
    mockAxios.reset();
  });

  afterAll(() => {
    mockAxios.restore();
  });

  describe("Project Status", () => {
    /** Studio answers the status as the `compileStatus` of the project it returns for `include=status`. */
    const withStatus = (status: object): object => ({ compileStatus: status });

    it("starts lazy compilation on idle by default and returns the resulting status", async () => {
      const encoded = encodeProjectPath(projectId);
      mockAxios
        .onGet(`/projects/${encoded}`)
        .replyOnce(200, withStatus({ projectId, branch: "main", compileState: "idle" }))
        .onGet(`/projects/${encoded}`)
        .replyOnce(200, withStatus({
          projectId,
          branch: "main",
          compileState: "ok",
          compilation: {
            messages: { items: [], total: 0, errors: 0, warnings: 0 },
            modules: { total: 1, compiled: 1 },
            tests: { total: 34 },
          },
        }));
      mockAxios.onGet(`/projects/${encoded}/tables`, {
        params: { offset: 0, size: 1 },
      }).reply(200, {
        content: [],
        pageNumber: 0,
        pageSize: 1,
        numberOfElements: 0,
        total: 0,
      });

      const result = await executeTool(
        "project_status",
        { projectId, response_format: "json" },
        client,
      );

      const response = JSON.parse(result.content[0].text).data;
      expect(response.compileState).toBe("ok");
      expect(response.compilation.tests.total).toBe(34);
      expect(mockAxios.history.get.filter((request) => request.url?.endsWith("/tables"))).toHaveLength(1);
      // The default path above starts compilation, so MCP clients must not be
      // told that the tool is unconditionally read-only.
      expect(getAllTools().find((tool) => tool.name === "project_status")?.annotations?.readOnlyHint).toBe(false);
    });

    it("keeps wait=false as a read-only idle snapshot with actionable guidance", async () => {
      const encoded = encodeProjectPath(projectId);
      mockAxios.onGet(`/projects/${encoded}`).reply(200, withStatus({
        projectId,
        branch: "main",
        compileState: "idle",
      }));

      const result = await executeTool(
        "project_status",
        { projectId, wait: false, response_format: "json" },
        client,
      );

      const response = JSON.parse(result.content[0].text).data;
      expect(response.compileState).toBe("idle");
      expect(response.note).toContain("wait=true");
      expect(mockAxios.history.get.some((request) => request.url?.endsWith("/tables"))).toBe(false);
    });

    it("should execute openl_project_status and surface diagnostics on errors", async () => {
      const encoded = encodeProjectPath(projectId);
      const fixture: ProjectStatusView = {
        projectId: { repository: "design", projectName: "insurance-rules" },
        branch: "main",
        compileState: "errors",
        compilation: {
          messages: {
            items: [
              { id: 1, summary: "Datatype 'Driver' not found", severity: "ERROR" },
            ],
            total: 1,
            errors: 1,
            warnings: 0,
          },
          modules: { total: 1, compiled: 0 },
          tests: { total: 0 },
        },
      };
      mockAxios.onGet(`/projects/${encoded}`).reply(200, withStatus(fixture));

      const result = await executeTool(
        "project_status",
        { projectId, response_format: "json" },
        client
      );
      expect(result.content[0].text).toContain("\"errors\"");
      expect(result.content[0].text).toContain("Datatype 'Driver' not found");
    });

    it("should trim messages.items when compileState is ok", async () => {
      const encoded = encodeProjectPath(projectId);
      // Backend may return INFO items even when compilation succeeds; the handler should
      // strip the items[] list while preserving the counts.
      const fixture: ProjectStatusView = {
        projectId: { repository: "design", projectName: "insurance-rules" },
        branch: "main",
        compileState: "ok",
        compilation: {
          messages: {
            items: [
              { id: 1, summary: "INFO_ITEM_SHOULD_BE_TRIMMED", severity: "INFO" },
            ],
            total: 1,
            errors: 0,
            warnings: 0,
          },
          modules: { total: 2, compiled: 2, compiledModules: ["A", "B"] },
          tests: { total: 7 },
        },
      };
      mockAxios.onGet(`/projects/${encoded}`).reply(200, withStatus(fixture));

      const result = await executeTool(
        "project_status",
        { projectId, response_format: "json" },
        client
      );
      expect(result.content[0].text).toContain("\"ok\"");
      expect(result.content[0].text).not.toContain("INFO_ITEM_SHOULD_BE_TRIMMED");
      // Counts and module info are preserved
      expect(result.content[0].text).toContain("\"compiled\": 2");
      expect(result.content[0].text).toContain("\"total\": 7");
    });

    it("accepts the status of the branch the project is opened on", async () => {
      const encoded = encodeProjectPath(projectId);
      mockAxios.onGet(`/projects/${encoded}`).reply(200, withStatus({
        projectId,
        branch: "develop",
        compileState: "ok",
        compilation: {
          messages: { items: [], total: 0, errors: 0, warnings: 0 },
          modules: { total: 1, compiled: 1 },
          tests: { total: 0 },
        },
      }));

      const result = await executeTool(
        "project_status",
        { projectId, branch: "develop", wait: false, response_format: "json" },
        client
      );
      expect(result.content[0].text).toContain("\"ok\"");
    });

    it("should validate projectId for openl_project_status", async () => {
      await expect(executeTool("project_status", {}, client)).rejects.toThrow(/projectId/);
    });

    it("should sort compilation.messages.items by severity (ERROR → WARN → INFO)", async () => {
      const encoded = encodeProjectPath(projectId);
      // Backend returns items in id-ascending order — WARNs first, then ERRORs.
      const fixture: ProjectStatusView = {
        projectId,
        branch: "master",
        compileState: "errors",
        compilation: {
          messages: {
            items: [
              { id: 1, summary: "WARN_FIRST", severity: "WARN" },
              { id: 2, summary: "INFO_SECOND", severity: "INFO" },
              { id: 3, summary: "ERROR_THIRD", severity: "ERROR" },
              { id: 4, summary: "WARN_FOURTH", severity: "WARN" },
              { id: 5, summary: "ERROR_FIFTH", severity: "ERROR" },
            ],
            total: 5, errors: 2, warnings: 2,
          },
          modules: { total: 1, compiled: 0 },
          tests: { total: 0 },
        },
      };
      mockAxios.onGet(`/projects/${encoded}`).reply(200, withStatus(fixture));

      const result = await executeTool(
        "project_status",
        { projectId, response_format: "json" },
        client,
      );
      const text = result.content[0].text;
      // ERRORs should appear before WARNs/INFO in the serialized output.
      const errorThirdAt = text.indexOf("ERROR_THIRD");
      const errorFifthAt = text.indexOf("ERROR_FIFTH");
      const warnFirstAt = text.indexOf("WARN_FIRST");
      const infoSecondAt = text.indexOf("INFO_SECOND");
      expect(errorThirdAt).toBeGreaterThan(-1);
      expect(errorFifthAt).toBeGreaterThan(-1);
      expect(errorThirdAt).toBeLessThan(warnFirstAt);
      expect(errorFifthAt).toBeLessThan(warnFirstAt);
      expect(warnFirstAt).toBeLessThan(infoSecondAt);
    });

    it("should filter compilation.messages.items by severity when 'severity' is passed", async () => {
      const encoded = encodeProjectPath(projectId);
      const fixture: ProjectStatusView = {
        projectId,
        branch: "master",
        compileState: "errors",
        compilation: {
          messages: {
            items: [
              { id: 1, summary: "WARN_X", severity: "WARN" },
              { id: 2, summary: "ERROR_X", severity: "ERROR" },
              { id: 3, summary: "INFO_X", severity: "INFO" },
            ],
            total: 3, errors: 1, warnings: 1,
          },
          modules: { total: 1, compiled: 0 },
          tests: { total: 0 },
        },
      };
      mockAxios.onGet(`/projects/${encoded}`).reply(200, withStatus(fixture));

      const result = await executeTool(
        "project_status",
        { projectId, severity: ["ERROR"], response_format: "json" },
        client,
      );
      const text = result.content[0].text;
      expect(text).toContain("ERROR_X");
      expect(text).not.toContain("WARN_X");
      expect(text).not.toContain("INFO_X");
    });

    it("should cap items with maxMessages after sorting", async () => {
      const encoded = encodeProjectPath(projectId);
      const fixture: ProjectStatusView = {
        projectId,
        branch: "master",
        compileState: "errors",
        compilation: {
          messages: {
            items: [
              { id: 1, summary: "WARN_A", severity: "WARN" },
              { id: 2, summary: "ERROR_B", severity: "ERROR" },
              { id: 3, summary: "WARN_C", severity: "WARN" },
              { id: 4, summary: "ERROR_D", severity: "ERROR" },
            ],
            total: 4, errors: 2, warnings: 2,
          },
          modules: { total: 1, compiled: 0 },
          tests: { total: 0 },
        },
      };
      mockAxios.onGet(`/projects/${encoded}`).reply(200, withStatus(fixture));

      const result = await executeTool(
        "project_status",
        { projectId, maxMessages: 2, response_format: "json" },
        client,
      );
      const text = result.content[0].text;
      // After sort: [ERROR_B, ERROR_D, WARN_A, WARN_C]. maxMessages=2 keeps the first two.
      expect(text).toContain("ERROR_B");
      expect(text).toContain("ERROR_D");
      expect(text).not.toContain("WARN_A");
      expect(text).not.toContain("WARN_C");
    });

    it("should short-circuit openl_project_status wait=true when the initial state is already terminal", async () => {
      // When the very first HTTP fetch returns a terminal compileState, waitForCompilation
      // returns immediately without ever opening a STOMP subscription.
      const encoded = encodeProjectPath(projectId);
      const fixture: ProjectStatusView = {
        projectId: { repository: "design", projectName: "insurance-rules" },
        branch: "main",
        compileState: "errors",
        compilation: {
          messages: {
            items: [{ id: 7, summary: "Datatype 'Foo' not found", severity: "ERROR" }],
            total: 1,
            errors: 1,
            warnings: 0,
          },
          modules: { total: 1, compiled: 0 },
          tests: { total: 0 },
        },
      };
      mockAxios.onGet(`/projects/${encoded}`).reply(200, withStatus(fixture));

      const result = await executeTool(
        "project_status",
        { projectId, wait: true, response_format: "json" },
        client,
      );
      expect(result.content[0].text).toContain("\"errors\"");
      expect(result.content[0].text).toContain("Datatype 'Foo' not found");
      // Exactly one HTTP fetch — terminal short-circuit means no race-close fetch, no STOMP.
      const statusCalls = mockAxios.history.get.filter((req) => req.params?.include?.includes("status"));
      expect(statusCalls).toHaveLength(1);
    });

    it("should apply the ok-state items trim when wait=true short-circuits with compileState=ok", async () => {
      const encoded = encodeProjectPath(projectId);
      const fixture: ProjectStatusView = {
        projectId: { repository: "design", projectName: "insurance-rules" },
        branch: "main",
        compileState: "ok",
        compilation: {
          messages: {
            items: [{ id: 1, summary: "INFO_SHOULD_BE_TRIMMED", severity: "INFO" }],
            total: 1,
            errors: 0,
            warnings: 0,
          },
          modules: { total: 3, compiled: 3, compiledModules: ["A", "B", "C"] },
          tests: { total: 4 },
        },
      };
      mockAxios.onGet(`/projects/${encoded}`).reply(200, withStatus(fixture));

      const result = await executeTool(
        "project_status",
        { projectId, wait: true, response_format: "json" },
        client,
      );
      expect(result.content[0].text).toContain("\"ok\"");
      expect(result.content[0].text).not.toContain("INFO_SHOULD_BE_TRIMMED");
      expect(result.content[0].text).toContain("\"compiled\": 3");
    });
  });

  describe("Table Edits & ID Stability (EPBDS-16084/16085/16086)", () => {
    it("should execute openl_update_table", async () => {
      const encoded = encodeProjectPath(projectId);
      const view = {
        id: "Rules.xls_1234",
        tableType: "RawSource",
        kind: "Rules",
        name: "calculatePremium",
        source: [[{ value: "Rules void calculatePremium()" }]],
      };
      mockAxios.onPut(`/projects/${encoded}/tables/${encodeURIComponent("Rules.xls_1234")}`).reply(204);
      // The edit tool reads the table back to trigger a recompile.
      mockAxios.onGet(`/projects/${encoded}/tables/${encodeURIComponent("Rules.xls_1234")}`).reply(200, view);

      const result = await executeTool(
        "update_table",
        { projectId, tableId: "Rules.xls_1234", view },
        client
      );
      expect(result.content[0].text).toContain("Successfully updated table");
      // recompile trigger fired: GET on the table after the PUT
      expect(mockAxios.history.get.some((g) => g.url === `/projects/${encoded}/tables/Rules.xls_1234`)).toBe(true);
    });

    it("should execute openl_append_table", async () => {
      const encoded = encodeProjectPath(projectId);
      const appendData = {
        tableType: "RawSource",
        rows: [[{ value: "email" }, { value: "String" }]],
      };
      mockAxios
        .onPost(`/projects/${encoded}/tables/${encodeURIComponent("Customer_1234")}/lines`, appendData)
        .reply(200);
      // The edit tool reads the table back to trigger a recompile.
      mockAxios.onGet(`/projects/${encoded}/tables/${encodeURIComponent("Customer_1234")}`).reply(200, {
        id: "Customer_1234", name: "Customer", tableType: "RawSource", kind: "Datatype",
        source: [[{ value: "name" }, { value: "type" }]],
      });

      const result = await executeTool(
        "append_table",
        { projectId, tableId: "Customer_1234", appendData },
        client
      );
      expect(result.content[0].text).toContain("Successfully appended");
      expect(mockAxios.history.get.some((g) => g.url === `/projects/${encoded}/tables/Customer_1234`)).toBe(true);
    });

    it("should execute openl_append_table with RawSource and report row count", async () => {
      const encoded = encodeProjectPath(projectId);
      const appendData = {
        tableType: "RawSource",
        rows: [
          [{ value: "A1" }, { value: "B1" }],
          [{ value: "A2" }, { value: "B2" }],
        ],
      };
      // RawSource appends probe the raw view first (row-width validation + identity).
      mockAxios.onGet(`/projects/${encoded}/tables/${encodeURIComponent("RawTable_5678")}`).reply(200, {
        id: "RawTable_5678",
        name: "RawTable",
        tableType: "RawSource",
        kind: "Other",
        file: "Rules.xlsx",
        pos: "A1:B3",
        source: [
          [{ value: "H1" }, { value: "H2" }],
          [{ value: "a" }, { value: "b" }],
        ],
      });
      mockAxios
        .onPost(`/projects/${encoded}/tables/${encodeURIComponent("RawTable_5678")}/lines`, appendData)
        .reply(200);

      const result = await executeTool(
        "append_table",
        { projectId, tableId: "RawTable_5678", appendData },
        client
      );
      expect(result.content[0].text).toContain("Successfully appended 2 raw source row(s)");
    });

    it("openl_append_table rejects RawSource rows whose width does not match the table (EPBDS-16085)", async () => {
      const encoded = encodeProjectPath(projectId);
      const tableId = "WideTable_0001";
      mockAxios.onGet(`/projects/${encoded}/tables/${tableId}`).reply(200, {
        id: tableId,
        name: "bankFinancialData",
        tableType: "RawSource",
        kind: "Data",
        file: "Bank.xlsx",
        pos: "A1:E4",
        source: [
          [{ value: "Data BankData bankFinancialData", colspan: 5 }, { covered: true }, { covered: true }, { covered: true }, { covered: true }],
          [{ value: "id" }, { value: "date" }, { value: "a" }, { value: "b" }, { value: "c" }],
          [{ value: "R1" }, { value: "01/01/2024" }, { value: 1 }, { value: 2 }, { value: 3 }],
        ],
      });

      await expect(
        executeTool(
          "append_table",
          {
            projectId,
            tableId,
            appendData: { tableType: "RawSource", rows: [[{ value: "R2" }, { value: "01/01/2025" }]] },
          },
          client
        )
      ).rejects.toThrow(/5 column\(s\) wide.*row 1 has 2 cell\(s\)/);

      // Nothing must be written when validation fails.
      expect(mockAxios.history.post.length).toBe(0);
    });

    it("openl_append_table does not write when the validation probe fails", async () => {
      const encoded = encodeProjectPath(projectId);
      const tableId = "UnavailableTable_0001";
      mockAxios.onGet(`/projects/${encoded}/tables/${tableId}`).reply(503, {
        message: "Compilation model is unavailable",
      });
      mockAxios.onPost(`/projects/${encoded}/tables/${tableId}/lines`).reply(200);

      await expect(executeTool("append_table", {
        projectId,
        tableId,
        appendData: { tableType: "RawSource", rows: [[{ value: "x" }]] },
      }, client)).rejects.toThrow();

      expect(mockAxios.history.post).toHaveLength(0);
    });

    it("openl_append_table accepts RawSource rows that cover every column", async () => {
      const encoded = encodeProjectPath(projectId);
      const tableId = "WideTable_0002";
      mockAxios.onGet(`/projects/${encoded}/tables/${tableId}`).reply(200, {
        id: tableId,
        name: "bankFinancialData",
        tableType: "RawSource",
        kind: "Data",
        file: "Bank.xlsx",
        pos: "A1:C3",
        source: [
          [{ value: "h1" }, { value: "h2" }, { value: "h3" }],
          [{ value: 1 }, { value: 2 }, { value: 3 }],
        ],
      });
      mockAxios.onPost(`/projects/${encoded}/tables/${tableId}/lines`).reply(200);

      const result = await executeTool(
        "append_table",
        {
          projectId,
          tableId,
          appendData: { tableType: "RawSource", rows: [[{ value: "x" }, { value: "y" }, { value: "z" }]] },
        },
        client
      );
      expect(result.content[0].text).toContain("Successfully appended 1 raw source row(s)");
      expect(mockAxios.history.post.length).toBe(1);
    });

    it("openl_append_table returns the table's new id after the edit (EPBDS-16084)", async () => {
      const encoded = encodeProjectPath(projectId);
      const oldId = "aaaa1111aaaa1111";
      const newId = "bbbb2222bbbb2222";
      const tableMeta = { name: "bankFinancialData", tableType: "RawSource", kind: "Data", file: "Bank.xlsx", pos: "A1:E4", source: [[{}, {}, {}, {}, {}]] };

      // Pre-edit probe on the old id; recompile read on the NEW id.
      mockAxios.onGet(`/projects/${encoded}/tables/${oldId}`).reply(200, { id: oldId, ...tableMeta });
      mockAxios.onGet(`/projects/${encoded}/tables/${newId}`).reply(200, { id: newId, ...tableMeta, pos: "A1:E5" });
      // Table listing: before the edit the old id exists, afterwards only the new one.
      mockAxios
        .onGet(`/projects/${encoded}/tables`)
        .replyOnce(200, [{ id: oldId, ...tableMeta }])
        .onGet(`/projects/${encoded}/tables`)
        .replyOnce(200, [{ id: newId, ...tableMeta, pos: "A1:E5" }]);
      mockAxios.onPost(`/projects/${encoded}/tables/${oldId}/lines`).reply(200);

      const result = await executeTool(
        "append_table",
        {
          projectId,
          tableId: oldId,
          appendData: { tableType: "RawSource", rows: [[{ value: "R2" }, { value: "01/01/2025" }, { value: 500 }, { value: 600 }, { value: 700 }]] },
          response_format: "json",
        },
        client
      );

      const payload = JSON.parse(result.content[0].text).data;
      expect(payload.success).toBe(true);
      expect(payload.tableId).toBe(newId);
      expect(payload.tableIdChanged).toBe(true);
      expect(payload.previousTableId).toBe(oldId);
      expect(payload.recompileTriggered).toBe(true);
      expect(payload.note).toContain("changed the table's id");
      // The recompile read targeted the new id (with the old id it would silently 404).
      expect(mockAxios.history.get.some((g) => g.url === `/projects/${encoded}/tables/${newId}`)).toBe(true);
    });

    it("openl_append_table uses the studio-reported new id from the 200/{id}+Location response (EPBDS-16086)", async () => {
      const encoded = encodeProjectPath(projectId);
      const oldId = "aaaa0000aaaa0000";
      const newId = "bbbb1111bbbb1111";
      const tableMeta = { name: "bankFinancialData", tableType: "RawSource", kind: "Data", file: "Bank.xlsx", pos: "A1:E4", source: [[{}, {}, {}, {}, {}]] };

      mockAxios.onGet(`/projects/${encoded}/tables/${oldId}`).reply(200, { id: oldId, ...tableMeta });
      mockAxios.onGet(`/projects/${encoded}/tables/${newId}`).reply(200, { id: newId, ...tableMeta, pos: "A1:E5" });
      // listTables would feed the LEGACY heuristic the OLD id (i.e. "unchanged"),
      // so asserting newId below proves the studio-reported id was used instead.
      mockAxios.onGet(`/projects/${encoded}/tables`).reply(200, [{ id: oldId, ...tableMeta }]);
      mockAxios
        .onPost(`/projects/${encoded}/tables/${oldId}/lines`)
        .reply(200, { id: newId }, { Location: `http://localhost:8080/rest/projects/${encoded}/tables/${newId}` });

      const result = await executeTool(
        "append_table",
        {
          projectId,
          tableId: oldId,
          appendData: { tableType: "RawSource", rows: [[{ value: "R2" }, { value: "01/01/2025" }, { value: 500 }, { value: 600 }, { value: 700 }]] },
          response_format: "json",
        },
        client
      );

      const payload = JSON.parse(result.content[0].text).data;
      expect(payload.success).toBe(true);
      expect(payload.tableId).toBe(newId);
      expect(payload.tableIdChanged).toBe(true);
      expect(payload.previousTableId).toBe(oldId);
      expect(payload.recompileTriggered).toBe(true);
      // Recompile read targeted the studio-reported id.
      expect(mockAxios.history.get.some((g) => g.url === `/projects/${encoded}/tables/${newId}`)).toBe(true);
    });

    it("openl_update_table reads the new id from the Location header when the body has none (EPBDS-16086)", async () => {
      const encoded = encodeProjectPath(projectId);
      const oldId = "cccc0000cccc0000";
      const newId = "dddd1111dddd1111";
      const tableMeta = { tableType: "RawSource", kind: "Data", name: "vehicleRating", file: "Rating.xlsx", pos: "A1:B4", source: [[{}, {}]] };

      // before-snapshot / would-be heuristic input: only the OLD id (→ "unchanged" if it ran)
      mockAxios.onGet(`/projects/${encoded}/tables`).reply(200, [{ id: oldId, ...tableMeta }]);
      mockAxios.onGet(`/projects/${encoded}/tables/${oldId}`).reply(200, { id: oldId, ...tableMeta });
      // 200 with EMPTY body but a Location header pointing at the new id.
      mockAxios
        .onPut(`/projects/${encoded}/tables/${oldId}`)
        .reply(200, "", { Location: `http://example.com/rest/projects/${encoded}/tables/${newId}` });
      mockAxios.onGet(`/projects/${encoded}/tables/${newId}`).reply(200, { id: newId, ...tableMeta, pos: "A1:B5" });

      const view = {
        id: oldId,
        tableType: tableMeta.tableType,
        kind: tableMeta.kind,
        name: tableMeta.name,
        source: [[{ value: 1 }, { value: 2 }]],
      };
      const result = await executeTool(
        "update_table",
        { projectId, tableId: oldId, view, response_format: "json" },
        client
      );

      const payload = JSON.parse(result.content[0].text).data;
      expect(payload.success).toBe(true);
      expect(payload.tableId).toBe(newId);
      expect(payload.tableIdChanged).toBe(true);
      expect(payload.previousTableId).toBe(oldId);
      expect(payload.recompileTriggered).toBe(true);
    });

    it("openl_append_table reports 204 (in-place edit) as an unchanged id", async () => {
      const encoded = encodeProjectPath(projectId);
      const tableId = "eeee2222eeee2222";
      const tableMeta = { name: "ratesTable", tableType: "RawSource", kind: "Data", file: "Rates.xlsx", pos: "A1:C9", source: [[{}, {}, {}]] };

      mockAxios.onGet(`/projects/${encoded}/tables/${tableId}`).reply(200, { id: tableId, ...tableMeta });
      // Same id before and after — an in-place edit that did not relocate the table.
      mockAxios.onGet(`/projects/${encoded}/tables`).reply(200, [{ id: tableId, ...tableMeta }]);
      mockAxios.onPost(`/projects/${encoded}/tables/${tableId}/lines`).reply(204);

      const result = await executeTool(
        "append_table",
        { projectId, tableId, appendData: { tableType: "RawSource", rows: [[{ value: 1 }, { value: 2 }, { value: 3 }]] }, response_format: "json" },
        client
      );

      const payload = JSON.parse(result.content[0].text).data;
      expect(payload.success).toBe(true);
      expect(payload.tableId).toBe(tableId);
      expect(payload.tableIdChanged).toBeUndefined();
      expect(payload.previousTableId).toBeUndefined();
    });

    it("openl_get_table transparently resolves a stale id recorded by a previous edit (EPBDS-16084)", async () => {
      const encoded = encodeProjectPath(projectId);
      const oldId = "cccc3333cccc3333";
      const newId = "dddd4444dddd4444";
      const tableMeta = { name: "driverRating", tableType: "RawSource", kind: "Data", file: "Rating.xlsx", pos: "A1:C4", source: [[{}, {}, {}]] };

      // 1) An append records the old→new rename.
      mockAxios.onGet(`/projects/${encoded}/tables/${oldId}`).reply(200, { id: oldId, ...tableMeta });
      mockAxios.onGet(`/projects/${encoded}/tables/${newId}`).reply(200, { id: newId, ...tableMeta, pos: "A1:C5" });
      mockAxios
        .onGet(`/projects/${encoded}/tables`)
        .replyOnce(200, [{ id: oldId, ...tableMeta }])
        .onGet(`/projects/${encoded}/tables`)
        .replyOnce(200, [{ id: newId, ...tableMeta, pos: "A1:C5" }]);
      mockAxios.onPost(`/projects/${encoded}/tables/${oldId}/lines`).reply(200);
      await executeTool(
        "append_table",
        { projectId, tableId: oldId, appendData: { tableType: "RawSource", rows: [[{ value: 1 }, { value: 2 }, { value: 3 }]] } },
        client
      );

      // 2) A later read that still uses the pre-edit id is resolved automatically.
      mockAxios.reset();
      mockAxios.onGet(`/projects/${encoded}/tables/${oldId}`).reply(404, { message: "The table is not found." });
      mockAxios.onGet(`/projects/${encoded}/tables/${newId}`).reply(200, { id: newId, ...tableMeta, pos: "A1:C5" });

      const result = await executeTool(
        "get_table",
        { projectId, tableId: oldId, response_format: "json" },
        client
      );
      expect(result.content[0].text).toContain("stale");
      expect(result.content[0].text).toContain(newId);
      expect(result.content[1].text).toContain(newId);
    });

    it("openl_update_table retries with the recorded id when the given id is stale (EPBDS-16084)", async () => {
      const encoded = encodeProjectPath(projectId);
      const oldId = "eeee5555eeee5555";
      const newId = "ffff6666ffff6666";
      const tableMeta = { name: "vehicleRating", tableType: "RawSource", kind: "Data", file: "Rating.xlsx", pos: "A1:B4", source: [[{}, {}]] };

      // 1) An append records the old→new rename.
      mockAxios.onGet(`/projects/${encoded}/tables/${oldId}`).reply(200, { id: oldId, ...tableMeta });
      mockAxios.onGet(`/projects/${encoded}/tables/${newId}`).reply(200, { id: newId, ...tableMeta, pos: "A1:B5" });
      mockAxios
        .onGet(`/projects/${encoded}/tables`)
        .replyOnce(200, [{ id: oldId, ...tableMeta }])
        .onGet(`/projects/${encoded}/tables`)
        .replyOnce(200, [{ id: newId, ...tableMeta, pos: "A1:B5" }]);
      mockAxios.onPost(`/projects/${encoded}/tables/${oldId}/lines`).reply(200);
      await executeTool(
        "append_table",
        { projectId, tableId: oldId, appendData: { tableType: "RawSource", rows: [[{ value: 1 }, { value: 2 }]] } },
        client
      );

      // 2) An update that still uses the pre-edit id is retried with the current id.
      mockAxios.reset();
      mockAxios.onPut(`/projects/${encoded}/tables/${oldId}`).reply(404, { message: "The table is not found." });
      mockAxios.onPut(`/projects/${encoded}/tables/${newId}`).reply(204);
      mockAxios.onGet(`/projects/${encoded}/tables/${newId}`).reply(200, { id: newId, ...tableMeta, pos: "A1:B5" });

      const view = {
        id: oldId,
        tableType: tableMeta.tableType,
        kind: tableMeta.kind,
        name: tableMeta.name,
        source: [[{ value: 9 }, { value: 9 }]],
      };
      const result = await executeTool(
        "update_table",
        { projectId, tableId: oldId, view, response_format: "json" },
        client
      );

      const payload = JSON.parse(result.content[0].text).data;
      expect(payload.success).toBe(true);
      expect(payload.tableId).toBe(newId);
      expect(payload.tableIdChanged).toBe(true);
      expect(payload.note).toContain("stale");
      expect(mockAxios.history.put.length).toBe(2);
    });

    it("openl_get_table 404 explains that table ids go stale after edits (EPBDS-16086)", async () => {
      const encoded = encodeProjectPath(projectId);
      mockAxios
        .onGet(`/projects/${encoded}/tables/deadbeefdeadbeef`)
        .reply(404, { message: "The table is not found." });

      await expect(
        executeTool("get_table", { projectId, tableId: "deadbeefdeadbeef" }, client)
      ).rejects.toThrow(/The table is not found.*does NOT mean the edit was rolled back.*openl_list_tables/s);
    });
  });

  describe("Branches & Deployments", () => {
    it("should execute openl_create_project_branch", async () => {
      const encoded = encodeProjectPath(projectId);
      mockAxios.onPost(`/projects/${encoded}/branches`, { branch: "feature/test-branch" }).reply(200);

      const result = await executeTool(
        "create_project_branch",
        { projectId, branchName: "feature/test-branch" },
        client
      );
      expect(result.content[0].text).toContain("Successfully created branch");
    });

    it("should execute openl_list_deployments", async () => {
      mockAxios.onGet("/deployments").reply(200, mockDeployments);

      const result = await executeTool("list_deployments", {
        response_format: "markdown",
      }, client);
      expect(result.content[0].text).toContain("# Deployments");
    });
  });

  describe("Project Creation & Copying", () => {
    it("should create an empty project and return its canonical backend id", async () => {
      mockAxios.onGet("/repos").reply(200, mockRepositories);
      mockAxios.onPut("/repos/design/projects/Offer-CW").reply(200, { revision: "abc123", branch: "main" });
      const canonicalProjectId = Buffer.from("design:Offer-CW").toString("base64");
      mockAxios.onGet("/projects", {
        params: { repository: "design", name: "Offer-CW", offset: 0, size: 200 },
      }).reply(200, {
        content: [{ id: canonicalProjectId, name: "Offer-CW", repository: "design" }],
        pageNumber: 0,
        pageSize: 200,
        numberOfElements: 1,
        total: 1,
      });

      const result = await executeTool(
        "create_project",
        { repository: "Design Repository", projectName: "Offer-CW", response_format: "json" },
        client
      );

      const response = JSON.parse(result.content[0].text).data;
      expect(response).toMatchObject({
        projectId: canonicalProjectId,
        projectName: "Offer-CW",
        revision: "abc123",
      });
      expect(response.projectId).not.toBe(response.projectName);
    });

    it("does not label the project name as projectId when canonical id discovery is unavailable", async () => {
      mockAxios.onGet("/repos").reply(200, mockRepositories);
      mockAxios.onPut("/repos/design/projects/Offer-CW").reply(200, { revision: "abc123", branch: "main" });
      mockAxios.onGet("/projects").reply(503, { message: "Index is refreshing" });

      const result = await executeTool(
        "create_project",
        { repository: "design", projectName: "Offer-CW", response_format: "json" },
        client
      );

      const response = JSON.parse(result.content[0].text).data;
      expect(response).not.toHaveProperty("projectId");
      expect(response.note).toContain("openl_list_projects");
    });

    it("stops canonical-id discovery when Studio repeats a page without total metadata", async () => {
      mockAxios.onGet("/repos").reply(200, mockRepositories);
      mockAxios.onPut("/repos/design/projects/Offer-CW").reply(200, { revision: "abc123", branch: "main" });
      mockAxios.onGet("/projects").reply(200, {
        content: [{ id: "other-id", name: "Other", repository: "design" }],
        pageNumber: 0,
        pageSize: 200,
        numberOfElements: 1,
      });

      const result = await executeTool("create_project", {
        repository: "design",
        projectName: "Offer-CW",
        response_format: "json",
      }, client);

      expect(JSON.parse(result.content[0].text).data).not.toHaveProperty("projectId");
      expect(mockAxios.history.get.filter((request) => request.url === "/projects")).toHaveLength(2);
    });

    it("should reject openl_create_project on name collision (409)", async () => {
      mockAxios.onGet("/repos").reply(200, mockRepositories);
      mockAxios.onPut("/repos/design/projects/Existing").reply(409, { message: "duplicated.project.message" });

      await expect(
        executeTool("create_project", { repository: "design", projectName: "Existing" }, client)
      ).rejects.toThrow(/already exists/i);
    });

    it("should create a blank project on a requested branch", async () => {
      mockAxios.onGet("/repos").reply(200, mockRepositories);
      mockAxios.onPut("/repos/design/projects/Offer-CW").reply(200, { revision: "branch123", branch: "dev" });
      const canonicalProjectId = Buffer.from("design:Offer-CW").toString("base64");
      mockAxios.onGet("/projects", {
        params: { repository: "design", name: "Offer-CW", branch: "dev", offset: 0, size: 200 },
      }).reply(200, {
        content: [{ id: canonicalProjectId, name: "Offer-CW", repository: "design", branch: "dev" }],
        pageNumber: 0,
        pageSize: 200,
        numberOfElements: 1,
        total: 1,
      });

      const result = await executeTool(
        "create_project",
        { repository: "design", projectName: "Offer-CW", branch: "dev", response_format: "json" },
        client,
      );

      expect(JSON.parse(result.content[0].text).data).toMatchObject({
        mode: "create",
        projectId: canonicalProjectId,
        branch: "dev",
        revision: "branch123",
      });
    });

    it("should copy a project through Studio's server-side copy API", async () => {
      mockAxios.onGet("/repos").reply(200, mockRepositories);
      const sourceProjectId = "mapped:Offer-US:opaque-id";
      mockAxios.onGet(/^\/projects\/[^/]+$/).reply(200, { id: "mapped:Offer-US:opaque-id", repository: "mapped" });
      mockAxios.onPost("/repos/design/projects/Offer-CW/from-project", {
        sourceRepositoryId: "mapped",
        sourceProject: sourceProjectId,
      }).reply(200, { revision: "def456", branch: "main" });
      const canonicalProjectId = Buffer.from("design:Offer-CW").toString("base64");
      mockAxios.onGet("/projects", {
        params: { repository: "design", name: "Offer-CW", offset: 0, size: 200 },
      }).reply(200, {
        content: [{ id: canonicalProjectId, name: "Offer-CW", repository: "design" }],
        pageNumber: 0,
        pageSize: 200,
        numberOfElements: 1,
        total: 1,
      });

      const result = await executeTool(
        "create_project",
        { repository: "design", template: sourceProjectId, projectName: "Offer-CW", response_format: "json" },
        client
      );

      const response = JSON.parse(result.content[0].text).data;
      expect(response).toMatchObject({
        mode: "copy",
        projectId: canonicalProjectId,
        revision: "def456",
      });
      expect(response.message).toContain(`Copied '${sourceProjectId}'`);
      expect(mockAxios.history.post.some((p) => p.url === "/repos/design/file-copy")).toBe(false);
    });

    it("should copy a project atomically onto a requested branch", async () => {
      mockAxios.onGet("/repos").reply(200, mockRepositories);
      const sourceProjectId = "mapped:Offer-US:opaque-id";
      mockAxios.onGet(/^\/projects\/[^/]+$/).reply(200, { id: "mapped:Offer-US:opaque-id", repository: "mapped" });
      mockAxios.onPost("/repos/design/projects/Offer-CW/from-project", {
        sourceRepositoryId: "mapped",
        sourceProject: sourceProjectId,
        branch: "dev",
      }).reply(200, { revision: "def456", branch: "dev" });
      const canonicalProjectId = Buffer.from("design:Offer-CW").toString("base64");
      mockAxios.onGet("/projects", {
        params: { repository: "design", name: "Offer-CW", branch: "dev", offset: 0, size: 200 },
      }).reply(200, {
        content: [{ id: canonicalProjectId, name: "Offer-CW", repository: "design", branch: "dev" }],
        pageNumber: 0,
        pageSize: 200,
        numberOfElements: 1,
        total: 1,
      });

      const result = await executeTool(
        "create_project",
        { repository: "design", template: sourceProjectId, projectName: "Offer-CW", branch: "dev", response_format: "json" },
        client
      );

      expect(JSON.parse(result.content[0].text).data).toMatchObject({
        mode: "copy",
        projectId: canonicalProjectId,
        branch: "dev",
        revision: "def456",
      });
      expect(mockAxios.history.post.some((request) => request.url === "/repos/design/file-copy")).toBe(false);
    });

    it("should reject a copy when the destination already exists (409)", async () => {
      mockAxios.onGet("/repos").reply(200, mockRepositories);
      mockAxios.onGet(/^\/projects\/[^/]+$/).reply(200, { id: "mapped:Offer-US:opaque-id", repository: "mapped" });
      mockAxios.onPost("/repos/design/projects/Existing/from-project").reply(409, { message: "duplicated.project.message" });

      await expect(
        executeTool(
          "create_project",
          { repository: "design", template: "mapped:Offer-US:opaque-id", projectName: "Existing" },
          client
        )
      ).rejects.toThrow(/already exists/i);
    });

    it("should reject copying a source that does not exist", async () => {
      mockAxios.onGet("/repos").reply(200, mockRepositories);
      mockAxios.onGet(/^\/projects\/[^/]+$/).reply(404, { message: "Project was not found" });

      await expect(
        executeTool(
          "create_project",
          { repository: "design", template: "missing-project-id", projectName: "NewOne" },
          client
        )
      ).rejects.toThrow(/not found/i);
      expect(mockAxios.history.post).toHaveLength(0);
    });

    it("should validate required params for create", async () => {
      await expect(executeTool("create_project", {}, client)).rejects.toThrow(/repository|projectName/);
    });
  });

  // ---------------------------------------------------------------------------
  // Trace Debug (interactive debugger) — tool-layer glue over the client:
  // breakpoint pre-set on start, resume's status poll loop, inspect's fields
  // trim + highlights merge, and the actionable 404/409 mapping.
  // ---------------------------------------------------------------------------
  describe("Trace Debug tools", () => {
    const encoded = encodeProjectPath(projectId);
    const suspendedStack = {
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

    it("openl_start_trace replaces the breakpoint set BEFORE starting when breakpoints are given", async () => {
      const calls: string[] = [];
      mockAxios.onPut(`/projects/${encoded}/trace/breakpoints`).reply((config) => {
        calls.push(`put:${config.data}`);
        return [204];
      });
      mockAxios.onPost(new RegExp(`/projects/${encoded}/trace\\?`)).reply(() => {
        calls.push("start");
        return [200, suspendedStack];
      });

      const result = await executeTool(
        "start_trace",
        { projectId, tableId: "calc_42", breakpoints: ["MyDT#rule"], response_format: "json" },
        client
      );

      expect(calls[0]).toBe('put:{"uris":["MyDT#rule"]}');
      expect(calls[1]).toBe("start");
      expect(result.content[0].text).toContain("suspended");
    });

    it("openl_start_trace profiling asks the backend for the bounded profile (includeTree=false) and returns it", async () => {
      let params: Record<string, unknown> | undefined;
      mockAxios.onPost(new RegExp(`/projects/${encoded}/trace\\?`)).reply((config) => {
        params = Object.fromEntries(new URLSearchParams(config.url?.split("?")[1] ?? ""));
        return [200, {
          status: "completed",
          frames: [],
          profile: {
            hotspots: [{ uri: "u", name: "VehiclePriceFactor", kind: "spreadsheet", selfMillis: 83.4, totalMillis: 120.1, count: 6 }],
            distinctTables: 42,
            nodeCount: 3571,
            totalMillis: 128.4,
            truncated: true,
          },
        }];
      });

      const result = await executeTool(
        "start_trace",
        { projectId, tableId: "calc_42", stopAtEntry: false, profiling: true, profileTop: 30, response_format: "json" },
        client
      );

      expect(params?.includeTree).toBe("false"); // never pull the >1MB tree by default
      expect(params?.profileTop).toBe("30");
      expect(result.content[0].text).toContain("VehiclePriceFactor");
      expect(result.content[0].text).toContain("hotspots");
    });

    it("openl_start_trace with includeTree: true asks the backend for the full tree", async () => {
      let params: Record<string, unknown> | undefined;
      mockAxios.onPost(new RegExp(`/projects/${encoded}/trace\\?`)).reply((config) => {
        params = Object.fromEntries(new URLSearchParams(config.url?.split("?")[1] ?? ""));
        return [200, { status: "completed", frames: [], tree: { uri: "u", name: "root", kind: "spreadsheet", durationMillis: 5, selfMillis: 1, steps: [] } }];
      });

      await executeTool(
        "start_trace",
        { projectId, tableId: "calc_42", stopAtEntry: false, profiling: true, includeTree: true, response_format: "json" },
        client
      );

      expect(params?.includeTree).toBe("true");
    });

    it("openl_expand_trace_tree loads one level of the lazy tree and flags the next page", async () => {
      let params: Record<string, unknown> | undefined;
      mockAxios.onGet(`/projects/${encoded}/trace/tree/children`).reply((config) => {
        params = config.params;
        return [200, {
          children: [
            { uri: "u1", name: "AgeFactor", kind: "decisionTable", instance: 0, durationMillis: 1, selfMillis: 1, steps: [{ ref: "R1C0", status: "executed", childrenTotal: 0 }] },
            { uri: "u2", name: "AgeFactor", kind: "decisionTable", instance: 1, durationMillis: 1, selfMillis: 1, steps: [{ ref: "R1C0", status: "executed", childrenTotal: 2 }], notRetained: 7 },
          ],
          total: 4,
        }];
      });

      const result = await executeTool(
        "expand_trace_tree",
        { projectId, uri: "P/Rules.xlsx?sheet=Main&range=B2:D8", instance: 0, step: "R2C0", response_format: "json" },
        client
      );

      // uri/instance/step forwarded (URI kept intact for axios to percent-encode).
      expect(params).toMatchObject({ uri: "P/Rules.xlsx?sheet=Main&range=B2:D8", instance: "0", step: "R2C0" });
      const parsed = JSON.parse(result.content[0].text).data;
      expect(parsed.total).toBe(4);
      expect(parsed.children).toHaveLength(2);
      // total 4 > offset 0 + 2 returned → more pages, with the next offset computed.
      expect(parsed.hasMore).toBe(true);
      expect(parsed.nextOffset).toBe(2);
      expect(parsed.children[1].notRetained).toBe(7);
    });

    it("openl_expand_trace_tree does not flag hasMore when the page is the last", async () => {
      mockAxios.onGet(`/projects/${encoded}/trace/tree/children`).reply(200, {
        children: [{ uri: "u1", name: "AgeFactor", kind: "decisionTable", instance: 2, durationMillis: 1, selfMillis: 1, steps: [] }],
        total: 3,
      });

      const result = await executeTool(
        "expand_trace_tree",
        { projectId, uri: "u", instance: 0, step: "R1C0", offset: 2, response_format: "json" },
        client
      );

      const parsed = JSON.parse(result.content[0].text).data;
      expect(parsed.hasMore).toBe(false);
      expect(parsed.nextOffset).toBeUndefined();
    });

    it("openl_expand_trace_tree normalizes a childless step (backend returns { total: 0 }, no children) to an empty list", async () => {
      // A step that made no sub-calls answers 200 with just { total: 0 } — the
      // backend omits the children array — so the tool fills in children: [].
      mockAxios.onGet(`/projects/${encoded}/trace/tree/children`).reply(200, { total: 0 });

      const result = await executeTool(
        "expand_trace_tree",
        { projectId, uri: "u", instance: 0, step: "R9C9", response_format: "json" },
        client
      );

      const parsed = JSON.parse(result.content[0].text).data;
      expect(parsed.children).toEqual([]);
      expect(parsed.total).toBe(0);
      expect(parsed.hasMore).toBe(false);
    });

    it("openl_expand_trace_tree stops paging when a page comes back empty even though total still exceeds offset (dropped sub-calls)", async () => {
      // A step whose sub-calls were dropped at the retained-tree size cap: total
      // counts them but they can never be paged to, so an offset past the retained
      // children returns []. hasMore must be false — otherwise nextOffset freezes
      // and an agent following it re-requests the same page forever.
      mockAxios.onGet(`/projects/${encoded}/trace/tree/children`).reply(200, { children: [], total: 500 });

      const result = await executeTool(
        "expand_trace_tree",
        { projectId, uri: "u", instance: 0, step: "R1C0", offset: 100, response_format: "json" },
        client
      );

      const parsed = JSON.parse(result.content[0].text).data;
      expect(parsed.hasMore).toBe(false);
      expect(parsed.nextOffset).toBeUndefined();
      expect(parsed.total).toBe(500);
    });

    it("openl_expand_trace_tree maps a 404 to an actionable no-session / profiling-required message", async () => {
      mockAxios.onGet(`/projects/${encoded}/trace/tree/children`).reply(404, { message: "trace.execution.task.message" });

      await expect(
        executeTool("expand_trace_tree", { projectId, uri: "u", instance: 0, step: "R1C0" }, client)
      ).rejects.toThrow(/openl_start_trace|profiling/);
    });

    it("openl_expand_trace_tree maps a 409 to an actionable 'run still in progress' message", async () => {
      mockAxios.onGet(`/projects/${encoded}/trace/tree/children`).reply(409, { message: "trace.execution.not.suspended.message" });

      await expect(
        executeTool("expand_trace_tree", { projectId, uri: "u", instance: 0, step: "R1C0" }, client)
      ).rejects.toThrow(/still in progress|openl_resume_trace/);
    });

    it("openl_step_trace steps compact by default and returns the new stack", async () => {
      let stepParams: Record<string, unknown> | undefined;
      mockAxios.onPost(`/projects/${encoded}/trace/step`).reply((config) => {
        stepParams = config.params;
        return [200, suspendedStack];
      });

      const result = await executeTool(
        "step_trace",
        { projectId, type: "over", response_format: "json" },
        client
      );
      expect(stepParams).toEqual({ type: "over", view: "compact", includeTree: "false" });
      expect(result.content[0].text).toContain("CalcRule");
    });

    it("openl_step_trace with withValues bundles the active frame's variables", async () => {
      mockAxios.onPost(`/projects/${encoded}/trace/step`).reply(200, suspendedStack);
      mockAxios.onGet(`/projects/${encoded}/trace/frames/0/variables`).reply(200, {
        parameters: [{ name: "policy", description: "AutoPolicy", value: { age: 30 } }],
        steps: [],
        errors: [],
      });

      const result = await executeTool(
        "step_trace",
        { projectId, type: "out", withValues: true, response_format: "json" },
        client
      );

      const body = JSON.parse(result.content[0].text) as { data: { variables?: { parameters: Array<{ name: string }> } } };
      expect(body.data.variables?.parameters[0].name).toBe("policy");
    });

    it("openl_step_trace maps a 409 to an actionable not-suspended message", async () => {
      mockAxios.onPost(`/projects/${encoded}/trace/step`).reply(409, { message: "trace.execution.not.suspended.message" });

      await expect(
        executeTool("step_trace", { projectId, type: "into" }, client)
      ).rejects.toThrow(/not suspended/i);
    });

    it("openl_step_trace maps a 404 to an actionable no-session message", async () => {
      mockAxios.onPost(`/projects/${encoded}/trace/step`).reply(404, { message: "trace.execution.task.message" });

      await expect(
        executeTool("step_trace", { projectId, type: "into" }, client)
      ).rejects.toThrow(/openl_start_trace/);
    });

    it("openl_resume_trace resumes, polls the status until it leaves running, then returns the stack", async () => {
      mockAxios.onPost(`/projects/${encoded}/trace/resume`).reply(202);
      const statuses = ["running", "running", "suspended"];
      mockAxios.onGet(`/projects/${encoded}/trace/status`).reply(() => [200, { status: statuses.shift() ?? "suspended" }]);
      mockAxios.onGet(`/projects/${encoded}/trace/stack`).reply(200, suspendedStack);

      const result = await executeTool(
        "resume_trace",
        { projectId, response_format: "json" },
        client
      );

      expect(statuses).toHaveLength(0);
      expect(result.content[0].text).toContain("suspended");
    });

    it("openl_resume_trace re-attaches to a still-running session instead of failing on the resume 409", async () => {
      mockAxios.onPost(`/projects/${encoded}/trace/resume`).reply(409, { message: "trace.execution.not.suspended.message" });
      const statuses = ["running", "completed"];
      mockAxios.onGet(`/projects/${encoded}/trace/status`).reply(() => [200, { status: statuses.shift() ?? "completed" }]);
      mockAxios.onGet(`/projects/${encoded}/trace/stack`).reply(200, { status: "completed", frames: [] });

      const result = await executeTool(
        "resume_trace",
        { projectId, response_format: "json" },
        client
      );

      expect(result.content[0].text).toContain("completed");
    });

    it("openl_resume_trace rejects the resume 409 when the session is terminal (not merely running)", async () => {
      mockAxios.onPost(`/projects/${encoded}/trace/resume`).reply(409, { message: "trace.execution.not.suspended.message" });
      mockAxios.onGet(`/projects/${encoded}/trace/status`).reply(200, { status: "completed" });

      await expect(
        executeTool("resume_trace", { projectId }, client)
      ).rejects.toThrow(/not suspended/i);
    });

    it("openl_resume_trace reports a still-running trace on timeout as a structured status, not erroring", async () => {
      mockAxios.onPost(`/projects/${encoded}/trace/resume`).reply(202);
      mockAxios.onGet(`/projects/${encoded}/trace/status`).reply(200, { status: "running" });

      const result = await executeTool(
        "resume_trace",
        { projectId, timeoutMs: 1, response_format: "json" },
        client
      );

      const body = JSON.parse(result.content[0].text) as { data: { status?: string; message?: string } };
      // The actual status is carried through (not lost), and JSON consumers get a real object.
      expect(body.data.status).toBe("running");
      expect(body.data.message).toMatch(/still running/);
      expect(body.data.message).toContain("openl_stop_trace");
    });

    it("openl_resume_trace maps a session reaped mid-poll (404) to the actionable no-session message", async () => {
      mockAxios.onPost(`/projects/${encoded}/trace/resume`).reply(202);
      // First poll shows running; the session is then reaped and status 404s.
      const statuses = [() => [200, { status: "running" }], () => [404, { message: "trace.execution.task.message" }]];
      mockAxios.onGet(`/projects/${encoded}/trace/status`).reply(() => (statuses.shift() ?? (() => [404, {}]))());

      await expect(
        executeTool("resume_trace", { projectId, timeoutMs: 5000 }, client)
      ).rejects.toThrow(/openl_start_trace/);
    });

    it("openl_inspect_trace_frame trims the response via ?fields by default and lifts the trim with full: true", async () => {
      const paramsSeen: Array<Record<string, unknown> | undefined> = [];
      mockAxios.onGet(`/projects/${encoded}/trace/frames/0/variables`).reply((config) => {
        paramsSeen.push(config.params);
        return [200, { parameters: [], steps: [], errors: [], decision: { firedRules: ["R10"], conditions: [] } }];
      });

      const result = await executeTool(
        "inspect_trace_frame",
        { projectId, frameIndex: 0, response_format: "json" },
        client
      );
      expect(result.content[0].text).toContain("R10");
      expect(paramsSeen[0]?.fields).toContain("decision");
      expect(paramsSeen[0]?.fields).toContain("parameters(name,description,lazy,parameterId,value)");
      expect(paramsSeen[0]?.fields).not.toContain("schema");

      await executeTool("inspect_trace_frame", { projectId, frameIndex: 0, full: true }, client);
      expect(paramsSeen[1]).toEqual({ includeSchema: true });
    });

    it("openl_inspect_trace_frame filters steps by onlyExecutedSteps and excludeStepValues, resolving lazy values", async () => {
      mockAxios.onGet(`/projects/${encoded}/trace/frames/0/variables`).reply(200, {
        parameters: [],
        errors: [],
        steps: [
          { ref: "R0C0", label: "$AgeFactor", status: "executed", value: { name: "$AgeFactor", value: 1 } },
          // A neutral factor delivered lazily — must still be dropped by [1].
          { ref: "R3C0", label: "$BaseRate", status: "executed", value: { name: "$BaseRate", lazy: true, parameterId: 7 } },
          { ref: "R1C0", label: "$VehiclePriceFactor", status: "executed", value: { name: "$VehiclePriceFactor", value: 83.372 } },
          { ref: "R2C0", label: "$Pending", status: "pending" },
        ],
      });
      // The lazy $BaseRate resolves to 1 via the parameter endpoint.
      mockAxios.onGet(`/projects/${encoded}/trace/parameters/7`).reply(200, { name: "$BaseRate", value: 1 });

      const result = await executeTool(
        "inspect_trace_frame",
        { projectId, frameIndex: 0, onlyExecutedSteps: true, excludeStepValues: [1], response_format: "json" },
        client
      );

      const body = JSON.parse(result.content[0].text) as { data: { steps: Array<{ label: string }> } };
      // The neutral factors (inline 1 and lazy→1) and the pending step are gone; the outlier stays.
      expect(body.data.steps.map((s) => s.label)).toEqual(["$VehiclePriceFactor"]);
    });

    it("openl_inspect_trace_frame with withHighlights merges the overlay and the raw grid of the frame's table", async () => {
      mockAxios.onGet(`/projects/${encoded}/trace/frames/0/variables`).reply(200, { parameters: [], steps: [], errors: [] });
      mockAxios.onGet(`/projects/${encoded}/trace/frames/0/highlights`).reply(200, [{ cell: "C5", state: "current" }]);
      mockAxios.onGet(`/projects/${encoded}/trace/stack`).reply(200, suspendedStack);
      let tableParams: Record<string, unknown> | undefined;
      mockAxios.onGet(new RegExp(`/projects/${encoded}/tables/calc_42`)).reply((config) => {
        tableParams = config.params;
        return [200, { view: "raw", cells: [["Header"], ["=A1*2"]] }];
      });

      const result = await executeTool(
        "inspect_trace_frame",
        { projectId, frameIndex: 0, withHighlights: true, response_format: "json" },
        client
      );

      expect(tableParams?.raw).toBe(true);
      const text = result.content[0].text;
      expect(text).toContain('"C5"');
      expect(text).toContain("=A1*2");
    });

    it("openl_inspect_trace_frame maps a 409 to guidance about the terminal/running state", async () => {
      mockAxios.onGet(`/projects/${encoded}/trace/frames/3/variables`).reply(409, { message: "trace.execution.not.suspended.message" });

      await expect(
        executeTool("inspect_trace_frame", { projectId, frameIndex: 3 }, client)
      ).rejects.toThrow(/not suspended/i);
    });

    it("openl_set_trace_breakpoints reads the current set and the available targets", async () => {
      mockAxios.onGet(`/projects/${encoded}/trace/breakpoints`).reply(200, ["CalcRule"]);
      mockAxios.onGet(`/projects/${encoded}/trace/breakpoint-tables`).reply(200, [
        { name: "CalcRule", kind: "spreadsheet" },
        { name: "BankRatingGroup", kind: "decisionTable" },
      ]);

      const result = await executeTool(
        "set_trace_breakpoints",
        { projectId, response_format: "json" },
        client
      );

      const text = result.content[0].text;
      expect(text).toContain("BankRatingGroup");
      expect(mockAxios.history.put).toHaveLength(0);
    });

    it("openl_set_trace_breakpoints with set replaces the whole set before reading it back", async () => {
      let putBody: unknown;
      mockAxios.onPut(`/projects/${encoded}/trace/breakpoints`).reply((config) => {
        putBody = config.data;
        return [204];
      });
      mockAxios.onGet(`/projects/${encoded}/trace/breakpoints`).reply(200, ["uri#R0C1"]);
      mockAxios.onGet(`/projects/${encoded}/trace/breakpoint-tables`).reply(200, []);

      const result = await executeTool(
        "set_trace_breakpoints",
        { projectId, set: ["uri#R0C1"], response_format: "json" },
        client
      );

      expect(JSON.parse(putBody as string)).toEqual({ uris: ["uri#R0C1"] });
      expect(result.content[0].text).toContain("uri#R0C1");
    });

    it("openl_get_trace_value expands a lazy parameter, dropping the value's JSON Schema unless withSchema is set", async () => {
      const paramsSeen: Array<Record<string, unknown> | undefined> = [];
      mockAxios.onGet(`/projects/${encoded}/trace/parameters/5`).reply((config) => {
        paramsSeen.push(config.params);
        return [200, { name: "premium", description: "Double", value: 1000 }];
      });

      const result = await executeTool(
        "get_trace_value",
        { projectId, parameterId: 5, response_format: "json" },
        client
      );
      expect(result.content[0].text).toContain("1000");
      // The default ?fields projection excludes the token-heavy `schema`.
      expect(paramsSeen[0]?.fields).toBe("name,description,value");

      await executeTool("get_trace_value", { projectId, parameterId: 5, withSchema: true }, client);
      expect(paramsSeen[1]).toEqual({ includeSchema: true });
    });

    it("openl_get_trace_value maps a 404 to an actionable no-session message", async () => {
      mockAxios.onGet(`/projects/${encoded}/trace/parameters/9`).reply(404, { message: "trace.parameter.not.found.message" });

      await expect(
        executeTool("get_trace_value", { projectId, parameterId: 9 }, client)
      ).rejects.toThrow(/openl_start_trace/);
    });

    it("openl_stop_trace terminates the session", async () => {
      mockAxios.onDelete(`/projects/${encoded}/trace`).reply(204);

      const result = await executeTool("stop_trace", { projectId }, client);
      expect(result.content[0].text).toContain("terminated");
      expect(mockAxios.history.delete).toHaveLength(1);
    });

    it("openl_watch_trace_cells sets the watches, runs the table, and returns the collected series", async () => {
      const calls: string[] = [];
      let putBody: unknown;
      let clearedBreakpoints: unknown;
      let startParams: Record<string, string> = {};
      // Watch clears breakpoints first so the run reaches completion.
      mockAxios.onPut(`/projects/${encoded}/trace/breakpoints`).reply((config) => {
        clearedBreakpoints = config.data;
        return [204];
      });
      mockAxios.onPut(`/projects/${encoded}/trace/watches`).reply((config) => {
        putBody = config.data;
        calls.push("put");
        return [204];
      });
      mockAxios.onPost(new RegExp(`/projects/${encoded}/trace\\?`)).reply((config) => {
        startParams = Object.fromEntries(new URLSearchParams(config.url?.split("?")[1] ?? ""));
        calls.push("run");
        return [200, { status: "completed", frames: [] }];
      });
      let watchFields: string | undefined;
      mockAxios.onGet(`/projects/${encoded}/trace/watch`).reply((config) => {
        calls.push("watch");
        watchFields = config.params?.fields;
        return [200, {
          series: [{
            name: "$VehiclePriceFactor",
            table: "VehiclePremiumCalculation",
            points: [
              { instance: 0, value: { name: "$VehiclePriceFactor", description: "Double", value: 1.0 } },
              { instance: 1, ref: "R7C1", value: { name: "$VehiclePriceFactor", description: "Double", value: 83.372 } },
            ],
          }],
        }];
      });

      const result = await executeTool(
        "watch_trace_cells",
        { projectId, tableId: "calc_42", cells: ["$VehiclePriceFactor"], testRanges: "1", response_format: "json" },
        client
      );

      expect(calls).toEqual(["put", "run", "watch"]); // set → run → read
      expect(JSON.parse(clearedBreakpoints as string)).toEqual({ uris: [] }); // breakpoints cleared first
      expect(JSON.parse(putBody as string)).toEqual({ cells: ["$VehiclePriceFactor"] });
      expect(startParams.stopAtEntry).toBe("false");
      // The run to completion materializes lazy branches on its own — no
      // profiling (which would make the studio build a tree this tool discards).
      expect(startParams.profiling).toBeUndefined();
      expect(startParams.includeTree).toBeUndefined();
      // The point value is a ParameterValue; its JSON Schema is trimmed by default.
      expect(watchFields).toContain("value(name,description,lazy,parameterId,value)");
      expect(watchFields).not.toContain("schema");
      expect(result.content[0].text).toContain("83.372");
    });

    it("openl_watch_trace_cells surfaces the server's truncation (total + a note) when the series is capped", async () => {
      mockAxios.onPut(`/projects/${encoded}/trace/breakpoints`).reply(204);
      mockAxios.onPut(`/projects/${encoded}/trace/watches`).reply(204);
      mockAxios.onPost(new RegExp(`/projects/${encoded}/trace\\?`)).reply(200, { status: "completed", frames: [] });
      // The server caps points per series and reports the full execution count via `total`.
      const points = Array.from({ length: 200 }, (_, i) => ({ instance: i, value: { name: "$ClaimCost", value: i } }));
      mockAxios.onGet(`/projects/${encoded}/trace/watch`).reply(200, {
        truncated: true,
        series: [{ name: "$ClaimCost", table: "ClaimCostPerBenefitPerAgeBand", total: 11200, points }],
      });

      const result = await executeTool(
        "watch_trace_cells",
        { projectId, tableId: "calc_42", cells: ["$ClaimCost"], response_format: "json" },
        client
      );

      const body = JSON.parse(result.content[0].text) as {
        data: { truncated_json_preview: string };
        truncated: boolean;
      };
      // The formatter now enforces the response cap for single-object JSON too;
      // its preview still surfaces Studio's total and the tool's recovery guidance.
      expect(body.truncated).toBe(true);
      expect(body.data.truncated_json_preview).toContain("11200");
      expect(body.data.truncated_json_preview).toMatch(/truncated|@N/);
    });
  });
});
