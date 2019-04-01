package org.openl.rules.tbasic;

import java.util.HashMap;
import java.util.Map;

import org.openl.meta.StringValue;
import org.openl.rules.table.IGridRegion;
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
    private IGridRegion gridRegion;

    // capacity by number of values
    private Map<String, IGridRegion> valueGridRegions = new HashMap<>(7);

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

    /**
     * @return the gridRegion
     */
    public IGridRegion getGridRegion() {
        return gridRegion;
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

    public IGridRegion getValueGridRegion(String valueName) {
        return valueGridRegions.get(valueName);
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

    /**
     * @param gridRegion the gridRegion to set
     */
    public void setGridRegion(IGridRegion gridRegion) {
        this.gridRegion = gridRegion;
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

    public void setValueGridRegion(String valueName, IGridRegion region) {
        valueGridRegions.put(valueName, region);
    }

    @Override
    public String toString() {
        String delimeter = " | ";
        StringBuilder buf = new StringBuilder();
        buf.append(label).append(delimeter);
        buf.append(description).append(delimeter);
        buf.append(operation).append(delimeter);
        buf.append(condition).append(delimeter);
        buf.append(action).append(delimeter);
        return buf.toString();
    }
}
