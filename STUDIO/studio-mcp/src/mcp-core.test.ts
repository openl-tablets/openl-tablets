/**
 * Tests for the shared MCP core's tool-call error channel.
 *
 * A tool's own failure (backend 4xx/5xx, argument validation) must reach the
 * caller as an `isError` RESULT carrying the detailed message, so an agent can
 * self-correct — not as a thrown JSON-RPC protocol error that clients surface
 * as a generic "tool execution failed". Only a genuinely unknown tool stays a
 * protocol error. These tests drive the real CallTool handler end-to-end over
 * an in-memory transport pair, using pre-existing tools as vehicles.
 */

import { describe, it, expect, beforeEach, afterEach } from "vitest";
import { MockHttp } from "./testing/mock-http.js";
import { InMemoryTransport } from "@modelcontextprotocol/server";
import type { McpServer } from "@modelcontextprotocol/server";
import { Client } from "@modelcontextprotocol/client";
import { OpenLClient } from "./client.js";
import { createConfiguredServer } from "./mcp-core.js";
import { mcpToolName } from "./constants.js";
import { getAllTools } from "./handlers/index.js";

describe("MCP core — tool-call error channel", () => {
  let openlClient: OpenLClient;
  let mockHttp: MockHttp;
  let server: McpServer;
  let client: Client;

  beforeEach(async () => {
    openlClient = new OpenLClient({ baseUrl: "http://localhost:8080", authorization: "Token openl_pat_test" });
    mockHttp = new MockHttp(openlClient);

    server = createConfiguredServer(openlClient);

    const [clientTransport, serverTransport] = InMemoryTransport.createLinkedPair();
    await server.connect(serverTransport);
    client = new Client({ name: "test-client", version: "0.0.0" });
    await client.connect(clientTransport);
  });

  afterEach(async () => {
    await client.close();
    await server.close();
  });

  const textOf = (result: { content: unknown }) =>
    (result.content as Array<{ type: string; text: string }>)[0].text;

  it("surfaces a backend error as an isError result carrying the detailed reason", async () => {
    // The studio rejects the read with a precise reason in the error body.
    mockHttp
      .onGet(/\/tables\/t1$/)
      .reply(400, { code: "BAD_REQUEST", message: "column height 6 exceeds table height 5" });

    const result = await client.callTool({
      name: "openl_get_table",
      arguments: { projectId: "p1", tableId: "t1" },
    });

    // Reported as a tool RESULT (isError), not a thrown protocol error.
    expect(result.isError).toBe(true);
    const text = textOf(result);
    // The precise backend reason reaches the model, not a generic "failed".
    expect(text).toContain("column height 6 exceeds table height 5");
    expect(text).toContain("400");
    // ...but the internal studio REST endpoint is NOT leaked to the agent.
    expect(text).not.toContain("/projects/");
    expect(text).not.toContain("/tables/");
  });

  it("surfaces an argument-validation failure as an isError result (no backend call)", async () => {
    // get_table requires tableId; the handler rejects before any backend call.
    const result = await client.callTool({
      name: "openl_get_table",
      arguments: { projectId: "p1" },
    });

    expect(result.isError).toBe(true);
    expect(textOf(result)).toContain("Invalid arguments for get_table");
    expect(mockHttp.requests).toHaveLength(0);
  });

  it("succeeds without isError when the tool runs cleanly", async () => {
    mockHttp.onGet("/repos").reply(200, [{ id: "design", name: "Design" }]);

    const result = await client.callTool({ name: "openl_list_repositories", arguments: {} });

    expect(result.isError).toBeFalsy();
    expect(textOf(result)).toContain("Design");
  });

  it("does not advertise repeatable side effects as idempotent", async () => {
    const tools = await client.listTools();
    for (const name of [
      "openl_append_table",
      "openl_start_project_tests",
      "openl_deploy_project",
      "openl_redeploy_project",
    ]) {
      const tool = tools.tools.find((candidate) => candidate.name === name);
      expect(tool?.annotations?.idempotentHint).not.toBe(true);
    }
  });

  it("lists every tool with the JSON Schema of the registry, as the SDK does not rewrite it", async () => {
    const { tools } = await client.listTools();
    const registry = new Map(getAllTools().map((tool) => [mcpToolName(tool.name), tool.inputSchema]));

    expect(tools).toHaveLength(registry.size);
    for (const tool of tools) {
      expect(tool.inputSchema).toEqual(registry.get(tool.name));
    }
  });

  it("advertises whole-table replacement as destructive and idempotent", async () => {
    const tools = await client.listTools();
    const update = tools.tools.find((tool) => tool.name === "openl_update_table");

    expect(update?.annotations?.destructiveHint).toBe(true);
    expect(update?.annotations?.idempotentHint).toBe(true);
  });

  it("surfaces a backend HTTP 405 as an isError result, not a protocol error", async () => {
    // handleToolError maps HTTP 405 to ProtocolErrorCode.MethodNotFound. A registered
    // tool that gets a 405 is still a tool failure, so it must reach the agent as an
    // isError result, not a throw.
    mockHttp.onGet(/\/tables\/t1$/).reply(405, { code: "METHOD_NOT_ALLOWED", message: "Not allowed here" });

    const result = await client.callTool({
      name: "openl_get_table",
      arguments: { projectId: "p1", tableId: "t1" },
    });

    expect(result.isError).toBe(true);
    expect(textOf(result)).toContain("405");
  });

  it("keeps a genuinely unknown tool a thrown protocol error, not an isError result", async () => {
    await expect(
      client.callTool({ name: "openl_does_not_exist", arguments: {} }),
    ).rejects.toThrow(/Tool openl_\w+ not found/);
  });
});
