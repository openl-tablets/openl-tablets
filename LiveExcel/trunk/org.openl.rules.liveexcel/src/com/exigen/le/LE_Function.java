/**
 * 
 */
package com.exigen.le;

import org.apache.poi.hssf.record.formula.eval.ValueEval;
import org.apache.poi.ss.formula.OperationEvaluationContext;

/**
 * @author vabramovs
 *
 */
public interface LE_Function {
	
	/**
	 * Evaluate function value
	 * @param args
	 * @param fec
	 * @return
	 */
	LE_Value evaluate(LE_Value[] args, FunctionEvaluationContext fec);


}
