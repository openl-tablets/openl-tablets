/**
 * Wait for the studio's asynchronous compilation to finish — over the STOMP
 * WebSocket, inside a single tool call, instead of polling.
 *
 * {@link waitForCompilation} blocks on the project-status topic until the
 * compile state is terminal. An optional idle path first initializes Studio's
 * lazy model through the tables API; an already-running job uses STOMP
 * (subscribe → close the no-replay race over HTTP → await a terminal frame,
 * bounded by timeout + abort, always unsubscribe).
 * The bounded, abortable, self-cleaning wait for a single out-of-band value
 * lives in {@link awaitTerminal}.
 *
 * See docs/development/websockets.md for why WebSockets are used at all.
 */

import {
  subscribeProjectStatus,
  Subscription,
} from "./stomp-client.js";
import { OpenLClient } from "./client.js";
import type * as Types from "./types.js";
import { isAxiosError } from "./utils.js";

const DEFAULT_TIMEOUT_MS = 120_000;

// =============================================================================
// Shared wait primitive
// =============================================================================

/**
 * Internal sentinel: the bounded terminal wait elapsed. The caller maps it to
 * a domain outcome (compile → return the last snapshot). Never escapes this
 * module.
 */
class TerminalWaitTimeout extends Error {
  constructor() {
    super("terminal wait timed out");
    this.name = "TerminalWaitTimeout";
  }
}

/** Conventional AbortError shape so callers can `if (err.name === 'AbortError')`. */
function makeAbortError(operation: string): Error {
  const err = new Error(`${operation} aborted`);
  err.name = "AbortError";
  return err;
}

/**
 * Await a single terminal value delivered out-of-band by a STOMP frame handler,
 * with a bounded timeout and abort support. Centralises the timer (unref'd so it
 * never keeps the event loop alive), abort wiring, and listener/timer cleanup on
 * every exit path.
 *
 * `register` is invoked synchronously with the resolver the frame handler should
 * call once a terminal value arrives (typically stored in a `let` the handler
 * closes over). Create this only when you actually need to block: callers do the
 * HTTP race-close check first and skip the wait — and its timer — when already
 * resolved.
 *
 * Rejects with {@link TerminalWaitTimeout} on timeout and an AbortError on abort.
 */
function awaitTerminal<T>(opts: {
  timeoutMs: number;
  signal?: AbortSignal;
  abortOperation: string;
  register: (resolve: (value: T) => void) => void;
}): Promise<T> {
  return new Promise<T>((resolve, reject) => {
    let abortHandler: (() => void) | null = null;

    const settle = (fn: () => void): void => {
      clearTimeout(timer);
      if (abortHandler) {
        opts.signal?.removeEventListener("abort", abortHandler);
      }
      fn();
    };

    const timer = setTimeout(() => {
      settle(() => reject(new TerminalWaitTimeout()));
    }, opts.timeoutMs);
    // Don't keep the Node event loop alive just because of this timer.
    if (typeof (timer as { unref?: () => void }).unref === "function") {
      (timer as unknown as { unref: () => void }).unref();
    }

    abortHandler = (): void => {
      settle(() => reject(makeAbortError(opts.abortOperation)));
    };
    opts.signal?.addEventListener("abort", abortHandler, { once: true });

    opts.register((value) => settle(() => resolve(value)));
  });
}

// =============================================================================
// Compilation wait (openl_project_status wait:true)
// =============================================================================

/**
 * States the wait flow treats as "no need to wait" when lazy initialization is
 * not requested — everything except `compiling`. `idle` has no registered job,
 * so subscribing without first triggering compilation would hang.
 */
const RESOLVED_STATES: ReadonlySet<Types.CompileState> = new Set([
  "idle",
  "ok",
  "warnings",
  "errors",
]);

export interface WaitForCompilationOptions {
  /** Called for every non-terminal status snapshot received over STOMP. Use it to emit MCP progress notifications. */
  onProgress?: (status: Types.ProjectStatusView) => void;
  /** When aborted, the call rejects with `AbortError` and the STOMP subscription is torn down. */
  signal?: AbortSignal;
  /** Hard cap on wait time. On expiry, the last-seen status is returned (no error). Default 120000 ms. */
  timeoutMs?: number;
  /** Start Studio's lazy compilation when no model is registered yet. */
  compileOnIdle?: boolean;
}

/** Test seam: lets unit tests inject a fake STOMP subscriber without touching the network. */
export type SubscribeFn = typeof subscribeProjectStatus;

/**
 * True for any state that doesn't warrant waiting — terminal outcomes
 * (`ok`/`warnings`/`errors`) and `idle` (no compile registered, nothing to
 * wait for). The wait flow only blocks when `compileState === "compiling"`.
 */
export function isResolvedCompileState(state: Types.CompileState): boolean {
  return RESOLVED_STATES.has(state);
}

/**
 * Block until an OpenL project's compilation reaches a terminal state.
 *
 * Combines an HTTP snapshot from `OpenLClient.getProjectStatus` with a per-call
 * STOMP subscription on the studio's status topic. The flow:
 *
 *   1. HTTP fetch (no `branch` query — backend returns the project's actual
 *      branch, also seeds the JSESSIONID cookie needed for the WS handshake).
 *   2. If the caller requested a branch that differs from the project's actual
 *      branch, silently switch (re-using {@link OpenLClient.switchBranch} when
 *      the project is already opened, falling back to
 *      {@link OpenLClient.openProject} otherwise — same pattern as the existing
 *      `openl_open_project` handler). Re-fetch status after the switch.
 *   3. Resolved? return immediately, unless `idle` should start lazy compilation.
 *   4. If idle, call the tables API to initialize and await compilation, then
 *      re-fetch status. No STOMP subscription is needed because that API waits.
 *   5. Otherwise STOMP subscribe using the project's *actual* branch (so the destination
 *      matches the topic the studio publishes to — branch-supporting projects
 *      get `/user/topic/projects/<id>/branches/<branch>/status`; others get
 *      the no-branch variant).
 *   6. HTTP fetch again — closes the race where the terminal status arrived
 *      between steps 1 and 4.
 *   7. Terminal? unsubscribe + return.
 *   8. Wait for a terminal STOMP message (or timeout / abort).
 *   9. Always unsubscribe in `finally`.
 *
 * The race in step 6 is unavoidable for an already-running compilation: it was
 * triggered before this call, so a terminal event may already be in flight.
 */
export async function waitForCompilation(
  client: OpenLClient,
  projectId: string,
  requestedBranch: string | undefined,
  options: WaitForCompilationOptions = {},
  // Allow tests to inject a fake STOMP implementation.
  subscribeImpl: SubscribeFn = subscribeProjectStatus,
): Promise<Types.ProjectStatusView> {
  const timeoutMs = options.timeoutMs ?? DEFAULT_TIMEOUT_MS;
  const startedAt = Date.now();
  const remainingTimeoutMs = (): number => Math.max(0, timeoutMs - (Date.now() - startedAt));

  if (options.signal?.aborted) {
    throw makeAbortError("waitForCompilation");
  }

  // Step 1: initial fetch WITHOUT the branch query — the backend would otherwise
  // 409 on mismatch, but we want to detect the mismatch and switch instead.
  // The response also seeds the JSESSIONID cookie needed for the WS handshake.
  let initial = await client.getProjectStatus(projectId);

  // Step 2: silent branch switch if the caller asked for a branch that differs
  // from the project's actual branch. `initial.branch` is populated only for
  // projects that support branches (see `ProjectStatusMapperImpl.map`), so a
  // missing `initial.branch` means there's nothing to switch.
  if (
    requestedBranch &&
    initial.branch &&
    requestedBranch !== initial.branch
  ) {
    await switchToBranch(client, projectId, requestedBranch);
    initial = await client.getProjectStatus(projectId);
  }

  if (isResolvedCompileState(initial.compileState) && !(initial.compileState === "idle" && options.compileOnIdle)) {
    return initial;
  }

  if (initial.compileState === "idle" && options.compileOnIdle) {
    options.onProgress?.(initial);
    try {
      const triggerTimeoutMs = remainingTimeoutMs();
      if (triggerTimeoutMs === 0) {
        return initial;
      }
      await client.triggerProjectCompilation(projectId, {
        signal: options.signal,
        timeoutMs: triggerTimeoutMs,
      });
    } catch (error) {
      if (options.signal?.aborted) {
        throw makeAbortError("waitForCompilation");
      }
      if (isAxiosError(error) && error.code === "ECONNABORTED") {
        // The Studio request may keep compiling after the HTTP wait expires.
        // Preserve the wait contract by returning its latest snapshot rather
        // than turning a timeout into a tool failure.
        return client.getProjectStatus(projectId);
      }
      throw error;
    }
    initial = await client.getProjectStatus(projectId);
    if (isResolvedCompileState(initial.compileState)) {
      return initial;
    }
  }

  const cookie = client.getSessionCookie();
  if (!cookie) {
    // The HTTP fetch above should have set the cookie; if it didn't, the studio
    // isn't issuing a session and we can't open the WS. Fall back to the latest
    // snapshot so the caller at least gets the current state.
    return initial;
  }

  // Subscribe with the project's ACTUAL branch (after any switch above). The
  // studio publishes to either `/topic/projects/<id>/branches/<branch>/status`
  // or `/topic/projects/<id>/status` depending on `project.isSupportsBranches`
  // — mirror that exactly here.
  const actualBranch = initial.branch;

  let lastSeen: Types.ProjectStatusView = initial;
  let resolveTerminal: ((status: Types.ProjectStatusView) => void) | null = null;

  const handleMessage = (status: Types.ProjectStatusView): void => {
    lastSeen = status;
    if (isResolvedCompileState(status.compileState)) {
      resolveTerminal?.(status);
      resolveTerminal = null;
    } else {
      options.onProgress?.(status);
    }
  };

  let subscription: Subscription | null = null;
  try {
    subscription = await subscribeImpl({
      studioBaseUrl: client.getBaseUrl(),
      cookieHeader: `JSESSIONID=${cookie}`,
      authorizationHeader: client.getAuthorizationHeader(),
      projectId,
      branch: actualBranch,
      onMessage: handleMessage,
      signal: options.signal,
    });

    // Step 6: race-close. Status may have flipped to terminal between step 1 and the subscribe.
    const afterSubscribe = await client.getProjectStatus(projectId);
    lastSeen = afterSubscribe;
    if (isResolvedCompileState(afterSubscribe.compileState)) {
      return afterSubscribe;
    }

    // Step 8: wait for terminal via STOMP, with timeout + abort. On timeout
    // return the last-seen snapshot rather than erroring.
    try {
      const waitTimeoutMs = remainingTimeoutMs();
      if (waitTimeoutMs === 0) {
        return lastSeen;
      }
      return await awaitTerminal<Types.ProjectStatusView>({
        timeoutMs: waitTimeoutMs,
        signal: options.signal,
        abortOperation: "waitForCompilation",
        register: (resolve) => { resolveTerminal = resolve; },
      });
    } catch (err) {
      if (err instanceof TerminalWaitTimeout) {
        return lastSeen;
      }
      throw err;
    }
  } finally {
    if (subscription) {
      await subscription.close().catch(() => { /* best-effort teardown */ });
    }
  }
}

/**
 * Silently switch the project to `targetBranch`. Mirrors the dispatch logic in
 * the `openl_open_project` handler — uses `switchBranch` (PATCH without status)
 * when the project is already opened, and falls back to `openProject` (PATCH
 * with status: OPENED) when it is closed. A single extra `getProject` call is
 * the cost of avoiding 409 Conflicts.
 */
async function switchToBranch(
  client: OpenLClient,
  projectId: string,
  targetBranch: string,
): Promise<void> {
  const project = await client.getProject(projectId);
  if (project.status === "OPENED" || project.status === "EDITING") {
    await client.switchBranch(projectId, targetBranch);
  } else {
    await client.openProject(projectId, { branch: targetBranch });
  }
}
