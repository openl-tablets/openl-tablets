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

import { describe, it, expect, beforeEach, afterEach } from "@jest/globals";
import MockAdapter from "axios-mock-adapter";
import { InMemoryTransport } from "@modelcontextprotocol/server";
import type { Server } from "@modelcontextprotocol/server";
import { Client } from "@modelcontextprotocol/client";
import { OpenLClient } from "../src/client.js";
import { createConfiguredServer } from "../src/mcp-core.js";
describe("MCP core — tool-call error channel", () => {
  let openlClient: OpenLClient;
  let mockAxios: MockAdapter;
  let server: Server;
  let client: Client;

  beforeEach(async () => {
    openlClient = new OpenLClient({ baseUrl: "http://localhost:8080", personalAccessToken: "openl_pat_test" });
    // @ts-ignore - access the private axios instance for mocking
    mockAxios = new MockAdapter(openlClient.axiosInstance);

    server = createConfiguredServer(openlClient);

    const [clientTransport, serverTransport] = InMemoryTransport.createLinkedPair();
    await server.connect(serverTransport);
    client = new Client({ name: "test-client", version: "0.0.0" });
    await client.connect(clientTransport);
  });

  afterEach(async () => {
    mockAxios.restore();
    await client.close();
    await server.close();
  });

  const textOf = (result: { content: unknown }) =>
    (result.content as Array<{ type: string; text: string }>)[0].text;

  it("surfaces a backend error as an isError result carrying the detailed reason", async () => {
    // The studio rejects the read with a precise reason in the error body.
    mockAxios
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
    let called = false;
    mockAxios.onAny().reply(() => {
      called = true;
      return [200];
    });

    // get_table requires tableId; the handler rejects before any backend call.
    const result = await client.callTool({
      name: "openl_get_table",
      arguments: { projectId: "p1" },
    });

    expect(result.isError).toBe(true);
    expect(textOf(result)).toContain("Invalid arguments for get_table");
    expect(called).toBe(false);
  });

  it("succeeds without isError when the tool runs cleanly", async () => {
    mockAxios.onGet("/repos").reply(200, [{ id: "design", name: "Design" }]);

    const result = await client.callTool({ name: "openl_list_repositories", arguments: {} });

    expect(result.isError).toBeFalsy();
    expect(textOf(result)).toContain("Design");
  });

  it("preserves non-image binary bytes and their base64 input schema on the MCP wire", async () => {
    const bytes = Buffer.from([0x50, 0x4b, 0x03, 0x04]);
    mockAxios.onGet("/projects/p1/files/model.xlsx").reply(200, bytes, {
      "content-type": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
      "content-disposition": "attachment; filename=model.xlsx",
    });

    const result = await client.callTool({
      name: "openl_read_project_file",
      arguments: { projectId: "p1", path: "model.xlsx" },
    });
    expect(result.content).toHaveLength(1);
    expect(JSON.parse((result.content[0] as { text: string }).text).data).toMatchObject({
      path: "model.xlsx",
      mimeType: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
      content: bytes.toString("base64"),
    });

    const tools = await client.listTools();
    const writeTool = tools.tools.find((tool) => tool.name === "openl_write_project_file");
    const alternatives = writeTool?.inputSchema.anyOf as Array<Record<string, unknown>>;
    expect(writeTool?.inputSchema.type).toBe("object");
    expect(alternatives).toHaveLength(3);
    const blobAlternative = alternatives.find((alternative) =>
      Object.hasOwn(alternative.properties as object, "blob"));
    const properties = blobAlternative?.properties as Record<string, Record<string, unknown>>;
    expect(properties.blob).toMatchObject({
      contentEncoding: "base64",
      contentMediaType: "application/octet-stream",
    });
    expect(blobAlternative?.required).toEqual(expect.arrayContaining(["blob"]));
    expect(properties).not.toHaveProperty("content");
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

  it("advertises whole-table replacement as destructive and idempotent", async () => {
    const tools = await client.listTools();
    const update = tools.tools.find((tool) => tool.name === "openl_update_table");

    expect(update?.annotations?.destructiveHint).toBe(true);
    expect(update?.annotations?.idempotentHint).toBe(true);
  });

  it("surfaces a backend HTTP 405 as an isError result, not a protocol error", async () => {
    // handleToolError maps HTTP 405 to ProtocolErrorCode.MethodNotFound — the same code an
    // unknown tool produces. A registered tool that gets a 405 is still a tool
    // failure, so it must reach the agent as an isError result, not a throw.
    mockAxios.onGet(/\/tables\/t1$/).reply(405, { code: "METHOD_NOT_ALLOWED", message: "Not allowed here" });

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
    ).rejects.toThrow(/Unknown tool/);
  });
});
