package org.openl.studio.projects.service.trace;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import org.openl.rules.webstudio.web.trace.debug.TraceDebugger;
import org.openl.studio.projects.model.ProjectIdModel;

/**
 * One interactive debug session: a running (or suspended) rule execution and the data needed to
 * inspect it. At most one session is active per user.
 */
@Getter
@RequiredArgsConstructor
public final class DebugSession {

    private static final long TERMINATE_JOIN_MILLIS = 2_000;

    private final ProjectIdModel projectId;
    private final String tableId;
    private final TraceDebugger debugger;
    private final @Nullable ClassLoader classLoader;

    /** Cancel the session, releasing the worker thread. */
    public void terminate() {
        debugger.terminate(TERMINATE_JOIN_MILLIS);
    }
}
