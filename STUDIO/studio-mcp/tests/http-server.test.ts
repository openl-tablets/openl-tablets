/**
 * Unit tests for `resolveHttpBaseUrl` (the Streamable HTTP transport's base-URL
 * resolver) exported from src/http-server.ts.
 *
 * It mirrors the stdio transport's precedence — an explicit override (the
 * positional `<url>` / `--base-url` forwarded by the dispatcher) wins over the
 * `OPENL_BASE_URL` environment variable — and tolerates a missing/invalid value
 * by returning `undefined` so the server can still start. These cover that
 * resolution logic directly, in-process, without binding a port.
 */

import { afterEach, beforeEach, describe, expect, it, jest } from "@jest/globals";
import type { AddressInfo } from "node:net";
import type { Server as HttpServer } from "node:http";
import { Client, StreamableHTTPClientTransport } from "@modelcontextprotocol/client";
import { OpenLClient } from "../src/client.js";
import { SERVER_INFO } from "../src/constants.js";
import { versionInfo } from "../src/build-info.js";
import {
  createHttpApp,
  createHttpSessionClient,
  isAllowedHttpOrigin,
  resolveAllowedOrigins,
  resolveHttpBaseUrl,
  startHttpServer,
} from "../src/http-server.js";

async function listen(app: ReturnType<typeof createHttpApp>): Promise<{ server: HttpServer; url: URL }> {
  const server = await new Promise<HttpServer>((resolve) => {
    const listening = app.listen(0, "127.0.0.1", () => resolve(listening));
  });
  const address = server.address() as AddressInfo;
  return { server, url: new URL(`http://127.0.0.1:${address.port}/mcp`) };
}

async function closeServer(server: HttpServer): Promise<void> {
  await new Promise<void>((resolve, reject) => {
    server.close((error) => error ? reject(error) : resolve());
  });
}

describe("resolveHttpBaseUrl (HTTP MCP transport)", () => {
  let savedBaseUrl: string | undefined;
  let errSpy: ReturnType<typeof jest.spyOn>;

  beforeEach(() => {
    savedBaseUrl = process.env.OPENL_BASE_URL;
    delete process.env.OPENL_BASE_URL;
    // The resolver warns to stderr on an invalid URL — silence it.
    errSpy = jest.spyOn(console, "error").mockImplementation(() => {});
  });

  afterEach(() => {
    if (savedBaseUrl === undefined) delete process.env.OPENL_BASE_URL;
    else process.env.OPENL_BASE_URL = savedBaseUrl;
    errSpy.mockRestore();
  });

  it("returns the override (positional <url> / --base-url) when set", () => {
    expect(resolveHttpBaseUrl("http://positional:8080")).toBe("http://positional:8080");
  });

  it("prefers the override over OPENL_BASE_URL (documented precedence)", () => {
    process.env.OPENL_BASE_URL = "http://env-host:9999";
    expect(resolveHttpBaseUrl("http://positional:8080")).toBe("http://positional:8080");
  });

  it("falls back to OPENL_BASE_URL when no override is given", () => {
    process.env.OPENL_BASE_URL = "http://env-host:9999";
    expect(resolveHttpBaseUrl()).toBe("http://env-host:9999");
  });

  it("returns undefined when neither override nor OPENL_BASE_URL is set", () => {
    expect(resolveHttpBaseUrl()).toBeUndefined();
    expect(errSpy).not.toHaveBeenCalled();
  });

  it("returns undefined and warns on an invalid override", () => {
    expect(resolveHttpBaseUrl("not-a-url")).toBeUndefined();
    expect(errSpy).toHaveBeenCalledWith(expect.stringMatching(/Invalid OpenL base URL/i));
  });

  it("returns undefined and warns on an invalid OPENL_BASE_URL", () => {
    process.env.OPENL_BASE_URL = "not-a-url";
    expect(resolveHttpBaseUrl()).toBeUndefined();
    expect(errSpy).toHaveBeenCalledWith(expect.stringMatching(/Invalid OpenL base URL/i));
  });
});

describe("GET /health", () => {
  it("reports the running build so a deployed server can be identified, and leaks no configuration", async () => {
    const { server, url } = await listen(createHttpApp({ baseUrl: "http://studio.internal:8080" }));
    try {
      const response = await fetch(new URL("/health", url));
      expect(response.status).toBe(200);

      const body = await response.json() as {
        status: string;
        service: string;
        version: string;
        build: { id: string; source: string };
        runtime: { node: string; platform: string; arch: string };
      };

      expect(body.status).toBe("ok");
      expect(body.service).toBe(SERVER_INFO.NAME);
      expect(body.version).toBe(versionInfo().version);
      // The build id always starts with the version; the token after it depends
      // on whether build metadata shipped (CI tests run before the build).
      expect(body.build.id.startsWith(body.version)).toBe(true);
      expect(["build-metadata", "unavailable"]).toContain(body.build.source);
      expect(body.runtime.node).toBe(process.version);
      // The probe is unauthenticated: it must not expose the configured studio.
      expect(JSON.stringify(body)).not.toContain("studio.internal");
    } finally {
      await closeServer(server);
    }
  });
});

describe("HTTP MCP transport security and protocol negotiation", () => {
  it("accepts non-browser requests but requires exact configured browser origins", () => {
    const allowed = resolveAllowedOrigins("https://agent.example, http://localhost:4173");

    expect(isAllowedHttpOrigin(undefined, allowed)).toBe(true);
    expect(isAllowedHttpOrigin("https://agent.example", allowed)).toBe(true);
    expect(isAllowedHttpOrigin("http://localhost:4173", allowed)).toBe(true);
    expect(isAllowedHttpOrigin("http://localhost:4174", allowed)).toBe(false);
    expect(isAllowedHttpOrigin("https://evil.example", allowed)).toBe(false);
    expect(isAllowedHttpOrigin("null", allowed)).toBe(false);
  });

  it("rejects an untrusted Origin before dispatch and exposes the legacy session header", async () => {
    const allowedOrigin = "https://agent.example";
    const { server, url } = await listen(createHttpApp({
      baseUrl: "http://localhost:8080",
      allowedOrigins: new Set([allowedOrigin]),
    }));
    try {
      const rejected = await fetch(url, {
        method: "POST",
        headers: { "content-type": "application/json", origin: "https://evil.example" },
        body: JSON.stringify({ jsonrpc: "2.0", id: 1, method: "initialize", params: {} }),
      });
      expect(rejected.status).toBe(403);

      const preflight = await fetch(url, {
        method: "OPTIONS",
        headers: { origin: allowedOrigin },
      });
      expect(preflight.status).toBe(204);
      expect(preflight.headers.get("access-control-allow-origin")).toBe(allowedOrigin);
      expect(preflight.headers.get("access-control-expose-headers")).toContain("Mcp-Session-Id");
    } finally {
      await closeServer(server);
    }
  });

  it("negotiates the modern 2026-07-28 protocol", async () => {
    const { server, url } = await listen(createHttpApp({ baseUrl: "http://localhost:8080" }));
    const client = new Client(
      { name: "modern-test", version: "1.0.0" },
      { versionNegotiation: { mode: { pin: "2026-07-28" } } },
    );
    try {
      await client.connect(new StreamableHTTPClientTransport(url));
      expect(client.getProtocolEra()).toBe("modern");
      const tools = await client.listTools();
      expect(tools.tools.some((tool) => tool.name === "openl_list_projects")).toBe(true);
    } finally {
      await client.close();
      await closeServer(server);
    }
  });

  it("creates a distinct OpenLClient for every anonymous legacy MCP session", async () => {
    const created: OpenLClient[] = [];
    const { server, url } = await listen(createHttpApp({
      baseUrl: "http://localhost:8080",
      clientFactory: (baseUrl, authHeader) => {
        const client = createHttpSessionClient(baseUrl, authHeader);
        created.push(client);
        return client;
      },
    }));
    const first = new Client({ name: "legacy-one", version: "1.0.0" });
    const second = new Client({ name: "legacy-two", version: "1.0.0" });
    try {
      await first.connect(new StreamableHTTPClientTransport(url));
      await second.connect(new StreamableHTTPClientTransport(url));

      expect(first.getProtocolEra()).toBe("legacy");
      expect(second.getProtocolEra()).toBe("legacy");
      expect(created).toHaveLength(2);
      expect(created[0]).not.toBe(created[1]);
    } finally {
      await first.close();
      await second.close();
      await closeServer(server);
    }
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

  it("listens only on the interface HOST names", async () => {
    process.env.PORT = "0";
    process.env.HOST = "127.0.0.1";
    const logSpy = jest.spyOn(console, "log").mockImplementation(() => {});
    try {
      const server = await startHttpServer({ baseUrl: "http://localhost:8080" });
      expect((server.address() as AddressInfo).address).toBe("127.0.0.1");
      await closeServer(server);
    } finally {
      logSpy.mockRestore();
    }
  });
});
