/**
 * HTTP transport for OpenL MCP Server, on the `node:http` server of Node.js.
 *
 * The `/mcp` endpoint serves both protocol eras: modern MCP 2026-07-28 via
 * `createMcpHandler`, and sessionful 2025-era clients through the legacy
 * Streamable HTTP transport. OpenL Studio passes a request on once its security
 * and its CORS settings have let it through.
 */

import { randomUUID } from "node:crypto";
import { once } from "node:events";
import type { AddressInfo } from "node:net";
import {
  createServer,
  type IncomingMessage,
  type RequestListener,
  type Server as HttpServer,
  type ServerResponse,
} from "node:http";
import {
  createMcpHandler,
  isInitializeRequest,
  isJsonContentType,
  isLegacyRequest,
  JSONRPC_VERSION,
  ProtocolErrorCode,
} from "@modelcontextprotocol/server";
import {
  NodeStreamableHTTPServerTransport,
  toNodeHandler,
  toWebRequest,
} from "@modelcontextprotocol/node";
import { OpenLClient } from "./client.js";
import { createConfiguredServer } from "./mcp-core.js";
import { sanitizeError } from "./utils.js";
import { SERVER_INFO } from "./constants.js";

/** The port the server listens on when `PORT` is unset or blank. */
const DEFAULT_PORT = 3000;
/** The largest JSON body a request may carry: 5 MB. */
const MAX_BODY_BYTES = 5 * 1024 * 1024;
/** The JSON-RPC server error of a request that names no session the server knows. */
const UNKNOWN_SESSION = -32000;
/** How long a legacy session may stay unused before it is closed: the HTTP session timeout of OpenL Studio. */
export const LEGACY_SESSION_IDLE_MS = 30 * 60 * 1000;

/** The session of a legacy 2025 client. */
interface LegacySession {
  transport: NodeStreamableHTTPServerTransport;
  /** The requests of the session in progress, its stream of server messages included. */
  inProgress: number;
  /** When the session opened, or its last request ended. */
  usedAt: number;
}

function jsonRpcError(code: number, message: string): Record<string, unknown> {
  return { jsonrpc: JSONRPC_VERSION, error: { code, message }, id: null };
}

function sendJson(res: ServerResponse, status: number, body: unknown): void {
  res.writeHead(status, { "Content-Type": "application/json; charset=utf-8" });
  res.end(JSON.stringify(body));
}

/** The JSON body of a request, or the answer that refuses it. */
async function readJsonBody(
  req: IncomingMessage,
): Promise<{ body?: unknown; refusal?: { status: number; code: number; message: string } }> {
  const chunks: Buffer[] = [];
  let size = 0;
  for await (const chunk of req as AsyncIterable<Buffer>) {
    size += chunk.length;
    // Read on past the limit, so that the client gets the answer instead of a reset connection.
    if (size <= MAX_BODY_BYTES) chunks.push(chunk);
  }
  if (size > MAX_BODY_BYTES) {
    return { refusal: { status: 413, code: ProtocolErrorCode.InvalidRequest, message: "Request body exceeds 5 MB" } };
  }
  const text = Buffer.concat(chunks).toString("utf8");
  if (!text.trim()) return {};
  try {
    return { body: JSON.parse(text) };
  } catch {
    return { refusal: { status: 400, code: ProtocolErrorCode.ParseError, message: "Parse error: the body is not valid JSON" } };
  }
}

/**
 * The client of OpenL Studio a request to the MCP server works with, at the address of Studio the request names: the
 * `Authorization` header of the request, which OpenL Studio has checked at `/mcp`, and the address of Studio its
 * caller opened, which OpenL Studio passes in the `OpenL-Public-Url` header. Never share anonymous mutable state.
 */
export function createHttpSessionClient(baseUrl: string, headers?: Headers): OpenLClient {
  return new OpenLClient({
    baseUrl,
    authorization: headers?.get("authorization") ?? undefined,
    publicUrl: headers?.get("openl-public-url") ?? undefined,
  });
}

/** The server of a session, built for the headers of the request that opens it. */
type SessionServerFactory = (headers: Headers) => ReturnType<typeof createConfiguredServer>;

/**
 * The handler of the requests of legacy 2025 clients: an initializing POST opens a session, every other request names
 * its session.
 *
 * Most clients never close their session, so opening one closes the others no request has used for
 * `LEGACY_SESSION_IDLE_MS`. A session with a request in progress, such as an open stream of server messages, is in use.
 */
function createLegacyHandler(
  serverFor: SessionServerFactory,
): (req: IncomingMessage, res: ServerResponse, body: unknown, headers: Headers) => Promise<void> {
  const sessions = new Map<string, LegacySession>();

  const closeIdleSessions = (): void => {
    const idleSince = Date.now() - LEGACY_SESSION_IDLE_MS;
    for (const { transport, inProgress, usedAt } of sessions.values()) {
      if (inProgress === 0 && usedAt < idleSince) {
        transport.close().catch((error: unknown) => {
          console.error("Failed to close an idle MCP session:", sanitizeError(error));
        });
      }
    }
  };

  const openSession = async (headers: Headers): Promise<LegacySession> => {
    closeIdleSessions();
    const transport = new NodeStreamableHTTPServerTransport({
      sessionIdGenerator: randomUUID,
      onsessioninitialized: (id) => {
        sessions.set(id, session);
      },
    });
    const session: LegacySession = { transport, inProgress: 0, usedAt: Date.now() };
    transport.onclose = () => {
      if (transport.sessionId) sessions.delete(transport.sessionId);
    };
    await serverFor(headers).connect(transport);
    return session;
  };

  // A session is in use until the response of its request ends.
  const serveInSession = async (
    session: LegacySession,
    req: IncomingMessage,
    res: ServerResponse,
    body: unknown,
  ): Promise<void> => {
    session.inProgress++;
    res.once("close", () => {
      session.inProgress--;
      session.usedAt = Date.now();
    });
    await session.transport.handleRequest(req, res, body);
  };

  return async (req, res, body, headers) => {
    try {
      const sessionId = headers.get("mcp-session-id") ?? undefined;
      const opens = !sessionId && req.method === "POST";
      let session: LegacySession | undefined;
      if (sessionId) {
        session = sessions.get(sessionId);
      } else if (opens && isInitializeRequest(body)) {
        session = await openSession(headers);
      }
      if (!session) {
        sendJson(res, opens ? 400 : 404, jsonRpcError(UNKNOWN_SESSION, "Invalid or missing MCP session ID"));
        return;
      }
      await serveInSession(session, req, res, body);
    } catch (error) {
      console.error("Failed to handle legacy MCP request:", sanitizeError(error));
      if (!res.headersSent) sendJson(res, 500, jsonRpcError(ProtocolErrorCode.InternalError, "Internal server error"));
    }
  };
}

interface HttpServerOverrides {
  /** The address of OpenL Studio for a request that names none in `OpenL-Base-Url`: a test seam. */
  baseUrl?: string;
  /** Test seam for verifying request/session isolation. */
  clientFactory?: typeof createHttpSessionClient;
}

/** Build the request listener of the HTTP transport without binding a port, enabling in-process tests. */
export function createHttpApp(overrides: HttpServerOverrides = {}): RequestListener {
  const clientFactory = overrides.clientFactory ?? createHttpSessionClient;
  // OpenL Studio passes the address it is called at with every request: the one the request came in through.
  const clientOf = (headers?: Headers): OpenLClient => {
    const baseUrl = headers?.get("openl-base-url") ?? overrides.baseUrl;
    if (!baseUrl || !URL.canParse(baseUrl)) {
      throw new Error("No OpenL client available: the request names no valid OpenL Studio address in OpenL-Base-Url.");
    }
    return clientFactory(baseUrl, headers);
  };

  // Modern 2026-07-28 is stateless at the protocol layer. A fresh OpenLClient
  // per request keeps its cache of repositories from leaking between callers.
  const modernHandler = createMcpHandler(
    (ctx) => createConfiguredServer(clientOf(ctx.requestInfo?.headers)),
    {
      legacy: "reject",
      onerror: (error) => console.error("MCP modern transport error:", sanitizeError(error)),
    },
  );
  const modernNodeHandler = toNodeHandler(modernHandler, {
    onerror: (error) => console.error("MCP Node adapter error:", sanitizeError(error)),
  });

  // The protocol of legacy 2025 clients is sessionful. Each MCP session owns a
  // distinct OpenLClient — including anonymous sessions.
  const handleLegacy = createLegacyHandler((headers) => createConfiguredServer(clientOf(headers)));

  const handleMcp = async (req: IncomingMessage, res: ServerResponse): Promise<void> => {
    let body: unknown;
    if (isJsonContentType(req.headers["content-type"])) {
      const read = await readJsonBody(req);
      if (read.refusal) {
        sendJson(res, read.refusal.status, jsonRpcError(read.refusal.code, read.refusal.message));
        return;
      }
      body = read.body;
    }
    // The parsed body spares the classification a second read of the request.
    const webRequest = await toWebRequest(req, body);
    if (await isLegacyRequest(webRequest, body)) {
      await handleLegacy(req, res, body, webRequest.headers);
      return;
    }
    await modernNodeHandler(req, res, body);
  };

  const route = async (req: IncomingMessage, res: ServerResponse): Promise<void> => {
    const path = new URL(req.url ?? "/", "http://localhost").pathname;
    if (path === "/mcp" || path === "/mcp/") {
      await handleMcp(req, res);
    } else {
      sendJson(res, 404, { error: "Not found", path, method: req.method });
    }
  };

  return (req, res) => {
    route(req, res).catch((error: unknown) => {
      console.error("Unhandled error:", sanitizeError(error));
      if (!res.headersSent) sendJson(res, 500, { error: "Internal server error", message: sanitizeError(error) });
    });
  };
}

/**
 * Start the HTTP transport on `PORT`, or on a free port when it is `0`.
 *
 * `HOST` names the interface to listen on; unset, the server listens on every
 * interface. OpenL Studio sets it to the loopback address, so only its `/mcp`
 * servlet reaches the server it runs, and `PORT` to `0`: it reads the port the
 * server took from the line logged once the server listens.
 */
export async function startHttpServer(): Promise<HttpServer> {
  const host = process.env.HOST?.trim() || undefined;
  const server = createServer(createHttpApp()).listen({ port: Number(process.env.PORT || DEFAULT_PORT), host });
  // A port that cannot be taken rejects the start instead of crashing on an unhandled error event.
  await once(server, "listening");
  const { port } = server.address() as AddressInfo;
  // OpenL Studio reads the port from the end of this line.
  console.log(`${SERVER_INFO.NAME} ${SERVER_INFO.VERSION} listening on ${host ?? "every interface"}, port ${port}`);
  return server;
}
