/**
 * Unit tests for the Streamable HTTP transport of src/http-server.ts: the
 * addresses of Studio it calls and its clients opened, the
 * `/mcp` route over both protocol eras, the limit of a request
 * body, and the start on `PORT` and `HOST`.
 */

import { afterEach, describe, expect, it, vi } from "vitest";
import { once } from "node:events";
import type { AddressInfo } from "node:net";
import { createServer, type Server as HttpServer } from "node:http";
import { Client, StreamableHTTPClientTransport } from "@modelcontextprotocol/client";
import { OpenLClient } from "./client.js";
import { MockHttp } from "./testing/mock-http.js";
import {
  createHttpApp,
  createHttpSessionClient,
  LEGACY_SESSION_IDLE_MS,
  startHttpServer,
} from "./http-server.js";

/** Serves `app` on a free port of the loopback interface; `await using` closes the server at the end of the scope. */
async function listen(app: ReturnType<typeof createHttpApp>): Promise<HttpServer> {
  const server = createServer(app).listen(0, "127.0.0.1");
  await once(server, "listening");
  return server;
}

/** The address of the MCP endpoint of a server `listen` started. */
function mcpUrl(server: HttpServer): URL {
  return new URL(`http://127.0.0.1:${(server.address() as AddressInfo).port}/mcp`);
}

/** The headers of a request that sends JSON and takes JSON or an event stream. */
const json = { "content-type": "application/json", accept: "application/json, text/event-stream" };
/** The header OpenL Studio passes a request on with, for the secret `s3cret` of the run. */
const fromStudio = { "openl-mcp-secret": "s3cret" };

afterEach(() => {
  vi.restoreAllMocks();
});

describe("the address of OpenL Studio", () => {
  it("is the one OpenL Studio passes with the request", async () => {
    const called: string[] = [];
    await using server = await listen(createHttpApp({
      baseUrl: "http://fallback:8080",
      clientFactory: (baseUrl, headers) => {
        called.push(baseUrl);
        return createHttpSessionClient(baseUrl, headers);
      },
    }));
    const client = new Client({ name: "base-url", version: "1.0.0" });
    try {
      const requestInit = { headers: { "OpenL-Base-Url": "http://10.0.0.5:8080/studio" } };
      await client.connect(new StreamableHTTPClientTransport(mcpUrl(server), { requestInit }));
      await client.listTools();

      expect(called.length).toBeGreaterThan(0);
      expect(new Set(called)).toEqual(new Set(["http://10.0.0.5:8080/studio"]));
    } finally {
      await client.close();
    }
  });

  it("is required: a request that names none is refused", async () => {
    vi.spyOn(console, "error").mockImplementation(() => {});
    await using server = await listen(createHttpApp());
    const client = new Client({ name: "no-base-url", version: "1.0.0" });
    try {
      await expect(client.connect(new StreamableHTTPClientTransport(mcpUrl(server)))).rejects.toThrow();
    } finally {
      await client.close();
    }
  });
});

describe("createHttpSessionClient", () => {
  const headers = (values: Record<string, string>): Headers => new Headers(values);

  it("knows the address of OpenL Studio the client opened from the header OpenL Studio passes", () => {
    const client = createHttpSessionClient("http://127.0.0.1:8080/webstudio", headers({
      host: "127.0.0.1:3000",
      "openl-public-url": "https://studio-server.example.com/someContext/webstudio/",
    }));
    expect(client.publicUrl).toBe("https://studio-server.example.com/someContext/webstudio/");
  });

  it("falls back to the address the server calls without the header", () => {
    expect(createHttpSessionClient("http://127.0.0.1:8080/webstudio", headers({})).publicUrl)
      .toBe("http://127.0.0.1:8080/webstudio/");
  });

  it("calls OpenL Studio with the Authorization header of the request as it is", async () => {
    const client = createHttpSessionClient("http://127.0.0.1:8080", headers({ authorization: "Bearer eyJ.payload.sig" }));
    const mock = new MockHttp(client);
    mock.onGet("/repos").reply(200, []);

    await client.listRepositories();

    expect(mock.history.get[0].headers.Authorization).toBe("Bearer eyJ.payload.sig");
  });

  it("names the address of its caller back to OpenL Studio, with the secret of the server alone", async () => {
    vi.stubEnv("OPENL_MCP_SECRET", "s3cret");
    try {
      const client = createHttpSessionClient("http://127.0.0.1:8080", headers({ "openl-client-address": "203.0.113.7" }));
      const mock = new MockHttp(client);
      mock.onGet("/repos").reply(200, []);

      await client.listRepositories();

      expect(mock.history.get[0].headers["OpenL-Client-Address"]).toBe("203.0.113.7");
      expect(mock.history.get[0].headers["OpenL-Mcp-Secret"]).toBe("s3cret");
    } finally {
      vi.unstubAllEnvs();
    }
  });

  it("names no caller without the secret of the server", async () => {
    vi.stubEnv("OPENL_MCP_SECRET", "");
    try {
      const client = createHttpSessionClient(
        "http://127.0.0.1:8080",
        headers({ "openl-client-address": "203.0.113.7" }),
        { name: "claude-code", version: "2.1.0" },
      );
      const mock = new MockHttp(client);
      mock.onGet("/repos").reply(200, []);

      await client.listRepositories();

      expect(Object.keys(mock.history.get[0].headers).filter((name) => name.startsWith("OpenL-"))).toEqual([]);
    } finally {
      vi.unstubAllEnvs();
    }
  });
});

describe("the MCP client the calls to OpenL Studio are made for", () => {
  it.each([
    ["legacy", {}],
    ["modern", { versionNegotiation: { mode: { pin: "2026-07-28" as const } } }],
  ])("is named in each call of a %s client, with the secret of the server", async (era, options) => {
    vi.stubEnv("OPENL_MCP_SECRET", "s3cret");
    const mocks: MockHttp[] = [];
    await using server = await listen(createHttpApp({
      baseUrl: "http://localhost:8080",
      clientFactory: (baseUrl, headers, mcpClient) => {
        const client = createHttpSessionClient(baseUrl, headers, mcpClient);
        const mock = new MockHttp(client);
        mock.onGet("/repos").reply(200, []);
        mocks.push(mock);
        return client;
      },
    }));
    const client = new Client({ name: "claude-code", version: "2.1.0" }, options);
    try {
      await client.connect(new StreamableHTTPClientTransport(mcpUrl(server), { requestInit: { headers: fromStudio } }));
      expect(client.getProtocolEra()).toBe(era);

      await client.callTool({ name: "openl_list_repositories", arguments: {} });

      const calls = mocks.flatMap((mock) => mock.history.get);
      expect(calls.length).toBeGreaterThan(0);
      for (const call of calls) {
        expect(call.headers["OpenL-Mcp-Client"]).toBe("claude-code 2.1.0");
        expect(call.headers["OpenL-Mcp-Secret"]).toBe("s3cret");
      }
    } finally {
      await client.close();
      vi.unstubAllEnvs();
    }
  });
});

describe("HTTP MCP transport protocol negotiation", () => {
  it("negotiates the modern 2026-07-28 protocol", async () => {
    await using server = await listen(createHttpApp({ baseUrl: "http://localhost:8080" }));
    const client = new Client(
      { name: "modern-test", version: "1.0.0" },
      { versionNegotiation: { mode: { pin: "2026-07-28" } } },
    );
    try {
      await client.connect(new StreamableHTTPClientTransport(mcpUrl(server)));
      expect(client.getProtocolEra()).toBe("modern");
      const tools = await client.listTools();
      expect(tools.tools.some((tool) => tool.name === "openl_list_projects")).toBe(true);
    } finally {
      await client.close();
    }
  });

  it("creates a distinct OpenLClient for every anonymous legacy MCP session", async () => {
    const created: OpenLClient[] = [];
    await using server = await listen(createHttpApp({
      baseUrl: "http://localhost:8080/webstudio",
      clientFactory: (baseUrl, headers) => {
        const client = createHttpSessionClient(baseUrl, headers);
        created.push(client);
        return client;
      },
    }));
    const first = new Client({ name: "legacy-one", version: "1.0.0" });
    const second = new Client({ name: "legacy-two", version: "1.0.0" });
    try {
      const requestInit = { headers: { "OpenL-Public-Url": "https://studio.example.com/webstudio/" } };
      await first.connect(new StreamableHTTPClientTransport(mcpUrl(server), { requestInit }));
      await second.connect(new StreamableHTTPClientTransport(mcpUrl(server)));

      expect(first.getProtocolEra()).toBe("legacy");
      expect(second.getProtocolEra()).toBe("legacy");
      expect(created).toHaveLength(2);
      expect(created[0]).not.toBe(created[1]);
      // The address of OpenL Studio is the one OpenL Studio passes, else the one the server calls.
      expect(created[0].publicUrl).toBe("https://studio.example.com/webstudio/");
      expect(created[1].publicUrl).toBe("http://localhost:8080/webstudio/");
    } finally {
      await first.close();
      await second.close();
    }
  });
});

describe("idle legacy sessions", () => {
  const inSession = (sessionId: string) => ({ "mcp-session-id": sessionId, "mcp-protocol-version": "2025-11-25" });

  /** Opens a session the way a 2025-11-25 client does, and answers its ID. */
  async function openSession(url: URL): Promise<string> {
    const response = await fetch(url, {
      method: "POST",
      headers: json,
      body: JSON.stringify({
        jsonrpc: "2.0",
        id: 1,
        method: "initialize",
        params: { protocolVersion: "2025-11-25", capabilities: {}, clientInfo: { name: "idle", version: "1.0.0" } },
      }),
    });
    await response.text();
    return response.headers.get("mcp-session-id") ?? "";
  }

  /** The status a request of the session is answered with. */
  async function listTools(url: URL, sessionId: string): Promise<number> {
    const response = await fetch(url, {
      method: "POST",
      headers: { ...json, ...inSession(sessionId) },
      body: JSON.stringify({ jsonrpc: "2.0", id: 2, method: "tools/list" }),
    });
    await response.text();
    return response.status;
  }

  it("closes a session unused for longer than the timeout once another session opens", async () => {
    const now = vi.spyOn(Date, "now").mockReturnValue(0);
    await using server = await listen(createHttpApp({ baseUrl: "http://localhost:8080" }));
    const url = mcpUrl(server);

    const idle = await openSession(url);
    const used = await openSession(url);
    now.mockReturnValue(LEGACY_SESSION_IDLE_MS);
    expect(await listTools(url, used)).toBe(200);

    now.mockReturnValue(LEGACY_SESSION_IDLE_MS + 1);
    await openSession(url);

    expect(await listTools(url, idle)).toBe(404);
    expect(await listTools(url, used)).toBe(200);
  });

  it("keeps a session while its stream of server messages is open", async () => {
    const now = vi.spyOn(Date, "now").mockReturnValue(0);
    await using server = await listen(createHttpApp({ baseUrl: "http://localhost:8080" }));
    const url = mcpUrl(server);
    const stream = new AbortController();
    try {
      const listening = await openSession(url);
      const messages = await fetch(url, {
        headers: { accept: "text/event-stream", ...inSession(listening) },
        signal: stream.signal,
      });
      expect(messages.status).toBe(200);

      now.mockReturnValue(2 * LEGACY_SESSION_IDLE_MS);
      await openSession(url);

      expect(await listTools(url, listening)).toBe(200);
    } finally {
      // The open stream would keep the server from closing.
      stream.abort();
    }
  });

  it("names to OpenL Studio the address each request of a session comes from", async () => {
    vi.stubEnv("OPENL_MCP_SECRET", "s3cret");
    const mocks: MockHttp[] = [];
    await using server = await listen(createHttpApp({
      baseUrl: "http://localhost:8080",
      clientFactory: (baseUrl, headers) => {
        const client = createHttpSessionClient(baseUrl, headers);
        const mock = new MockHttp(client);
        mock.onGet("/repos").reply(200, []);
        mocks.push(mock);
        return client;
      },
    }));
    const url = mcpUrl(server);
    try {
      const response = await fetch(url, {
        method: "POST",
        headers: { ...json, ...fromStudio, "openl-client-address": "10.0.0.5" },
        body: JSON.stringify({
          jsonrpc: "2.0",
          id: 1,
          method: "initialize",
          params: { protocolVersion: "2025-11-25", capabilities: {}, clientInfo: { name: "roaming", version: "1.0.0" } },
        }),
      });
      await response.text();
      const sessionId = response.headers.get("mcp-session-id") ?? "";

      const call = await fetch(url, {
        method: "POST",
        headers: { ...json, ...fromStudio, ...inSession(sessionId), "openl-client-address": "10.0.0.9" },
        body: JSON.stringify({
          jsonrpc: "2.0",
          id: 2,
          method: "tools/call",
          params: { name: "openl_list_repositories", arguments: {} },
        }),
      });
      await call.text();

      const calls = mocks.flatMap((mock) => mock.history.get);
      expect(calls.length).toBeGreaterThan(0);
      expect(calls.map((sent) => sent.headers["OpenL-Client-Address"])).toEqual(calls.map(() => "10.0.0.9"));
    } finally {
      vi.unstubAllEnvs();
    }
  });
});

describe("requests that do not come through OpenL Studio", () => {
  it.each([
    ["no secret", {}],
    ["another secret", { "openl-mcp-secret": "guessed" }],
  ])("are refused with %s, so the secret is sent to no address they name", async (_, secret) => {
    vi.stubEnv("OPENL_MCP_SECRET", "s3cret");
    const clientFactory = vi.fn(createHttpSessionClient);
    await using server = await listen(createHttpApp({ clientFactory }));
    try {
      const response = await fetch(mcpUrl(server), {
        method: "POST",
        headers: { ...json, ...secret, "openl-base-url": "http://127.0.0.1:9" },
        body: JSON.stringify({
          jsonrpc: "2.0",
          id: 1,
          method: "initialize",
          params: { protocolVersion: "2025-11-25", capabilities: {}, clientInfo: { name: "local", version: "1.0.0" } },
        }),
      });
      await response.text();

      expect(response.status).toBe(403);
      expect(clientFactory).not.toHaveBeenCalled();
    } finally {
      vi.unstubAllEnvs();
    }
  });
});

describe("request bodies and routes", () => {
  it("refuses a body over 5 MB with 413 and a body that is not JSON with a parse error", async () => {
    await using server = await listen(createHttpApp({ baseUrl: "http://localhost:8080" }));
    const url = mcpUrl(server);

    const padding = "x".repeat(5 * 1024 * 1024);
    const tooLarge = await fetch(url, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ jsonrpc: "2.0", id: 1, method: "tools/list", params: { padding } }),
    });
    expect(tooLarge.status).toBe(413);
    expect(await tooLarge.json()).toMatchObject({ error: { code: -32600, message: "Request body exceeds 5 MB" } });

    const notJson = await fetch(url, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: "{not json",
    });
    expect(notJson.status).toBe(400);
    expect(await notJson.json()).toMatchObject({ error: { code: -32700 } });
  });

  it("answers an unknown address with 404", async () => {
    await using server = await listen(createHttpApp({ baseUrl: "http://localhost:8080" }));

    const response = await fetch(new URL("/other", mcpUrl(server)));

    expect(response.status).toBe(404);
    expect(await response.json()).toEqual({ error: "Not found", path: "/other", method: "GET" });
  });
});

describe("startHttpServer", () => {
  const saved = { PORT: process.env.PORT, HOST: process.env.HOST };

  afterEach(() => {
    for (const [name, value] of Object.entries(saved)) {
      if (value === undefined) delete process.env[name];
      else process.env[name] = value;
    }
  });

  it("listens only on the interface HOST names, on a free port it names", async () => {
    process.env.PORT = "0";
    process.env.HOST = "127.0.0.1";
    const logSpy = vi.spyOn(console, "log").mockImplementation(() => {});

    await using server = await startHttpServer();

    const address = server.address() as AddressInfo;
    expect(address.address).toBe("127.0.0.1");
    expect(address.port).toBeGreaterThan(0);
    // OpenL Studio reads the port from the end of the line
    const listening = new RegExp(`listening on 127\\.0\\.0\\.1, port ${address.port}$`);
    expect(logSpy).toHaveBeenCalledWith(expect.stringMatching(listening));
  });

  it("rejects the start when the port is taken", async () => {
    await using taken = createServer().listen(0, "127.0.0.1");
    await once(taken, "listening");
    process.env.PORT = String((taken.address() as AddressInfo).port);
    process.env.HOST = "127.0.0.1";

    await expect(startHttpServer()).rejects.toMatchObject({ code: "EADDRINUSE" });
  });
});
