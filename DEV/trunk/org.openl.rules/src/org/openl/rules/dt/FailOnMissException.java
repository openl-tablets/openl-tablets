package org.openl.rules.dt;


public class FailOnMissException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    
    DecisionTable decisionTable; 
    Object[] params;
    
    public FailOnMissException(String msg, DecisionTable decisionTable, Object[] params) {
        super(msg);
        this.decisionTable = decisionTable;
        this.params = params.clone();
        
    }

}
