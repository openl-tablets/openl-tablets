package org.openl.studio.projects.service.trace;
import org.openl.studio.projects.model.trace.DebugStatus;

/**
 * Receives debug status changes as they happen on the worker thread.
 *
 * <p>Used to push events (for example over WebSocket) when execution suspends or finishes, including
 * asynchronous transitions such as reaching a breakpoint during a resume. Callbacks run on the worker
 * thread outside any lock, so implementations must be quick and must not block.
 */
public interface DebugListener {

    DebugListener NOOP = status -> {
    };

    void onStatusChanged(DebugStatus status);
}
