package org.openl.studio.projects.service.trace;

import org.openl.vm.Tracer;

/**
 * Re-runs a session's traced execution with a supplied tracer.
 *
 * <p>The interactive debugger keeps only the live stack, not the whole executed tree. Exporting the full
 * trace therefore replays the same rule and test case, observed by a fresh tracer that records every node.
 * The replay is deterministic: rules are side-effect free, so it reproduces the run the user started.
 */
@FunctionalInterface
interface TraceReplay {

    /** Run the traced rule once, attaching {@code tracer} so its execution is observed. */
    void run(Tracer tracer);
}
