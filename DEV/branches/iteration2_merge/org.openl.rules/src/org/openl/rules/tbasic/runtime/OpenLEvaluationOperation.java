/**
 * 
 */
package org.openl.rules.tbasic.runtime;

/**
 * @author User
 *
 */
public abstract class OpenLEvaluationOperation<ResultValueType> extends RuntimeOperation {
    protected Object openLStatement;
    
    public OpenLEvaluationOperation(Object openLStatement){
        this.openLStatement = openLStatement;
    }
    
    public ResultValueType evaluateStatement(){
        ResultValueType resultValue = null;
        //resultValue = openLStatement.evaluate();
        return resultValue;
    }

}
