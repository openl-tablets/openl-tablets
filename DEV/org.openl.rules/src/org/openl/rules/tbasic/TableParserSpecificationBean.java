package org.openl.rules.tbasic;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
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
