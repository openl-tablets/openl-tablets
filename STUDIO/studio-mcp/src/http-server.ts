/**
 * Express HTTP transport for OpenL MCP Server.
 *
 * The `/mcp` endpoint serves both protocol eras: modern MCP 2026-07-28 via
 * `createMcpHandler`, and sessionful 2025-era clients through the legacy
 * Streamable HTTP transport. Browser origins are denied unless explicitly
 * allowlisted.
 */

import express, { type Express, type NextFunction, type Request, type Response } from "express";
import { randomUUID } from "node:crypto";
import { createServer, type Server as HttpServer } from "node:http";
import {
  createMcpHandler,
  isInitializeRequest,
  isLegacyRequest,
} from "@modelcontextprotocol/server";
import {
  NodeStreamableHTTPServerTransport,
  toNodeHandler,
  toWebRequest,
} from "@modelcontextprotocol/node";
import { OpenLClient } from "./client.js";
import { createConfiguredServer } from "./mcp-core.js";
import { parseBoolEnv, sanitizeError } from "./utils.js";
import type * as Types from "./types.js";
import { versionInfo, versionLine } from "./build-info.js";

const DEFAULT_PORT = "3000";
const DEFAULT_MAX_BODY_SIZE = "5mb";
const MCP_ALLOWED_METHODS = "GET, POST, DELETE, OPTIONS";
const MCP_ALLOWED_HEADERS = [
  "Accept",
  "Authorization",
  "Content-Type",
  "Last-Event-ID",
  "MCP-Protocol-Version",
  "Mcp-Method",
  "Mcp-Name",
  "Mcp-Session-Id",
].join(", ");

function jsonRpcError(code: number, message: string): Record<string, unknown> {
  return { jsonrpc: "2.0", error: { code, message }, id: null };
}

function resolveMaxBodySize(): string {
  const raw = process.env.MCP_MAX_BODY_SIZE?.trim();
  if (!raw) return DEFAULT_MAX_BODY_SIZE;
  if (/^\d+$/.test(raw) || /^\d+(\.\d+)?\s*(b|kb|mb|gb|tb|pb)$/i.test(raw)) return raw;
  console.warn(
    `⚠️  Ignoring invalid MCP_MAX_BODY_SIZE "${raw}" — falling back to ${DEFAULT_MAX_BODY_SIZE}. ` +
      `Use a byte count (e.g. 5242880) or a number with a unit (e.g. "5mb").`,
  );
  return DEFAULT_MAX_BODY_SIZE;
}

/** Resolve and validate the OpenL base URL used by HTTP request clients. */
export function resolveHttpBaseUrl(override?: string): string | undefined {
  const baseUrl = override ?? process.env.OPENL_BASE_URL;
  if (!baseUrl) return undefined;
  try {
    new URL(baseUrl);
  } catch {
    console.error(`⚠️  Invalid OpenL base URL: ${baseUrl}`);
    return undefined;
  }
  return baseUrl;
}

function normalizeConfiguredOrigin(value: string): string {
  const trimmed = value.trim();
  let parsed: URL;
  try {
    parsed = new URL(trimmed);
  } catch {
    throw new Error(`Invalid MCP_ALLOWED_ORIGINS entry: ${trimmed}`);
  }
  if (
    (parsed.protocol !== "http:" && parsed.protocol !== "https:") ||
    parsed.username || parsed.password || parsed.pathname !== "/" || parsed.search || parsed.hash
  ) {
    throw new Error(
      `Invalid MCP_ALLOWED_ORIGINS entry: ${trimmed}. Use an exact http(s) origin without a path.`,
    );
  }
  return parsed.origin;
}

/**
 * Browser origins allowed to call `/mcp`.
 *
 * Non-browser clients normally omit `Origin` and are unaffected. The default
 * admits only a browser page served from the MCP server's own loopback port;
 * deployments with a separate web client must enumerate its exact origins in
 * `MCP_ALLOWED_ORIGINS` (comma-separated).
 */
export function resolveAllowedOrigins(
  raw = process.env.MCP_ALLOWED_ORIGINS,
  port = process.env.PORT ?? DEFAULT_PORT,
): ReadonlySet<string> {
  const configured = raw?.trim()
    ? raw.split(",")
    : [`http://localhost:${port}`, `http://127.0.0.1:${port}`, `http://[::1]:${port}`];
  return new Set(configured.map(normalizeConfiguredOrigin));
}

/** Exact-origin check used by the HTTP middleware and unit tests. */
export function isAllowedHttpOrigin(origin: string | undefined, allowed: ReadonlySet<string>): boolean {
  if (origin === undefined) return true;
  try {
    return new URL(origin).origin === origin && allowed.has(origin);
  } catch {
    return false;
  }
}

function mcpCorsMiddleware(allowedOrigins: ReadonlySet<string>) {
  return (req: Request, res: Response, next: NextFunction): Response | void => {
    const origin = req.headers.origin;
    if (!isAllowedHttpOrigin(origin, allowedOrigins)) {
      return res.status(403).json(jsonRpcError(-32000, "Forbidden: Origin is not allowed"));
    }

    if (origin) {
      res.setHeader("Access-Control-Allow-Origin", origin);
      res.setHeader("Access-Control-Allow-Methods", MCP_ALLOWED_METHODS);
      res.setHeader("Access-Control-Allow-Headers", MCP_ALLOWED_HEADERS);
      res.setHeader("Access-Control-Expose-Headers", "Mcp-Session-Id");
      res.setHeader("Vary", "Origin");
    }
    if (req.method === "OPTIONS") return res.sendStatus(204);
    next();
  };
}

/**
 * Opt-in: present an inbound `Bearer` credential to Studio as `Bearer` instead of
 * rewriting it to `Token`.
 *
 * OFF BY DEFAULT, and the default is byte-identical to the previous behaviour —
 * both schemes are accepted on the way in and `Token` is what goes out. That
 * matters because forwarding a client-supplied credential to an upstream API is
 * token passthrough, which the MCP specification forbids
 * ("MCP servers MUST NOT accept any tokens that were not explicitly issued for
 * the MCP server"). Until this server can validate an inbound IdP token itself
 * — `iss`/`aud`/`exp` against the IdP's JWKS, plus RFC 9728 resource metadata,
 * the plan's P2.1 — a deployment that wants the passthrough should have to say
 * so, in a setting a reviewer can see.
 *
 * Why it is needed at all: a Studio in oauth2 mode accepts an IdP access token
 * as `Bearer`, and the same token presented as `Token` does not authenticate. So
 * an OAuth-capable MCP client can reach this server today but cannot reach
 * Studio through it.
 */
let passthroughAnnounced = false;

function preserveInboundAuthScheme(): boolean {
  const on = parseBoolEnv(process.env.OPENL_MCP_PRESERVE_AUTH_SCHEME);
  // Say it out loud, once. This server does NOT validate the credential it is
  // handed — it has no issuer, no audience and no JWKS to check against — so with
  // the flag on it forwards a caller-supplied token to Studio unexamined. That is
  // token passthrough, which the MCP specification forbids, and it is a deliberate
  // deployment choice rather than a default. A choice that only ever appears in a
  // config file is one nobody revisits; in the log it is at least visible to
  // whoever is looking at the server.
  if (on && !passthroughAnnounced) {
    passthroughAnnounced = true;
    console.error(
      "[Auth] OPENL_MCP_PRESERVE_AUTH_SCHEME is ON: an inbound Bearer credential is " +
        "forwarded to OpenL Studio AS RECEIVED and is NOT validated here (no issuer/" +
        "audience/JWKS check). This is token passthrough — keep this server reachable " +
        "only from callers you trust.",
    );
  }
  return on;
}

/** The credential and the scheme it arrived under; `Token` unless told otherwise. */
function credentialFromAuthorizationHeader(
  authHeader: string | string[] | undefined,
): { token?: string; scheme: Types.OpenLAuthScheme } {
  if (typeof authHeader !== "string") return { scheme: "Token" };
  const match = /^(Token|Bearer)\s+(.+)$/i.exec(authHeader.trim());
  const token = match?.[2] || undefined;
  const inbound = match?.[1]?.toLowerCase() === "bearer" ? "Bearer" : "Token";
  // Rewriting to Token is the historical behaviour and stays the default.
  const scheme: Types.OpenLAuthScheme =
    inbound === "Bearer" && preserveInboundAuthScheme() ? "Bearer" : "Token";
  return { token, scheme };
}

/** Construct a new Studio HTTP session; never share anonymous mutable state. */
export function createHttpSessionClient(baseUrl: string, authHeader?: string | string[]): OpenLClient {
  const { token, scheme } = credentialFromAuthorizationHeader(authHeader);
  return new OpenLClient({
    baseUrl,
    personalAccessToken: token,
    authScheme: scheme,
  });
}

export interface HttpServerOverrides {
  /** Base URL from the positional `<url>` / `--base-url`; wins over the env var. */
  baseUrl?: string;
  /** Test/deployment override; otherwise `MCP_ALLOWED_ORIGINS` is used. */
  allowedOrigins?: ReadonlySet<string>;
  /** Test seam for verifying request/session isolation. */
  clientFactory?: typeof createHttpSessionClient;
}

/** Build the Express app without binding a port, enabling in-process tests. */
export function createHttpApp(overrides: HttpServerOverrides = {}): Express {
  const app = express();
  const baseUrl = resolveHttpBaseUrl(overrides.baseUrl);
  const allowedOrigins = overrides.allowedOrigins ?? resolveAllowedOrigins();
  const clientFactory = overrides.clientFactory ?? createHttpSessionClient;
  const requireBaseUrl = (): string => {
    if (!baseUrl) {
      throw new Error(
        "No OpenL client available. Provide the OpenL Studio base URL as a positional " +
          "argument (openl-mcp <url> --http) or via OPENL_BASE_URL.",
      );
    }
    return baseUrl;
  };

  app.use("/mcp", mcpCorsMiddleware(allowedOrigins));
  app.use(express.json({ limit: resolveMaxBodySize() }));

  // Modern 2026-07-28 is stateless at the protocol layer. A fresh OpenLClient
  // per request prevents cookies, test headers, trace state and caches from
  // leaking between anonymous callers.
  const modernHandler = createMcpHandler(
    (ctx) => createConfiguredServer(
      clientFactory(requireBaseUrl(), ctx.requestInfo?.headers.get("authorization") ?? undefined),
    ),
    {
      legacy: "reject",
      onerror: (error) => console.error("MCP modern transport error:", sanitizeError(error)),
    },
  );
  const modernNodeHandler = toNodeHandler(modernHandler, {
    onerror: (error) => console.error("MCP Node adapter error:", sanitizeError(error)),
  });

  // Legacy 2025 clients retain their sessionful transport because several
  // Studio workflows are session-bound. Each MCP session owns a distinct
  // OpenLClient — including anonymous sessions.
  const legacyTransports = new Map<string, NodeStreamableHTTPServerTransport>();

  const handleLegacyPost = async (req: Request, res: Response): Promise<Response | void> => {
    try {
      const sessionId = req.headers["mcp-session-id"] as string | undefined;
      let transport = sessionId ? legacyTransports.get(sessionId) : undefined;

      if (!transport && !sessionId && isInitializeRequest(req.body)) {
        const newSessionId = randomUUID();
        const client = clientFactory(requireBaseUrl(), req.headers.authorization);
        const sessionServer = createConfiguredServer(client);
        const createdTransport = new NodeStreamableHTTPServerTransport({
          sessionIdGenerator: () => newSessionId,
          onsessioninitialized: (id) => {
            legacyTransports.set(id, createdTransport);
          },
        });
        transport = createdTransport;
        createdTransport.onclose = () => {
          if (createdTransport.sessionId) legacyTransports.delete(createdTransport.sessionId);
        };
        await sessionServer.connect(transport);
      } else if (!transport) {
        const status = sessionId ? 404 : 400;
        return res.status(status).json(jsonRpcError(-32000, "Invalid or missing MCP session ID"));
      }

      await transport.handleRequest(req, res, req.body);
    } catch (error) {
      console.error("Failed to handle legacy MCP request:", sanitizeError(error));
      if (!res.headersSent) res.status(500).json(jsonRpcError(-32603, "Internal server error"));
    }
  };

  const handleLegacySession = async (req: Request, res: Response): Promise<Response | void> => {
    try {
      const sessionId = req.headers["mcp-session-id"] as string | undefined;
      const transport = sessionId ? legacyTransports.get(sessionId) : undefined;
      if (!transport) {
        return res.status(404).json(jsonRpcError(-32000, "Invalid or missing MCP session ID"));
      }
      await transport.handleRequest(req, res);
    } catch (error) {
      console.error("Failed to handle legacy MCP session request:", sanitizeError(error));
      if (!res.headersSent) res.status(500).json(jsonRpcError(-32603, "Internal server error"));
    }
  };

  app.all("/mcp", async (req: Request, res: Response): Promise<Response | void> => {
    const webRequest = await toWebRequest(req, req.body);
    if (await isLegacyRequest(webRequest)) {
      return req.method === "POST" ? handleLegacyPost(req, res) : handleLegacySession(req, res);
    }
    await modernNodeHandler(req, res, req.body);
  });

  // Liveness probe, and the diagnostics endpoint for a deployed HTTP server:
  // `version` alone cannot identify a snapshot build, so the build id and its
  // git/timestamp coordinates ride along. All of it is build-time metadata —
  // no configuration, credentials, or studio URL is exposed on this open route.
  app.get("/health", (_req: Request, res: Response) => {
    const info = versionInfo();
    res.json({
      status: "ok",
      timestamp: new Date().toISOString(),
      service: info.name,
      version: info.version,
      build: info.build,
      runtime: info.runtime,
    });
  });

  app.use((err: Error, _req: Request, res: Response, _next: NextFunction) => {
    console.error("Unhandled error:", sanitizeError(err));
    res.status(500).json({ error: "Internal server error", message: sanitizeError(err) });
  });

  app.use((req: Request, res: Response) => {
    res.status(404).json({ error: "Not found", path: req.path, method: req.method });
  });

  return app;
}

/**
 * Start the Express HTTP transport on `PORT`.
 *
 * `HOST` names the interface to listen on; unset, the server listens on every
 * interface. OpenL Studio sets it to the loopback address, so only Studio's own
 * `/mcp` proxy reaches the server it runs.
 */
export async function startHttpServer(overrides: HttpServerOverrides = {}): Promise<HttpServer> {
  const port = process.env.PORT || DEFAULT_PORT;
  const host = process.env.HOST?.trim() || undefined;
  const app = createHttpApp(overrides);
  return new Promise((resolve) => {
    const server = createServer(app).listen({ port: Number(port), host }, () => {
      // Name the exact build in the startup line: container logs are often the
      // only record of which build a deployment is running.
      console.log(`${versionLine()} listening on ${host ?? "every interface"}, port ${port}`);
      resolve(server);
    });
  });
}
