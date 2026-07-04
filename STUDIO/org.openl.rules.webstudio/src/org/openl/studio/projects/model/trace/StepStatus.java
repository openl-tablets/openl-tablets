package org.openl.studio.projects.model.trace;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Execution status of a frame's sub-step.
 *
 * <p>Each constant serializes to a stable code instead of the enum name, so the wire value stays stable
 * and human-readable.
 */
public enum StepStatus {

    /** The step has already run; an executed step can carry its frozen value and timings. */
    @JsonProperty("executed")
    EXECUTED,

    /** The step is running right now: the current line of the frame. */
    @JsonProperty("current")
    CURRENT,

    /** The step has not run yet and can be armed as a run-to target. */
    @JsonProperty("pending")
    PENDING
}
