package org.openl.rules.rest;

public class OpenlProblemMessage {

    private final long id;
    private final String summary;
    private final boolean hasStacktrace;
    private final String[] errorCode;
    private final boolean hasLinkToCell;
    private final String tableId;
    private final String errorCell;
    private final String errorUri;

    public OpenlProblemMessage(long id, String summary,
            boolean hasStacktrace,
            String[] errorCode,
            boolean hasLinkToCell,
            String tableId,
            String errorCell,
            String errorUri) {
        this.id = id;
        this.summary = summary;
        this.hasStacktrace = hasStacktrace;
        this.errorCode = errorCode;
        this.hasLinkToCell = hasLinkToCell;
        this.tableId = tableId;
        this.errorCell = errorCell;
        this.errorUri = errorUri;
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
}
