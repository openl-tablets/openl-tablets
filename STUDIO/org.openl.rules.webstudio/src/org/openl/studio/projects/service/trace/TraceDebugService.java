package org.openl.studio.projects.service.trace;

/**
 * Starts interactive debug sessions for rule tables.
 *
 * <p>Once started, a session is driven through its {@link org.openl.rules.webstudio.web.trace.debug.TraceDebugger}
 * (step, resume, pause, terminate) and inspected through the REST controller.
 */
public interface TraceDebugService {

    /** Build the test suite, spawn the worker, and start running to the first suspend point. */
    DebugSession startSession(TraceDebugStartRequest request);
}
