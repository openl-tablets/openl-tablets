/**
 * The allow-list applied through the real server, over an in-memory transport.
 *
 * The parser tests say what the list parses to; these say what the server does
 * with it — which is the half that matters. In particular a withheld tool must
 * be indistinguishable from one this build never had: same error, no hint that
 * something is being kept back.
 */

import { describe, it, expect, beforeEach, afterEach } from "@jest/globals";
import { InMemoryTransport } from "@modelcontextprotocol/server";
import type { Server } from "@modelcontextprotocol/server";
import { Client } from "@modelcontextprotocol/client";
import { OpenLClient } from "../src/client.js";
import { createConfiguredServer } from "../src/mcp-core.js";

describe("MCP core — server-side tool allow-list", () => {
  let server: Server;
  let client: Client;
  let saved: string | undefined;

  const connect = async () => {
    server = createConfiguredServer(
      new OpenLClient({ baseUrl: "http://localhost:8080", personalAccessToken: "openl_pat_test" }),
    );
    const [clientTransport, serverTransport] = InMemoryTransport.createLinkedPair();
    await server.connect(serverTransport);
    client = new Client({ name: "test-client", version: "0.0.0" });
    await client.connect(clientTransport);
  };

  beforeEach(() => {
    saved = process.env.OPENL_MCP_TOOLS;
    delete process.env.OPENL_MCP_TOOLS;
  });

  afterEach(async () => {
    if (saved === undefined) delete process.env.OPENL_MCP_TOOLS;
    else process.env.OPENL_MCP_TOOLS = saved;
    await client?.close();
    await server?.close();
  });

  it("serves every tool when unset", async () => {
    await connect();
    const { tools } = await client.listTools();
    expect(tools.length).toBeGreaterThan(50);
    expect(tools.map((t) => t.name)).toContain("openl_update_table");
  });

  it("lists only the allowed tools when set", async () => {
    process.env.OPENL_MCP_TOOLS = "openl_get_table,openl_list_tables";
    await connect();
    const { tools } = await client.listTools();
    expect(tools.map((t) => t.name).sort()).toEqual(["openl_get_table", "openl_list_tables"]);
  });

  it("refuses to CALL a tool outside the list, not just to list it", async () => {
    // Filtering discovery alone is decoration — a client that already knows the
    // name could still invoke it.
    process.env.OPENL_MCP_TOOLS = "openl_get_table";
    await connect();
    await expect(
      client.callTool({ name: "openl_update_table", arguments: {} }),
    ).rejects.toThrow(/Unknown tool/);
  });

  it("withholds a tool exactly the way it reports one that does not exist", async () => {
    process.env.OPENL_MCP_TOOLS = "openl_get_table";
    await connect();
    const withheld = await client
      .callTool({ name: "openl_update_table", arguments: {} })
      .catch((e: Error) => e.message);
    const nonexistent = await client
      .callTool({ name: "openl_no_such_tool_at_all", arguments: {} })
      .catch((e: Error) => e.message);
    expect(withheld.replace("update_table", "X")).toBe(nonexistent.replace("no_such_tool_at_all", "X"));
  });

  it("serves NOTHING when the variable is set but names nothing", async () => {
    // The dangerous reading of an empty value is "no restriction". A feature whose
    // whole job is restriction must not fail towards less of it.
    process.env.OPENL_MCP_TOOLS = "";
    await connect();
    const { tools } = await client.listTools();
    expect(tools).toEqual([]);
    await expect(
      client.callTool({ name: "openl_get_started", arguments: {} }),
    ).rejects.toThrow(/Unknown tool/);
  });

  it("still allows a tool that IS on the list", async () => {
    process.env.OPENL_MCP_TOOLS = "openl_get_started";
    await connect();
    const result = await client.callTool({ name: "openl_get_started", arguments: {} });
    expect(result.isError).toBeFalsy();
  });
});
