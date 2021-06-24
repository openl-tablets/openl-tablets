package org.openl.rules.rest;

import org.openl.message.Severity;

public class OpenlProblemMessage {

    private final long id;
    private final String summary;
    private final boolean hasStacktrace;
    private final String[] errorCode;
    private final boolean hasLinkToCell;
    private final String tableId;
    private final String errorCell;
    private final String errorUri;
    private final Severity severity;

    public OpenlProblemMessage(long id,
            String summary,
            boolean hasStacktrace,
            String[] errorCode,
            boolean hasLinkToCell,
            String tableId,
            String errorCell,
            String errorUri,
            Severity severity) {
        this.id = id;
        this.summary = summary;
        this.hasStacktrace = hasStacktrace;
        this.errorCode = errorCode;
        this.hasLinkToCell = hasLinkToCell;
        this.tableId = tableId;
        this.errorCell = errorCell;
        this.errorUri = errorUri;
        this.severity = severity;
    }

    public long getId() {
        return id;
    }

    public String getSummary() {
        return summary;
    }

    public boolean isHasStacktrace() {
        return hasStacktrace;
    }

    public String[] getErrorCode() {
        return errorCode;
    }

    public boolean isHasLinkToCell() {
        return hasLinkToCell;
    }

    public String getTableId() {
        return tableId;
    }

    public String getErrorCell() {
        return errorCell;
    }

    public String getErrorUri() {
        return errorUri;
    }

    public Severity getSeverity() {
        return severity;
    }
}
