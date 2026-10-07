package org.openl.studio.projects.model.trace;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * How much per-frame detail a stack response carries.
 *
 * <p>Each constant serializes to a stable lowercase code instead of the enum name, so the wire value
 * stays human-readable.
 */
public enum StackViewMode {

    /** Every frame carries its sub-steps, so the whole call tree renders at once (the UI default). */
    @JsonProperty("full")
    FULL,

    /** Only the active (top) frame carries its sub-steps; the other frames are the bare stack. */
    @JsonProperty("compact")
    COMPACT
}
