package org.openl.rules.webstudio.web.trace.debug;

/**
 * A command that resumes a suspended worker.
 *
 * <p>Stepping commands arm a depth threshold that decides where execution suspends next. {@code PAUSE}
 * is not part of this set: it is an asynchronous request handled while execution is running.
 */
public enum DebugCommand {

    /** Suspend at the next step in the current frame or the entry of any callee table. */
    STEP_INTO,

    /** Run callees to completion; suspend at the next step in the current frame or its caller. */
    STEP_OVER,

    /** Run the current frame to completion; suspend at its own return point so its result is visible. */
    STEP_OUT,

    /**
     * Run the current frame to completion and ascend straight to the caller, suspending at the caller's
     * next step. Unlike {@link #STEP_OUT} it does not stop at this frame's own exit, so a deep frame with
     * many sub-steps (for example a large non-indexed decision table) can be left in one action.
     */
    STEP_TO_CALLER,

    /** Run until the next breakpoint or until execution finishes. */
    RESUME
}
