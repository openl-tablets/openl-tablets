package org.openl.rules.webstudio.web.trace.debug;

/**
 * The kind of rule table that a debug stack frame represents.
 *
 * <p>Each constant carries a stable {@code code} used by the UI for icons and styling.
 */
public enum FrameKind {

    DECISION_TABLE("decisionTable"),
    SPREADSHEET("spreadsheet"),
    METHOD("method"),
    COLUMN_MATCH("cmatch"),
    TBASIC("tbasic"),
    TBASIC_METHOD("tbasicMethod");

    private final String code;

    FrameKind(String code) {
        this.code = code;
    }

    /** Stable identifier shared with the UI. */
    public String getCode() {
        return code;
    }
}
