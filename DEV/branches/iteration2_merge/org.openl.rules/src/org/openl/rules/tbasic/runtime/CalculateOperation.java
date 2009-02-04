/**
 * 
 */
package org.openl.rules.tbasic.runtime;

import org.openl.types.IMethodCaller;

/**
 * @author User
 *
 */
public class CalculateOperation<ResultValueType> extends OpenLEvaluationOperation<ResultValueType> {
   
    public CalculateOperation(IMethodCaller openLStatement) {
        super(openLStatement);
    }
    
    /* (non-Javadoc)
     * @see org.openl.rules.tbasic.runtime.RuntimeOperation#execute(org.openl.rules.tbasic.runtime.TBasicContext, java.lang.Object[])
     */
    @Override
    public Result execute(TBasicContext context, Object param) {
        // TODO Auto-generated method stub
        ResultValueType resultValue = evaluateStatement(context);
        return new Result(ReturnType.Next, resultValue);
    }

}
