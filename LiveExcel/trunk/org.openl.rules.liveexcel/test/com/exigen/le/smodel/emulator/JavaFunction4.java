/**
 * 
 */
package com.exigen.le.smodel.emulator;

import org.apache.poi.hssf.record.formula.eval.StringEval;
import org.apache.poi.hssf.record.formula.eval.ValueEval;
import org.apache.poi.hssf.record.formula.functions.FreeRefFunction;
import org.apache.poi.ss.formula.OperationEvaluationContext;

import com.exigen.le.FunctionEvaluationContext;
import com.exigen.le.LE_Function;
import com.exigen.le.LE_Value;

/**
 * @author vabramovs
 *
 */
public class JavaFunction4 implements LE_Function {

	/* (non-Javadoc)
	 * @see org.apache.poi.hssf.record.formula.functions.FreeRefFunction#evaluate(org.apache.poi.hssf.record.formula.eval.ValueEval[], org.apache.poi.ss.formula.OperationEvaluationContext)
	 */

	public LE_Value evaluate(LE_Value[] args, FunctionEvaluationContext fec) {
		LE_Value value = new LE_Value();
		value.setValue("Java 4 greets Excel");
		return value;
	}

}
