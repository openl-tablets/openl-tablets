package org.openl.rules.dt.algorithm;

import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.dt.DecisionTable;

public class FailOnMissException extends OpenLRuntimeException {
    
    private static final long serialVersionUID = -4344185808917149412L;
    
    private DecisionTable decisionTable; 
    private Object[] invocationParameters;
    
    public FailOnMissException(String message, DecisionTable decisionTable, Object[] invocationParameters) {
        super(message);
        
        this.decisionTable = decisionTable;
        this.invocationParameters = invocationParameters.clone();
    }

    public DecisionTable getDecisionTable() {
        return decisionTable;
    }

    public Object[] getInvocationParameters() {
        return invocationParameters;
    }

}
