package org.openl.rules.dt;


public class FailOnMissException extends RuntimeException {
    
    private static final long serialVersionUID = -4344185808917149412L;
    
    private DecisionTable decisionTable; 
    private Object[] invokationParameters;
    
    public FailOnMissException() {
    }
    
    public FailOnMissException(String message, DecisionTable theDecisionTable, Object[] theInvokationParameters) {
        super(message);
        decisionTable = theDecisionTable;
        invokationParameters = theInvokationParameters.clone();
        
    }

    public DecisionTable getDecisionTable() {
        return decisionTable;
    }

    public Object[] getInvokationParameters() {
        return invokationParameters;
    }

}
