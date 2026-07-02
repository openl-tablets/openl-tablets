package org.openl.studio.projects.model.trace;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.openl.rules.webstudio.web.trace.debug.DebugCommand;

/**
 * A stepping action requested on the trace {@code /step} endpoint.
 *
 * <p>Each constant serializes to (and is accepted on the wire as) a stable code, and maps to the engine
 * {@link DebugCommand} it performs. Resuming is a separate, asynchronous endpoint, so {@code RESUME} is
 * intentionally not a step type.
 */
public enum StepType {

    @JsonProperty("into")
    INTO(DebugCommand.STEP_INTO),

    @JsonProperty("over")
    OVER(DebugCommand.STEP_OVER),

    @JsonProperty("out")
    OUT(DebugCommand.STEP_OUT);

    private final DebugCommand command;

    StepType(DebugCommand command) {
        this.command = command;
    }

    /** The engine command this step performs. */
    public DebugCommand toCommand() {
        return command;
    }
}
