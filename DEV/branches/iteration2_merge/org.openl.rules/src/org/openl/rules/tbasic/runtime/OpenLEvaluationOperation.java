/**
 * 
 */
package org.openl.rules.tbasic.runtime;

import org.openl.types.IMethodCaller;

/**
 * @author User
 *
 */
public abstract class OpenLEvaluationOperation<ResultValueType> extends RuntimeOperation {
    protected IMethodCaller openLStatement;
    
    public OpenLEvaluationOperation(IMethodCaller openLStatement){
        this.openLStatement = openLStatement;
    }
    
    public ResultValueType evaluateStatement(TBasicContext context){
        ResultValueType resultValue = null;
        resultValue = (ResultValueType)openLStatement.invoke(context.getThisTarget(), context.getOpenLParams(), context.getOpenLEnvironment());
        return resultValue;
    }

}
