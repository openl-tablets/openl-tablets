package org.openl.rules.tbasic.runtime;

import org.openl.types.IMethodCaller;


public class PerformOperation extends OpenLEvaluationOperation<Object> {

    public PerformOperation(IMethodCaller openLStatement){
        super(openLStatement);
    }
    
    @Override
    public Result execute(TBasicContextHolderEnv environment, Object param) {
        evaluateStatement(environment);
        return new Result(ReturnType.Next);
    }

}
