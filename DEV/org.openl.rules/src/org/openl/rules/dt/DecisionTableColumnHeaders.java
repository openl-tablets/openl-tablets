package org.openl.rules.dt;

public enum DecisionTableColumnHeaders {
    CONDITION("C"),
    HORIZONTAL_CONDITION("HC"),
    MERGED_CONDITION("MC"),
    ACTION("A"),
    RULE("RULE"),
    RETURN("RET"),
    COLLECT_RETURN("CRET"),
    KEY("KEY"),;

    private String key;

    DecisionTableColumnHeaders(String key) {
        this.key = key;
    }

    public String getHeaderKey() {
        return key;
    }

}
