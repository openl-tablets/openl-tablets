/**
 * 
 */
package com.exigen.le;

/**
 * @author vabramovs
 *
 */
public class FunctionEvaluationContext {
	
	String functionName;    
	 // EvaluationContext POI
	// 	We use type Object to avoid necessity to import poi packages for whom it not interesting
	// 	Otherwise need to be cast to OperationEvaluationContext before using
	// TODO If we  do not create "thin" interface package Object need to replace to OperationEvaluationContext 
	Object operationEvaluationContext;   
 
	/**
	 * @param functionName
	 * @param operationEvaluationContext
	 */
	public FunctionEvaluationContext(String functionName,
			Object operationEvaluationContext) {
		this.functionName = functionName;
		this.operationEvaluationContext = operationEvaluationContext;
	}

	/**
	 * @return the functionName
	 */
	public String getFunctionName() {
		return functionName;
	}

	/**
	 * @return the operationEvaluationContext
	 */
	public Object getOperationEvaluationContext() {
		return operationEvaluationContext;
	}

}
