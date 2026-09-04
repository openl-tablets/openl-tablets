package org.openl.rules.dt;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum DecisionTableColumnHeaders {
    CONDITION("C"),
    HORIZONTAL_CONDITION("HC"),
    MERGED_CONDITION("MC"),
    ACTION("A"),
    RULE("RULE"),
    RETURN("RET"),
    COLLECT_RETURN("CRET"),
    KEY("KEY"),
    ;

    private final String key;

    public String getHeaderKey() {
        return key;
    }

}
