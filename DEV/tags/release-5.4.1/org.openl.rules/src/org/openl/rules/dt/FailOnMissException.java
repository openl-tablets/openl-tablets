package org.openl.rules.dt;


public class FailOnMissException extends RuntimeException {
    
    private static final long serialVersionUID = -4344185808917149412L;
    
    private DecisionTable decisionTable; 
    private Object[] invocationParameters;
    
    public FailOnMissException() {
    }
    
    public FailOnMissException(String message, DecisionTable theDecisionTable, Object[] theInvocationParameters) {
        super(message);
        decisionTable = theDecisionTable;
        invocationParameters = theInvocationParameters.clone();
        
    }

    public DecisionTable getDecisionTable() {
        return decisionTable;
    }

    public Object[] getInvocationParameters() {
        return invocationParameters;
    }

}
