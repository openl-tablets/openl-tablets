/**
 * 
 */
package org.openl.rules.tbasic.runtime;

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
    public Result execute(TBasicContext context, Object param) {
        ResultValueType resultValue = null;
        
        if (hasReturnValue){
            resultValue = evaluateStatement(context);
        }
        
        return new Result(ReturnType.Return, resultValue);
    }

}
