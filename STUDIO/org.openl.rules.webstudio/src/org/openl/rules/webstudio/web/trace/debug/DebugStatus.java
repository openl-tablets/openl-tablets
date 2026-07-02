package org.openl.rules.webstudio.web.trace.debug;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Lifecycle state of a debug session.
 *
 * <p>The normal flow is {@code pending -> running <-> suspended -> completed}. A session ends in
 * {@code error} when execution fails and in {@code terminated} when it is cancelled.
 *
 * <p>Each constant serializes to a stable lowercase code instead of the enum name, so the wire value
 * stays human-readable and is not a Java-style constant.
 */
@RequiredArgsConstructor
public enum DebugStatus {

    /** Created but the worker has not started yet. */
    @JsonProperty("pending")
    PENDING("pending"),

    /** Execution is running and not currently suspended. */
    @JsonProperty("running")
    RUNNING("running"),

    /** Execution is paused at a breakpoint or step point; the stack can be inspected. */
    @JsonProperty("suspended")
    SUSPENDED("suspended"),

    /** Execution finished normally. */
    @JsonProperty("completed")
    COMPLETED("completed"),

    /** Execution failed with an error. */
    @JsonProperty("error")
    ERROR("error"),

    /** Execution was cancelled before it finished. */
    @JsonProperty("terminated")
    TERMINATED("terminated");

    /** The stable wire code, sent over both REST and WebSocket. */
    @Getter
    private final String code;

    /** Whether this is a final state that accepts no further commands. */
    public boolean isTerminal() {
        return this == COMPLETED || this == ERROR || this == TERMINATED;
    }
}
