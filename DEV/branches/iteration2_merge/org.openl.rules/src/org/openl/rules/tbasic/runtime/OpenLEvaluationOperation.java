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
    
    public ResultValueType evaluateStatement(TBasicContextHolderEnv environment){
        ResultValueType resultValue = null;
        resultValue = (ResultValueType)openLStatement.invoke(environment.getTbasicTarget(), environment.getTbasicParams(), environment);
        return resultValue;
    }

}
