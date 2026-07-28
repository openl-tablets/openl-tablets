package org.openl.studio.projects.model.trace;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Kind of a decision-table row shown under a table in the trace: an evaluated condition (matched or not),
 * or the rule the table returned.
 *
 * <p>Present only on the sub-steps of a decision-table node, so a client can draw the legacy breakdown —
 * green check for a matched condition, red cross for an unmatched one, and the returned rule. Absent on
 * every other kind of step. Each constant serializes to a stable code instead of the enum name.
 */
public enum DecisionRow {

    /** An evaluated condition that matched for its rules. */
    @JsonProperty("matched")
    MATCHED,

    /** An evaluated condition that did not match for its rules. */
    @JsonProperty("unmatched")
    UNMATCHED,

    /** The rule the table returned. */
    @JsonProperty("returned")
    RETURNED
}
