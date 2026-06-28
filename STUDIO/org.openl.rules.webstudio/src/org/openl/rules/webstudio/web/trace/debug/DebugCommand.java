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

    /** Run until the current frame returns; suspend in the caller. */
    STEP_OUT,

    /** Run until the next breakpoint or until execution finishes. */
    RESUME
}
