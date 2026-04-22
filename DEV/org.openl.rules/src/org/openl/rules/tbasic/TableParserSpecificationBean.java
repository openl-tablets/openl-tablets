package org.openl.rules.tbasic;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TableParserSpecificationBean {
    public enum ValueNecessity {
        REQUIRED,
        OPTIONAL,
        PROHIBITED
    }

    private String keyword;
    private boolean multiline;
    private ValueNecessity condition;
    private ValueNecessity action;
    private ValueNecessity label;
    private ValueNecessity beforeAndAfter;
    private ValueNecessity topLevel;
    private boolean loopOperation;

    private String[] predecessorOperations;

}
