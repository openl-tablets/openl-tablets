/**
 * 
 */
package org.openl.rules.tbasic.compile;

import java.util.ArrayList;
import java.util.List;

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
    private String[] nameForDebug;
    private List<ConversionRuleStep> convertionSteps;


    public List<ConversionRuleStep> getConvertionSteps() {
        if (convertionSteps == null) {
            synchronized (this) {
                if (convertionSteps == null) {
                    convertionSteps = extractConversionSteps();
                }
            }
        }

        return convertionSteps;
    }

    private List<ConversionRuleStep> extractConversionSteps() {
        ArrayList<ConversionRuleStep> steps = new ArrayList<ConversionRuleStep>(operationType.length);

        for (int i = 0; i < operationType.length; i++) {
            String theOperationType = operationType[i];
            String theOperationParam1 = operationParam1[i];
            String theOperationParam2 = operationParam2[i];
            String theLabelInstruction = label[i];
            String theNameForDebug = nameForDebug[i];

            ConversionRuleStep step = new ConversionRuleStep(theOperationType, theOperationParam1, theOperationParam2,
                    theLabelInstruction, theNameForDebug);
            
            steps.add(step);
        }

        return steps;
    }

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
    /**
     * @param nameForDebug the nameForDebug to set
     */
    public void setNameForDebug(String[] nameForDebug) {
        this.nameForDebug = nameForDebug;
    }

    /**
     * @return the nameForDebug
     */
    public String[] getNameForDebug() {
        return nameForDebug;
    }
}
