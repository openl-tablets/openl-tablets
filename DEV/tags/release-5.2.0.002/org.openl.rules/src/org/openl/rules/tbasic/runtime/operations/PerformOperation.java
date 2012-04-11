package org.openl.rules.tbasic.runtime.operations;

import org.openl.rules.tbasic.runtime.Result;
import org.openl.rules.tbasic.runtime.ReturnType;
import org.openl.rules.tbasic.runtime.TBasicContextHolderEnv;
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
