/**
 * Waiting inside one tool call for work Studio does in the background: the reads
 * of a result Studio answers with `202 notReady` until it is ready, and the
 * progress notifications that tell the client the wait goes on.
 */
import { setTimeout as sleep } from "node:timers/promises";
import { ProtocolError, ProtocolErrorCode } from "@modelcontextprotocol/server";

import type { ResultNotReadyView } from "../types.js";
import { isResultNotReady } from "../utils.js";
import { isRequestTimeout } from "../http.js";
import type { ToolHandlerExtra } from "./common.js";

const WAIT_POLL_INITIAL_INTERVAL_MS = 250;
const WAIT_POLL_MAX_INTERVAL_MS = 2_000;

/** How {@link waitForResult} waits for a result. */
export interface WaitOptions {
  /** The longest time to wait, in milliseconds. */
  timeoutMs: number;
  /** The progress notification sent while the result is not ready. */
  progressMessage: string;
  /** The message of the error thrown when the result is not ready in time. */
  timeoutMessage: string;
}

/** What one read of a result may take: the signal of the call, and the time left to wait. */
interface ResultRequest {
  signal?: AbortSignal;
  timeoutMs: number;
}

/** Tells the client, when it asked for progress, that a wait started at `startedAt` goes on. Best-effort. */
export function reportProgress(extra: ToolHandlerExtra | undefined, startedAt: number, message: string): void {
  const progressToken = extra?._meta?.progressToken;
  if (progressToken !== undefined && extra?.sendNotification) {
    void extra.sendNotification({
      method: "notifications/progress",
      params: { progressToken, progress: Math.round((Date.now() - startedAt) / 1000), message },
    }).catch(() => { /* progress reporting is best-effort */ });
  }
}

/**
 * Reads a result Studio produces in the background, which it answers with `202 notReady` until the result is ready.
 *
 * The result is read again after a growing pause, and a progress notification tells the client the wait goes on.
 * The wait ends with the result, with a `timeoutMessage` error once `timeoutMs` has passed, or with an `AbortError`
 * when the client cancels the call.
 *
 * @param read - Reads the result with the signal of the call and the time one request may take.
 */
export async function waitForResult<T>(
  read: (request: ResultRequest) => Promise<T | ResultNotReadyView>,
  options: WaitOptions,
  extra?: ToolHandlerExtra,
): Promise<T> {
  const startedAt = Date.now();
  const deadline = startedAt + options.timeoutMs;

  // One read, then, while the result is not ready, a pause twice as long as the previous one before the next read
  const poll = async (pollIntervalMs: number): Promise<T> => {
    const result = await readOnce(read, deadline, options.timeoutMessage, extra?.signal);
    if (!isResultNotReady(result)) {
      return result;
    }

    reportProgress(extra, startedAt, options.progressMessage);

    // An unreferenced timer does not keep the process alive; a cancelled call rejects with an AbortError
    const pauseMs = Math.min(pollIntervalMs, Math.max(deadline - Date.now(), 0));
    await sleep(pauseMs, undefined, { signal: extra?.signal, ref: false });
    return poll(Math.min(pollIntervalMs * 2, WAIT_POLL_MAX_INTERVAL_MS));
  };
  return poll(WAIT_POLL_INITIAL_INTERVAL_MS);
}

/**
 * Reads a result once, with the time left until `deadline`. A read after the deadline, or one that times out, fails
 * with the `timeoutMessage`; a read of a cancelled call fails with an `AbortError`.
 */
async function readOnce<T>(
  read: (request: ResultRequest) => Promise<T | ResultNotReadyView>,
  deadline: number,
  timeoutMessage: string,
  signal: AbortSignal | undefined,
): Promise<T | ResultNotReadyView> {
  if (signal?.aborted) {
    throw new DOMException("wait aborted", "AbortError");
  }
  const remainingMs = deadline - Date.now();
  if (remainingMs <= 0) {
    throw new ProtocolError(ProtocolErrorCode.InvalidRequest, timeoutMessage);
  }
  try {
    return await read({ signal, timeoutMs: remainingMs });
  } catch (error) {
    if (signal?.aborted) {
      throw new DOMException("wait aborted", "AbortError");
    }
    if (Date.now() >= deadline || isRequestTimeout(error)) {
      throw new ProtocolError(ProtocolErrorCode.InvalidRequest, timeoutMessage);
    }
    throw error;
  }
}
