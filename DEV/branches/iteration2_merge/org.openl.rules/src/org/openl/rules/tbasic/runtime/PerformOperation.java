package org.openl.rules.tbasic.runtime;


public class PerformOperation extends OpenLEvaluationOperation<Object> {

    public PerformOperation(Object openLStatement){
        super(openLStatement);
    }
    
    @Override
    public Result execute(TBasicContext context, Object param) {
        evaluateStatement();
        return new Result(ReturnType.Next);
    }

}
