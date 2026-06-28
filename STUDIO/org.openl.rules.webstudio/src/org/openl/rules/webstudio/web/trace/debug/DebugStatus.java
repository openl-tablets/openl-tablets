package org.openl.rules.webstudio.web.trace.debug;

/**
 * Lifecycle state of a debug session.
 *
 * <p>The normal flow is {@code PENDING -> RUNNING <-> SUSPENDED -> COMPLETED}. A session ends in
 * {@code ERROR} when execution fails and in {@code TERMINATED} when it is cancelled.
 */
public enum DebugStatus {

    /** Created but the worker has not started yet. */
    PENDING,

    /** Execution is running and not currently suspended. */
    RUNNING,

    /** Execution is paused at a breakpoint or step point; the stack can be inspected. */
    SUSPENDED,

    /** Execution finished normally. */
    COMPLETED,

    /** Execution failed with an error. */
    ERROR,

    /** Execution was cancelled before it finished. */
    TERMINATED;

    /** Whether this is a final state that accepts no further commands. */
    public boolean isTerminal() {
        return this == COMPLETED || this == ERROR || this == TERMINATED;
    }
}
