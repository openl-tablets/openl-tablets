/**
 * Minimal STOMP client for the OpenL Studio websocket topics.
 *
 * The studio publishes per-user notifications via Spring's
 * `convertAndSendToUser` (see `ProjectSocketNotificationService`): project
 * compile status on `/topic/projects/{encoded}[/branches/{encoded}]/status`
 * (`ProjectStatusViewModel` JSON). The broker prefix is `/user`, the broker
 * resolves the principal from the WebSocket session, and STOMP CONNECT frames
 * carry no credentials — authentication piggybacks on the HTTP handshake's
 * JSESSIONID cookie / Authorization header.
 *
 * This module is intentionally narrow: one subscription per call, no pooling,
 * no global state. It's invoked from `stomp-waits.ts` inside a single tool
 * invocation and torn down when the call returns.
 */

import { Client, IFrame, IMessage } from "@stomp/stompjs";
import WebSocket from "ws";

import type * as Types from "./types.js";

/**
 * Opt-in verbose STOMP wire logging. Set `DEBUG_STOMP=true` on the container
 * to trace WebSocket URL construction, CONNECT/CONNECTED events, SUBSCRIBE
 * frames, every inbound frame, and disconnect causes on stderr. Errors are
 * always logged.
 */
const DEBUG = process.env.DEBUG_STOMP === "true";

function debug(message: string, context?: Record<string, unknown>): void {
  if (!DEBUG) return;
  console.error(`[STOMP] ${message}`, context ? JSON.stringify(context) : "");
}

export interface SubscribeProjectStatusOpts {
  /** Studio base URL (e.g. `http://host.docker.internal:8080/rest`). The `/rest` segment is stripped before appending `/ws`. */
  studioBaseUrl: string;
  /**
   * Full `Cookie` header value, e.g. `JSESSIONID=abc123`. Carries per-session
   * state (compile registry, workspace) when the studio's REST chain saves a
   * session — required so the same studio session services both the seed
   * HTTP fetch and the WS-side subscription state.
   */
  cookieHeader: string;
  /**
   * Optional `Authorization` header value to attach to the WS upgrade
   * request (e.g. `"Token openl_pat_…"`).
   * When present, the studio's REST filter chain authenticates the upgrade
   * the same way it authenticates every `/rest/*` request — the resulting
   * principal propagates to the WS session via Spring's
   * `DefaultHandshakeHandler.determineUser`, which lets STOMP `SUBSCRIBE`
   * frames on `/user/topic/...` pass `AuthorizationChannelInterceptor`'s
   * `.authenticated()` check. Without it, the WS session is anonymous and
   * subscribes to user-routed destinations are rejected.
   */
  authorizationHeader?: string;
  /** Already-decoded project identifier. URL-encoded here for the destination path. */
  projectId: string;
  /** Already-decoded branch name. Optional — when omitted, subscribes to the no-branch destination. */
  branch?: string;
  /** Called once per STOMP message received on the topic. The payload is parsed as ProjectStatusView. */
  onMessage: (status: Types.ProjectStatusView) => void;
  /** Optional non-fatal error sink. STOMP/WS errors are reported here; the subscription stays open until `close()` or `signal`. */
  onError?: (error: Error) => void;
  /** Aborting deactivates the underlying STOMP client. */
  signal?: AbortSignal;
}

export interface Subscription {
  close(): Promise<void>;
}

/**
 * Destination-agnostic variant of {@link SubscribeProjectStatusOpts}: same
 * connection/auth/lifecycle options, but the caller supplies the full STOMP
 * destination and receives RAW frame bodies (no payload parsing).
 */
interface SubscribeTopicOpts {
  studioBaseUrl: string;
  cookieHeader: string;
  authorizationHeader?: string;
  /** Full STOMP destination including the `/user` prefix (already URL-encoded). */
  destination: string;
  /** Called once per STOMP frame with the raw body text. */
  onFrame: (body: string) => void;
  onError?: (error: Error) => void;
  signal?: AbortSignal;
}

/**
 * Open a STOMP subscription to the project-status topic and call `onMessage`
 * with the parsed `ProjectStatusView` for each frame. Thin wrapper over
 * {@link subscribeTopic} that builds the status destination and parses JSON.
 */
export async function subscribeProjectStatus(
  opts: SubscribeProjectStatusOpts
): Promise<Subscription> {
  return subscribeTopic({
    studioBaseUrl: opts.studioBaseUrl,
    cookieHeader: opts.cookieHeader,
    authorizationHeader: opts.authorizationHeader,
    destination: buildDestination(opts.projectId, opts.branch),
    signal: opts.signal,
    onError: opts.onError,
    onFrame: (body) => {
      try {
        opts.onMessage(JSON.parse(body) as Types.ProjectStatusView);
      } catch (err) {
        opts.onError?.(
          err instanceof Error
            ? err
            : new Error(`Failed to parse STOMP frame: ${String(err)}`)
        );
      }
    },
  });
}

/**
 * Open a STOMP subscription to an arbitrary studio topic and call `onFrame`
 * with each raw frame body.
 *
 * Resolves once the underlying STOMP CONNECT has succeeded and the SUBSCRIBE
 * frame has been sent. Rejects if the WebSocket / STOMP handshake fails — the
 * caller should treat that as a hard error (typically: missing/expired cookie,
 * wrong URL, or studio not reachable).
 */
async function subscribeTopic(
  opts: SubscribeTopicOpts
): Promise<Subscription> {
  const wsUrl = deriveWsUrl(opts.studioBaseUrl);
  const destination = opts.destination;
  debug("subscribe.config", {
    wsUrl,
    destination,
    cookiePrefix: opts.cookieHeader.substring(0, 24) + "…",
  });

  const client = new Client({
    webSocketFactory: () => {
      const headers: Record<string, string> = { Cookie: opts.cookieHeader };
      if (opts.authorizationHeader) {
        headers.Authorization = opts.authorizationHeader;
      }
      debug("ws.connecting", {
        wsUrl,
        hasAuthorization: Boolean(opts.authorizationHeader),
      });
      // Node's `ws` accepts a `headers` option on construction; the browser
      // WebSocket does not. The `IStompSocket` interface is structurally
      // compatible — cast through `unknown` to satisfy the SDK's type.
      return new WebSocket(wsUrl, { headers }) as unknown as WebSocket;
    },
    // No reconnect: every subscription is scoped to a single tool wait and
    // torn down when the call returns.
    reconnectDelay: 0,
    heartbeatIncoming: 10_000,
    heartbeatOutgoing: 10_000,
  });

  return new Promise<Subscription>((resolve, reject) => {
    let connected = false;

    const abortHandler = (): void => {
      debug("abort.received → deactivating", { destination });
      void client.deactivate();
    };
    opts.signal?.addEventListener("abort", abortHandler, { once: true });

    const cleanup = (): void => {
      opts.signal?.removeEventListener("abort", abortHandler);
    };

    client.onConnect = () => {
      debug("stomp.connected → sending SUBSCRIBE", { destination });
      try {
        client.subscribe(destination, (frame: IMessage) => {
          debug("frame.received", {
            destination,
            bytes: frame.body?.length ?? 0,
          });
          try {
            opts.onFrame(frame.body);
          } catch (err) {
            opts.onError?.(
              err instanceof Error
                ? err
                : new Error(`STOMP frame handler failed: ${String(err)}`)
            );
          }
        });
        connected = true;
        resolve({
          close: async () => {
            debug("close.requested → deactivating", { destination });
            cleanup();
            await client.deactivate();
            debug("close.done", { destination });
          },
        });
      } catch (err) {
        cleanup();
        void client.deactivate();
        reject(
          err instanceof Error
            ? err
            : new Error(`Failed to subscribe: ${String(err)}`)
        );
      }
    };

    client.onDisconnect = () => {
      debug("stomp.disconnected", { destination });
    };

    client.onStompError = (frame: IFrame) => {
      const message = frame.headers["message"] ?? "STOMP error";
      const err = new Error(`STOMP error: ${message}`);
      debug("stomp.error", { destination, message, connected });
      if (!connected) {
        cleanup();
        reject(err);
      } else {
        opts.onError?.(err);
      }
    };

    client.onWebSocketError = (event: Event) => {
      // `event` here is whatever the underlying socket emits; ws gives us an Error-like shape.
      const err =
        event instanceof Error
          ? event
          : new Error(`WebSocket error: ${String((event as { message?: string }).message ?? event)}`);
      debug("ws.error", { wsUrl, error: err.message, connected });
      if (!connected) {
        cleanup();
        reject(err);
      } else {
        opts.onError?.(err);
      }
    };

    client.activate();
  });
}

/**
 * Build the STOMP destination for a project's status topic.
 *
 * Mirrors `ProjectSocketNotificationService.notifyProjectStatus` in the studio
 * — the broker resolves the `/user` prefix from the session principal, so the
 * subscriber path includes only the `/user/topic/...` part without a username
 * segment.
 */
export function buildDestination(projectId: string, branch?: string): string {
  const encodedProject = encodeURIComponent(projectId);
  if (branch && branch.length > 0) {
    return `/user/topic/projects/${encodedProject}/branches/${encodeURIComponent(branch)}/status`;
  }
  return `/user/topic/projects/${encodedProject}/status`;
}

/**
 * Translate an HTTP studio base URL into the WebSocket URL for the STOMP endpoint.
 *
 * - Scheme: `http`→`ws`, `https`→`wss`.
 * - Path: append `/ws` to the existing `/rest` API root, yielding `/rest/ws`.
 *   This puts the STOMP handshake inside the REST security filter chain
 *   (httpBasic / PAT / session creation), so the authenticated principal
 *   from the request gets propagated to the WS session — required for
 *   multi-user mode subscribes to `/user/topic/...` destinations to be
 *   authorized. The studio UI itself uses `/web/ws` (the legacy `permitAll`
 *   path), which works for single-user mode but breaks STOMP-side
 *   authorization in multi-user mode.
 */
function deriveWsUrl(httpBaseUrl: string): string {
  const url = new URL(httpBaseUrl);
  url.protocol = url.protocol === "https:" ? "wss:" : "ws:";
  // Append `/ws` to the existing path. `OpenLClient.normalizeOpenLBaseUrl`
  // guarantees the base ends with `/rest`, so the result is `/rest/ws`.
  url.pathname = `${url.pathname.replace(/\/$/, "")}/ws`;
  return url.toString();
}
