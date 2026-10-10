import { afterEach, beforeAll, beforeEach, describe, expect, it } from "@jest/globals";
import { ProtocolErrorCode } from "@modelcontextprotocol/server";
import MockAdapter from "axios-mock-adapter";

import { OpenLClient } from "../../src/client.js";
import { executeTool, registerAllTools } from "../../src/handlers/index.js";

function jsonResult<T>(text: string): T {
  const envelope = JSON.parse(text) as { data: T };
  return envelope.data;
}

describe("table workflow handlers", () => {
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

  it("runs a table, waits through current 202 and legacy 409 not-ready responses, and omits bulky schemas by default", async () => {
    let startParams: Record<string, unknown> | undefined;
    let startBody: unknown;
    let resultReads = 0;
    let resultFields: string | undefined;

    mockAxios.onPost("/projects/p1/run").reply((config) => {
      startParams = config.params;
      startBody = JSON.parse(config.data);
      return [202];
    });
    mockAxios.onGet("/projects/p1/run/result").reply((config) => {
      resultReads += 1;
      resultFields = config.params?.fields;
      if (resultReads === 1) {
        return [202, { status: "notReady" }];
      }
      if (resultReads === 2) {
        return [409, { code: "openl.error.409.run.not.completed", message: "still running" }];
      }
      return [200, { tableId: "t1", tableName: "Rate", result: { premium: 125 }, executionTimeMs: 4.5 }];
    });

    const response = await executeTool("run_table", {
      projectId: "p1",
      tableId: "t1",
      inputJson: { params: { age: 25 }, runtimeContext: { state: "CA" } },
      fromModule: "Main",
      response_format: "json",
    }, client);

    expect(startParams).toEqual({ tableId: "t1", fromModule: "Main" });
    expect(startBody).toEqual({ params: { age: 25 }, runtimeContext: { state: "CA" } });
    expect(resultReads).toBe(3);
    expect(resultFields).toContain("result");
    expect(resultFields).not.toContain("resultSchema");
    expect(jsonResult<Record<string, unknown>>(response.content[0].text)).toMatchObject({
      tableId: "t1",
      result: { premium: 125 },
    });
  });

  it("requests full run schemas only when explicitly asked", async () => {
    let resultParams: Record<string, unknown> | undefined = { sentinel: true };
    mockAxios.onPost("/projects/p1/run").reply(202);
    mockAxios.onGet("/projects/p1/run/result").reply((config) => {
      resultParams = config.params;
      return [200, { tableId: "t1", resultSchema: { type: "number" }, result: 42 }];
    });

    await executeTool("run_table", {
      projectId: "p1",
      tableId: "t1",
      inputJson: [],
      withSchema: true,
      response_format: "json",
    }, client);

    expect(resultParams).toBeUndefined();
  });

  it("cancels the Studio run when the MCP request is aborted", async () => {
    let cancelled = false;
    mockAxios.onPost("/projects/p1/run").reply(202);
    mockAxios.onGet("/projects/p1/run/result").reply(202, { status: "notReady" });
    mockAxios.onDelete("/projects/p1/run").reply(() => {
      cancelled = true;
      return [204];
    });

    const controller = new AbortController();
    const execution = executeTool(
      "run_table",
      { projectId: "p1", tableId: "t1", inputJson: [] },
      client,
      { signal: controller.signal } as never,
    );
    setTimeout(() => controller.abort(), 10);

    await expect(execution).rejects.toThrow(/aborted/);
    expect(cancelled).toBe(true);
  });

  it("cancels a run when abort happens before the start response is observed", async () => {
    const controller = new AbortController();
    let cancelled = false;
    let startSignal: AbortSignal | undefined;
    mockAxios.onPost("/projects/p1/run").reply((config) => {
      startSignal = config.signal;
      controller.abort();
      return [202];
    });
    mockAxios.onDelete("/projects/p1/run").reply(() => {
      cancelled = true;
      return [204];
    });

    await expect(executeTool(
      "run_table",
      { projectId: "p1", tableId: "t1", inputJson: [] },
      client,
      { signal: controller.signal } as never,
    )).rejects.toThrow(/aborted/);
    expect(cancelled).toBe(true);
    expect(startSignal).toBeUndefined();
  });

  it("rejects overlapping runs before they can replace session-scoped Studio state", async () => {
    let releaseStart!: () => void;
    let markStartEntered!: () => void;
    const startEntered = new Promise<void>((resolve) => {
      markStartEntered = resolve;
    });
    mockAxios.onPost("/projects/p1/run").reply(() => new Promise((resolve) => {
      markStartEntered();
      releaseStart = () => resolve([202]);
    }));
    mockAxios.onGet("/projects/p1/run/result").reply(200, {
      tableId: "t1",
      tableName: "Rate",
      result: 42,
    });

    const first = executeTool("run_table", {
      projectId: "p1",
      tableId: "t1",
      inputJson: [],
    }, client);
    await startEntered;

    await expect(executeTool("run_table", {
      projectId: "p1",
      tableId: "t2",
      inputJson: [],
    }, client)).rejects.toThrow(/already active in this Studio session/);
    expect(mockAxios.history.post).toHaveLength(1);
    expect(mockAxios.history.delete).toHaveLength(0);

    releaseStart();
    await expect(first).resolves.toBeDefined();
  });

  it("applies timeoutMs to startup and the remaining result-read budget", async () => {
    const requestTimeouts: number[] = [];
    mockAxios.onPost("/projects/p1/run").reply((config) => {
      requestTimeouts.push(config.timeout ?? 0);
      return [202];
    });
    mockAxios.onGet("/projects/p1/run/result").reply((config) => {
      requestTimeouts.push(config.timeout ?? 0);
      return [200, { tableId: "t1", tableName: "Rate", result: 42 }];
    });

    await executeTool("run_table", {
      projectId: "p1",
      tableId: "t1",
      inputJson: [],
      timeoutMs: 450_000,
    }, client);

    expect(requestTimeouts).toHaveLength(2);
    expect(requestTimeouts[0]).toBeGreaterThan(30_000);
    expect(requestTimeouts[0]).toBeLessThanOrEqual(450_000);
    expect(requestTimeouts[1]).toBeGreaterThan(0);
    expect(requestTimeouts[1]).toBeLessThanOrEqual(requestTimeouts[0]);
  });

  it("bounds result polling and cancels the Studio run on timeout", async () => {
    mockAxios.onPost("/projects/p1/run").reply(202);
    mockAxios.onGet("/projects/p1/run/result").reply(202, { status: "notReady" });
    mockAxios.onDelete("/projects/p1/run").reply(204);

    await expect(executeTool("run_table", {
      projectId: "p1",
      tableId: "t1",
      inputJson: [],
      timeoutMs: 1,
    }, client)).rejects.toMatchObject({
      code: ProtocolErrorCode.InvalidRequest,
      message: expect.stringMatching(/did not finish/),
    });

    expect(mockAxios.history.delete).toHaveLength(1);
  });

  it("cancels the Studio run instead of treating a genuine 409 as not ready", async () => {
    mockAxios.onPost("/projects/p1/run").reply(202);
    mockAxios.onGet("/projects/p1/run/result").reply(409, {
      code: "openl.error.409.run.conflict",
      message: "conflicting run state",
    });
    mockAxios.onDelete("/projects/p1/run").reply(204);

    await expect(executeTool("run_table", {
      projectId: "p1",
      tableId: "t1",
      inputJson: [],
    }, client)).rejects.toThrow(/conflicting run state|409/);

    expect(mockAxios.history.delete).toHaveLength(1);
  });

  it("maps whole-project and table-neighborhood graph options to their endpoints", async () => {
    const seen: Array<{ url?: string; params?: Record<string, unknown> }> = [];
    mockAxios.onGet(/\/projects\/p1\/tables(?:\/[^/]+)?\/graph/).reply((config) => {
      seen.push({ url: config.url, params: config.params });
      return [200, [{ id: "t1", name: "Rate", dependencies: ["base"], dependents: [] }]];
    });

    await executeTool("get_table_dependencies", {
      projectId: "p1", module: "Main", layer: "datatype", response_format: "json",
    }, client);
    await executeTool("get_table_dependencies", {
      projectId: "p1", tableId: "Rate #1", direction: "DEPENDENTS", depth: 3, response_format: "json",
    }, client);

    expect(seen).toEqual([
      { url: "/projects/p1/tables/graph", params: { module: "Main", layer: "datatype" } },
      { url: "/projects/p1/tables/Rate%20%231/graph", params: { direction: "DEPENDENTS", depth: 3 } },
    ]);
  });

  it("preserves dependency edges in every Markdown graph format", async () => {
    const graph = [
      { id: "root", name: "Premium", dependencies: ["base"], dependents: [] },
      { id: "base", name: "Base Rate", dependencies: [], dependents: ["root"] },
      { id: "isolated", name: "Standalone", dependencies: [], dependents: [] },
    ];
    mockAxios.onGet("/projects/p1/tables/root/graph").reply(200, graph);

    for (const response_format of ["markdown", "markdown_detailed", "markdown_concise"] as const) {
      const response = await executeTool("get_table_dependencies", {
        projectId: "p1",
        tableId: "root",
        direction: "DEPENDENCIES",
        depth: 1,
        response_format,
      }, client);
      const text = response.content[0].text;
      expect(text).toContain("Dependency");
      expect(text).toContain("Premium");
      expect(text).toContain("root");
      expect(text).toContain("DEPENDENCIES");
      if (response_format === "markdown_concise") {
        expect(text).not.toContain("…");
        expect(text).toContain("3 executable nodes with 1 call link");
      } else {
        expect(text).toContain("Base Rate");
      }
    }

    const markdown = (await executeTool("get_table_dependencies", {
      projectId: "p1", tableId: "root", response_format: "markdown",
    }, client)).content[0].text;
    expect(markdown).toContain("flowchart LR");
    expect(markdown).toContain("e0 --> e1");
    expect(markdown).not.toContain("## Node details");

    const detailed = (await executeTool("get_table_dependencies", {
      projectId: "p1", tableId: "root", response_format: "markdown_detailed",
    }, client)).content[0].text;
    expect(detailed).toContain("**Depends on:** Base Rate (`base`)");
    expect(detailed).toContain("**Used by:** Premium (`root`)");
  });

  it("discovers modules, worksheets, and allowed property definitions", async () => {
    const modules = [{ name: "Main", path: "rules/*.xlsx", modules: [{ name: "Rates", path: "rules/Rates.xlsx" }] }];
    const properties = [{ name: "state", type: "enum", multiple: true, values: [{ code: "CA", value: "California" }] }];
    let propertyParams: Record<string, unknown> | undefined;
    mockAxios.onGet("/projects/p1/modules").reply(200, modules);
    mockAxios.onGet("/projects/p1/modules/Main%20Rules/sheets").reply(200, ["Rates", "Tests"]);
    mockAxios.onGet("/projects/p1/properties").reply((config) => {
      propertyParams = config.params;
      return [200, properties];
    });

    const moduleResult = await executeTool("list_project_modules", {
      projectId: "p1", response_format: "json",
    }, client);
    const sheetResult = await executeTool("list_module_sheets", {
      projectId: "p1", moduleName: "Main Rules", response_format: "json",
    }, client);
    const propertyResult = await executeTool("list_table_property_definitions", {
      projectId: "p1", tableType: "Rules", response_format: "json",
    }, client);

    expect(jsonResult(moduleResult.content[0].text)).toEqual(modules);
    expect(jsonResult(sheetResult.content[0].text)).toEqual(["Rates", "Tests"]);
    expect(propertyParams).toEqual({ tableType: "Rules" });
    expect(jsonResult(propertyResult.content[0].text)).toEqual(properties);
  });

  it("copies a table server-side with the exact current request shape", async () => {
    let body: unknown;
    mockAxios.onPost("/projects/p1/tables/source%231/copy").reply((config) => {
      body = JSON.parse(config.data);
      return [201, {
        id: "copy-1", name: "CopiedRate", tableType: "SimpleRules", file: "rules/Copies.xlsx", pos: "A1:C5",
        success: false,
      }];
    });

    const response = await executeTool("copy_table", {
      projectId: "p1",
      tableId: "source#1",
      moduleName: "Copies",
      modulePath: "rules/Copies.xlsx",
      name: "CopiedRate",
      sheetName: "Rates",
      properties: [{ name: "state", value: "CA" }],
      response_format: "json",
    }, client);

    expect(body).toEqual({
      moduleName: "Copies",
      modulePath: "rules/Copies.xlsx",
      name: "CopiedRate",
      properties: [{ name: "state", value: "CA" }],
      sheetName: "Rates",
    });
    expect(jsonResult<Record<string, unknown>>(response.content[0].text)).toMatchObject({
      success: true,
      tableId: "copy-1",
      name: "CopiedRate",
    });
  });
});
