package org.openl.rules.tbasic;

import org.openl.meta.StringValue;
import org.openl.util.StringUtils;

public class AlgorithmRow {
    private StringValue label;
    private StringValue description;
    private StringValue operation;
    private StringValue condition;
    private StringValue action;
    private StringValue before;
    private StringValue after;
    private int operationLevel;
    private int rowNumber;

    public StringValue getAction() {
        if (action == null) {
            action = new StringValue(StringUtils.EMPTY);
        }
        return action;
    }

    public StringValue getAfter() {
        if (after == null) {
            after = new StringValue(StringUtils.EMPTY);
        }
        return after;
    }

    public StringValue getBefore() {
        if (before == null) {
            before = new StringValue(StringUtils.EMPTY);
        }
        return before;
    }

    public StringValue getCondition() {
        if (condition == null) {
            condition = new StringValue(StringUtils.EMPTY);
        }
        return condition;
    }

    public StringValue getDescription() {
        if (description == null) {
            description = new StringValue(StringUtils.EMPTY);
        }
        return description;
    }

    public StringValue getLabel() {
        if (label == null) {
            label = new StringValue(StringUtils.EMPTY);
        }
        return label;
    }

    public StringValue getOperation() {
        if (description == null) {
            description = new StringValue(StringUtils.EMPTY);
        }
        return operation;
    }

    public int getOperationLevel() {
        return operationLevel;
    }

    /**
     * @return the rowNumber
     */
    public int getRowNumber() {
        return rowNumber;
    }

    public void setAction(StringValue action) {
        this.action = action;
    }

    public void setAfter(StringValue after) {
        this.after = after;
    }

    public void setBefore(StringValue before) {
        this.before = before;
    }

    public void setCondition(StringValue condition) {
        this.condition = condition;
    }

    public void setDescription(StringValue description) {
        this.description = description;
    }

    public void setLabel(StringValue label) {
        this.label = label;
    }

    public void setOperation(StringValue operation) {
        this.operation = operation;
    }

    public void setOperationLevel(int operationLevel) {
        this.operationLevel = operationLevel;
    }

    /**
     * @param rowNumber the rowNumber to set
     */
    public void setRowNumber(int rowNumber) {
        this.rowNumber = rowNumber;
    }

    @Override
    public String toString() {
        var delimeter = " | ";
        return label + delimeter + description + delimeter + operation + delimeter + condition + delimeter + action
                + delimeter;
    }
}
