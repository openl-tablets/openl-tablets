/**
 * 
 */
package org.openl.rules.tbasic.compile;

/**
 * @author User
 *
 */
public class ConversionRuleBean {
    private String operation;
    private boolean multiLine;
    private String[] label;
    private String[] operationType;
    private String[] operationParam1;
    private String[] operationParam2;
    
    /**
     * @return the operation
     */
    public String getOperation() {
        return operation;
    }
    /**
     * @param operation the operation to set
     */
    public void setOperation(String operation) {
        this.operation = operation;
    }
    /**
     * @return the multiLine
     */
    public boolean isMultiLine() {
        return multiLine;
    }
    /**
     * @param multiLine the multiLine to set
     */
    public void setMultiLine(boolean multiLine) {
        this.multiLine = multiLine;
    }
    /**
     * @return the label
     */
    public String[] getLabel() {
        return label;
    }
    /**
     * @param label the label to set
     */
    public void setLabel(String[] label) {
        this.label = label;
    }
    /**
     * @return the operationType
     */
    public String[] getOperationType() {
        return operationType;
    }
    /**
     * @param operationType the operationType to set
     */
    public void setOperationType(String[] operationType) {
        this.operationType = operationType;
    }
    /**
     * @return the operationParam1
     */
    public String[] getOperationParam1() {
        return operationParam1;
    }
    /**
     * @param operationParam1 the operationParam1 to set
     */
    public void setOperationParam1(String[] operationParam1) {
        this.operationParam1 = operationParam1;
    }
    /**
     * @return the operationParam2
     */
    public String[] getOperationParam2() {
        return operationParam2;
    }
    /**
     * @param operationParam2 the operationParam2 to set
     */
    public void setOperationParam2(String[] operationParam2) {
        this.operationParam2 = operationParam2;
    }

}
