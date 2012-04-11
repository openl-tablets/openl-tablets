/**
 * 
 */
package org.openl.rules.tbasic.runtime.operations;

import org.openl.rules.tbasic.runtime.TBasicContextHolderEnv;
import org.openl.types.IMethodCaller;

/**
 * @author User
 *
 */
public abstract class OpenLEvaluationOperation<ResultValueType> extends RuntimeOperation {
    private IMethodCaller openLStatement;
    
    public OpenLEvaluationOperation(IMethodCaller openLStatement){
        this.openLStatement = openLStatement;
    }
    
    public ResultValueType evaluateStatement(TBasicContextHolderEnv environment){
        ResultValueType resultValue = null;
        
        if (openLStatement != null){
            resultValue = (ResultValueType)openLStatement.invoke(environment.getTbasicTarget(), environment.getTbasicParams(), environment);
        }
        
        return resultValue;
    }

}
