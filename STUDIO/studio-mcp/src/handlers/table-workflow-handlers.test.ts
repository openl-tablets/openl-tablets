/**
 * Integration tests of the table workflow tools of src/handlers/table-workflow-handlers.ts: running a table, its
 * dependency graph, the property definitions of a table type, and a server-side copy of a table.
 */

import { describe, expect, it } from "vitest";
import { ProtocolErrorCode } from "@modelcontextprotocol/server";
import { executeTool } from "./index.js";
import { bodyOf, queryOf } from "../testing/mock-http.js";
import { dataOf, harness, textOf, useToolHarness } from "../testing/harness.js";

useToolHarness();

describe("table workflow handlers", () => {
  it("posts an input given as a JSON string to Studio as it is", async () => {
    harness.mockHttp.onPost("/projects/p1/run").reply(202);
    harness.mockHttp.onGet("/projects/p1/run/result").reply(200, { tableId: "t1", result: 1 });

    await executeTool("run_table", { projectId: "p1", tableId: "t1", inputJson: '{"params":{"age":25}}' }, harness.client);

    expect(harness.mockHttp.history.post[0].data).toBe('{"params":{"age":25}}');
  });

  it("runs a table, waits through 202 not-ready responses, and omits bulky schemas by default", async () => {
    harness.mockHttp.onPost("/projects/p1/run").reply(202);
    harness.mockHttp
      .onGet("/projects/p1/run/result").replyOnce(202, { status: "notReady" })
      .onGet("/projects/p1/run/result").replyOnce(202, { status: "notReady" })
      .onGet("/projects/p1/run/result").reply(200, { result: { premium: 125 }, executionTimeMs: 4.5 });

    const response = await executeTool("run_table", {
      projectId: "p1",
      tableId: "t1",
      inputJson: { params: { age: 25 }, runtimeContext: { state: "CA" } },
      fromModule: "Main",
    }, harness.client);

    const [start] = harness.mockHttp.history.post;
    expect(start.params).toEqual({ tableId: "t1", fromModule: "Main" });
    expect(bodyOf(start)).toEqual({ params: { age: 25 }, runtimeContext: { state: "CA" } });
    expect(harness.mockHttp.history.get).toHaveLength(3);
    const resultFields = harness.mockHttp.history.get[2].params?.fields;
    expect(resultFields).toContain("result");
    expect(resultFields).not.toContain("resultSchema");
    // The input the caller sent is not read back
    expect(resultFields).not.toContain("parameters");
    // Studio answers the fields asked, which leave out the table the caller named too
    expect(resultFields).not.toContain("tableId");
    expect(dataOf<Record<string, unknown>>(response)).toEqual({ result: { premium: 125 }, executionTimeMs: 4.5 });
  });

  it("requests full run schemas only when explicitly asked", async () => {
    harness.mockHttp.onPost("/projects/p1/run").reply(202);
    harness.mockHttp.onGet("/projects/p1/run/result").reply(200, { tableId: "t1", resultSchema: { type: "number" }, result: 42 });

    await executeTool("run_table", {
      projectId: "p1",
      tableId: "t1",
      inputJson: [],
      withSchema: true,
    }, harness.client);

    expect(queryOf(harness.mockHttp.history.get[0])).toEqual({});
  });

  it("cancels the Studio run when the MCP request is aborted", async () => {
    harness.mockHttp.onPost("/projects/p1/run").reply(202);
    harness.mockHttp.onGet("/projects/p1/run/result").reply(202, { status: "notReady" });
    harness.mockHttp.onDelete("/projects/p1/run").reply(204);

    await expect(executeTool(
      "run_table",
      { projectId: "p1", tableId: "t1", inputJson: [] },
      harness.client,
      { signal: AbortSignal.timeout(10) },
    )).rejects.toThrow(/aborted/);
    expect(harness.mockHttp.history.delete).toHaveLength(1);
  });

  it("cancels a run when abort happens before the start response is observed", async () => {
    const controller = new AbortController();
    harness.mockHttp.onPost("/projects/p1/run").reply(() => {
      controller.abort();
      return [202];
    });
    harness.mockHttp.onDelete("/projects/p1/run").reply(204);

    await expect(executeTool(
      "run_table",
      { projectId: "p1", tableId: "t1", inputJson: [] },
      harness.client,
      { signal: controller.signal },
    )).rejects.toThrow(/aborted/);
    expect(harness.mockHttp.history.delete).toHaveLength(1);
    expect(harness.mockHttp.history.post[0].signal).toBeUndefined();
  });

  it("rejects overlapping runs before they can replace session-scoped Studio state", async () => {
    const startEntered = Promise.withResolvers<void>();
    const startAnswer = Promise.withResolvers<[number]>();
    const releaseStart = (): void => startAnswer.resolve([202]);
    harness.mockHttp.onPost("/projects/p1/run").reply(() => {
      startEntered.resolve();
      return startAnswer.promise;
    });
    harness.mockHttp.onGet("/projects/p1/run/result").reply(200, {
      tableId: "t1",
      tableName: "Rate",
      result: 42,
    });

    const first = executeTool("run_table", {
      projectId: "p1",
      tableId: "t1",
      inputJson: [],
    }, harness.client);
    await startEntered.promise;

    await expect(executeTool("run_table", {
      projectId: "p1",
      tableId: "t2",
      inputJson: [],
    }, harness.client)).rejects.toThrow(/already active in this Studio session/);
    expect(harness.mockHttp.history.post).toHaveLength(1);
    expect(harness.mockHttp.history.delete).toHaveLength(0);

    releaseStart();
    await expect(first).resolves.toBeDefined();
  });

  it("applies timeoutMs to startup and the remaining result-read budget", async () => {
    const requestTimeouts: number[] = [];
    harness.mockHttp.onPost("/projects/p1/run").reply((config) => {
      requestTimeouts.push(config.timeout ?? 0);
      return [202];
    });
    harness.mockHttp.onGet("/projects/p1/run/result").reply((config) => {
      requestTimeouts.push(config.timeout ?? 0);
      return [200, { tableId: "t1", tableName: "Rate", result: 42 }];
    });

    await executeTool("run_table", {
      projectId: "p1",
      tableId: "t1",
      inputJson: [],
      timeoutMs: 450_000,
    }, harness.client);

    expect(requestTimeouts).toHaveLength(2);
    expect(requestTimeouts[0]).toBeGreaterThan(30_000);
    expect(requestTimeouts[0]).toBeLessThanOrEqual(450_000);
    expect(requestTimeouts[1]).toBeGreaterThan(0);
    expect(requestTimeouts[1]).toBeLessThanOrEqual(requestTimeouts[0]);
  });

  it("bounds result polling and cancels the Studio run on timeout", async () => {
    harness.mockHttp.onPost("/projects/p1/run").reply(202);
    harness.mockHttp.onGet("/projects/p1/run/result").reply(202, { status: "notReady" });
    harness.mockHttp.onDelete("/projects/p1/run").reply(204);

    await expect(executeTool("run_table", {
      projectId: "p1",
      tableId: "t1",
      inputJson: [],
      timeoutMs: 1,
    }, harness.client)).rejects.toMatchObject({
      code: ProtocolErrorCode.InvalidRequest,
      message: expect.stringMatching(/did not finish/),
    });

    expect(harness.mockHttp.history.delete).toHaveLength(1);
  });

  it("cancels the Studio run instead of treating a genuine 409 as not ready", async () => {
    harness.mockHttp.onPost("/projects/p1/run").reply(202);
    harness.mockHttp.onGet("/projects/p1/run/result").reply(409, {
      code: "openl.error.409.run.conflict",
      message: "conflicting run state",
    });
    harness.mockHttp.onDelete("/projects/p1/run").reply(204);

    await expect(executeTool("run_table", {
      projectId: "p1",
      tableId: "t1",
      inputJson: [],
    }, harness.client)).rejects.toThrow(/conflicting run state|409/);

    expect(harness.mockHttp.history.delete).toHaveLength(1);
  });

  it("maps whole-project and table-neighborhood graph options to their endpoints", async () => {
    harness.mockHttp.onGet(/\/projects\/p1\/tables(?:\/[^/]+)?\/graph/).reply(200, [
      { id: "t1", name: "Rate", dependencies: ["base"], dependents: [] },
    ]);

    await executeTool("get_table_dependencies", {
      projectId: "p1", module: "Main", layer: "datatype", }, harness.client);
    await executeTool("get_table_dependencies", {
      projectId: "p1", tableId: "Rate #1", direction: "DEPENDENTS", depth: 3, }, harness.client);
    await executeTool("get_table_dependencies", { projectId: "p1", tableId: "Rate" }, harness.client);

    expect(harness.mockHttp.history.get.map(({ url, params }) => ({ url, params }))).toEqual([
      { url: "/projects/p1/tables/graph", params: { module: "Main", layer: "datatype" } },
      { url: "/projects/p1/tables/Rate%20%231/graph", params: { direction: "DEPENDENTS", depth: 3 } },
      // A neighborhood without direction and depth leaves them to Studio.
      { url: "/projects/p1/tables/Rate/graph", params: {} },
    ]);
  });

  it.each(["markdown", "markdown_detailed", "markdown_concise"] as const)(
    "describes the query of the graph in %s",
    async (response_format) => {
      harness.mockHttp.onGet("/projects/p1/tables/root/graph").reply(200, [
        { id: "root", name: "Premium", dependencies: ["base"], dependents: [] },
        { id: "base", name: "Base Rate", dependencies: [], dependents: ["root"] },
      ]);

      const text = textOf(await executeTool("get_table_dependencies", {
        projectId: "p1", tableId: "root", direction: "DEPENDENCIES", depth: 1, response_format,
      }, harness.client));

      expect(text).toContain("scope table neighborhood, root table `root`, direction DEPENDENCIES, depth 1");
      expect(text).toContain("Premium");
    },
  );

  it("lists the properties a table type allows", async () => {
    const properties = [{ name: "state", type: "enum", multiple: true, values: [{ code: "CA", value: "California" }] }];
    harness.mockHttp.onGet("/projects/p1/properties", { params: { tableType: "Rules" } }).reply(200, properties);

    const result = await executeTool("list_table_property_definitions", {
      projectId: "p1", tableType: "Rules", }, harness.client);

    expect(dataOf(result)).toEqual(properties);
  });

  it("copies a table server-side with the exact current request shape", async () => {
    harness.mockHttp.onPost("/projects/p1/tables/source%231/copy").reply(201, {
      id: "copy-1", name: "CopiedRate", tableType: "SimpleRules", file: "rules/Copies.xlsx", pos: "A1:C5",
      success: false,
    });

    const response = await executeTool("copy_table", {
      projectId: "p1",
      tableId: "source#1",
      moduleName: "Copies",
      modulePath: "rules/Copies.xlsx",
      name: "CopiedRate",
      sheetName: "Rates",
      properties: [{ name: "state", value: "CA" }],
    }, harness.client);

    expect(bodyOf(harness.mockHttp.history.post[0])).toEqual({
      moduleName: "Copies",
      modulePath: "rules/Copies.xlsx",
      name: "CopiedRate",
      properties: [{ name: "state", value: "CA" }],
      sheetName: "Rates",
    });
    expect(dataOf<Record<string, unknown>>(response)).toMatchObject({
      success: true,
      tableId: "copy-1",
      name: "CopiedRate",
    });
  });
});
