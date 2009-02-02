package org.openl.rules.tbasic;

import org.openl.meta.StringValue;

public class AlgorithmRow {
    private StringValue label;
    private StringValue description;
    private StringValue operation;
    private StringValue condition;
    private StringValue action;
    private StringValue before;
    private StringValue after;
    private int operationLevel;

    public void set(String column, StringValue sv) {
        ;
    }
    
    public StringValue getLabel() {
        return label;
    }

    public StringValue getDescription() {
        return description;
    }

    public StringValue getOperation() {
        return operation;
    }

    public StringValue getCondition() {
        return condition;
    }

    public StringValue getAction() {
        return action;
    }

    public StringValue getBefore() {
        return before;
    }

    public StringValue getAfter() {
        return after;
    }

    public int getOperationLevel() {
        return operationLevel;
    }

    public void setOperationLevel(int operationLevel) {
        this.operationLevel = operationLevel;
    }
}
