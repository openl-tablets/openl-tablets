package org.openl.studio.projects.messaging;

/**
 * A debug status change pushed over WebSocket.
 *
 * <p>Sessions of the same user and table share one notification topic, so the message carries the id of
 * the session it belongs to — the same id the session's stack views report. A client compares it with
 * the session it watches and drops foreign events, such as the terminal event of a stale session reaped
 * in the background.
 *
 * @param status    the new debug status code (for example {@code suspended} or {@code terminated})
 * @param sessionId identity of the debug session the status belongs to
 */
public record TraceDebugStatusMessage(String status, String sessionId) {
}
