/**
 * 
 */
package com.exigen.le.smodel.emulator;

import org.apache.poi.hssf.record.formula.eval.StringEval;
import org.apache.poi.hssf.record.formula.eval.ValueEval;
import org.apache.poi.hssf.record.formula.functions.FreeRefFunction;
import org.apache.poi.ss.formula.OperationEvaluationContext;

import com.exigen.le.LE_Value;

/**
 * @author vabramovs
 *
 */
public class TableEmulator  {

	/* (non-Javadoc)
	 * @see org.apache.poi.hssf.record.formula.functions.FreeRefFunction#evaluate(org.apache.poi.hssf.record.formula.eval.ValueEval[], org.apache.poi.ss.formula.OperationEvaluationContext)
	 */

	public static ValueEval evaluate(ValueEval[] args, OperationEvaluationContext ec) {
		String result="";
		for(int i=0;i<args.length;i++){
			result=result+LE_Value.fromValueEval(args[i]).getValue();
		}
		return new StringEval(result);
	}
	public static String calculate(Object[] args) {
		String result="";
		for(int i=0;i<args.length;i++){
			result=result+args[i];
		}
		return result;
	}

}
