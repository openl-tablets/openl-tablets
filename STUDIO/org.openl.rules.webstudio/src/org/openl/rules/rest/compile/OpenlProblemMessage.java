package org.openl.rules.rest.compile;

import lombok.RequiredArgsConstructor;

import org.openl.message.Severity;

@RequiredArgsConstructor
public class OpenlProblemMessage {

    private final long id;
    private final String summary;
    private final boolean hasStacktrace;
    private final String[] errorCode;
    private final boolean hasLinkToCell;
    private final String tableId;
    private final String errorCell;
    private final Severity severity;

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

    public Severity getSeverity() {
        return severity;
    }
}
