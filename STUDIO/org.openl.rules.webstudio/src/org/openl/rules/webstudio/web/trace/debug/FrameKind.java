package org.openl.rules.webstudio.web.trace.debug;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The kind of rule table that a debug stack frame represents.
 *
 * <p>Each constant serializes to a stable code (used by the UI for icons and styling) instead of the
 * enum name, so the wire value stays stable and human-readable.
 */
public enum FrameKind {

    @JsonProperty("decisionTable")
    DECISION_TABLE,

    @JsonProperty("spreadsheet")
    SPREADSHEET,

    @JsonProperty("method")
    METHOD,

    @JsonProperty("cmatch")
    COLUMN_MATCH,

    @JsonProperty("tbasic")
    TBASIC,

    @JsonProperty("tbasicMethod")
    TBASIC_METHOD,

    /** Not a table: a reference to a step that already executed elsewhere in the same frame. */
    @JsonProperty("stepRef")
    STEP_REF
}
