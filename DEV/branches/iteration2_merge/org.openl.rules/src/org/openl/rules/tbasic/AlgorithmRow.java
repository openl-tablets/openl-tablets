package org.openl.rules.tbasic;

import java.util.HashMap;
import java.util.Map;

import org.openl.meta.StringValue;
import org.openl.rules.table.IGridRegion;

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
    private Map<String, IGridRegion> valueGridRegions = new HashMap<String, IGridRegion>(7);  

    public StringValue getLabel() {
        if (label == null){
            label = new StringValue("");
        }
        return label;
    }

    public StringValue getDescription() {
        if (description == null){
            description = new StringValue("");
        }
        return description;
    }

    public StringValue getOperation() {
        if (description == null){
            description = new StringValue("");
        }
        return operation;
    }

    public StringValue getCondition() {
        if (condition == null){
            condition = new StringValue("");
        }
        return condition;
    }

    public StringValue getAction() {
        if (action == null){
            action = new StringValue("");
        }
        return action;
    }

    public StringValue getBefore() {
        if (before == null){
            before = new StringValue("");
        }
        return before;
    }

    public StringValue getAfter() {
        if (after == null){
            after = new StringValue("");
        }
        return after;
    }

    public int getOperationLevel() {
        return operationLevel;
    }

    public void setOperationLevel(int operationLevel) {
        this.operationLevel = operationLevel;
    }

    public void setLabel(StringValue label) {
        this.label = label;
    }

    public void setDescription(StringValue description) {
        this.description = description;
    }

    public void setOperation(StringValue operation) {
        this.operation = operation;
    }

    public void setCondition(StringValue condition) {
        this.condition = condition;
    }

    public void setAction(StringValue action) {
        this.action = action;
    }

    public void setBefore(StringValue before) {
        this.before = before;
    }

    public void setAfter(StringValue after) {
        this.after = after;
    }

    /**
     * @param rowNumber the rowNumber to set
     */
    public void setRowNumber(int rowNumber) {
        this.rowNumber = rowNumber;
    }

    /**
     * @return the rowNumber
     */
    public int getRowNumber() {
        return rowNumber;
    }

    /**
     * @param gridRegion the gridRegion to set
     */
    public void setGridRegion(IGridRegion gridRegion) {
        this.gridRegion = gridRegion;
    }

    /**
     * @return the gridRegion
     */
    public IGridRegion getGridRegion() {
        return gridRegion;
    }

    public void setValueGridRegion(String valueName, IGridRegion region) {
        valueGridRegions.put(valueName, region);
    }
    
    public IGridRegion getValueGridRegion(String valueName) {
        return valueGridRegions.get(valueName);
    }
}
