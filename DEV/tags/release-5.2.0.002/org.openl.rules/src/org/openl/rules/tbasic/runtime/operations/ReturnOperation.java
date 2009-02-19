/**
 * 
 */
package org.openl.rules.tbasic.runtime.operations;

import org.openl.rules.tbasic.runtime.Result;
import org.openl.rules.tbasic.runtime.ReturnType;
import org.openl.rules.tbasic.runtime.TBasicContextHolderEnv;
import org.openl.types.IMethodCaller;

/**
 * @author User
 *
 */
public class ReturnOperation<ResultValueType> extends OpenLEvaluationOperation<ResultValueType> {
    private boolean hasReturnValue;
    
    public ReturnOperation(IMethodCaller openLStatement){
        super(openLStatement);
        hasReturnValue = openLStatement != null;
    }

    /* (non-Javadoc)
     * @see org.openl.rules.tbasic.runtime.RuntimeOperation#execute(org.openl.rules.tbasic.runtime.TBasicContext, java.lang.Object[])
     */
    @Override
    public Result execute(TBasicContextHolderEnv environment, Object param) {
        ResultValueType resultValue = null;
        
        if (hasReturnValue){
            resultValue = evaluateStatement(environment);
        }
        
        return new Result(ReturnType.Return, resultValue);
    }

}
