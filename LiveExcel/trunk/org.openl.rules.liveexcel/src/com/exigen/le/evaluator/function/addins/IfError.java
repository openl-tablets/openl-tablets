/**
 *  Class to support Excel 2007 IFERROR
 */
package com.exigen.le.evaluator.function.addins;

import org.apache.poi.hssf.record.formula.eval.ErrorEval;
import org.apache.poi.hssf.record.formula.eval.RefEval;
import org.apache.poi.hssf.record.formula.eval.ValueEval;
import org.apache.poi.hssf.record.formula.functions.FreeRefFunction;
import org.apache.poi.ss.formula.OperationEvaluationContext;

/**
 * IFError executor
 * @author vabramovs
 *
 */
public class IfError implements FreeRefFunction {

	/* (non-Javadoc)
	 * @see org.apache.poi.hssf.record.formula.functions.FreeRefFunction#evaluate(org.apache.poi.hssf.record.formula.eval.ValueEval[], org.apache.poi.ss.formula.OperationEvaluationContext)
	 */
	public ValueEval evaluate(ValueEval[] args, OperationEvaluationContext ec) {
		
        for (int i = 0; i < args.length; i++) {
    		// Resolve all reference before calculating
	    	if(args[i] instanceof RefEval){
	    		args[i]= ((RefEval)args[i]).getInnerValueEval();
	    	}
        }	
		if(args[0] instanceof ErrorEval){
			return args[1];
		}
		else{
			return args[0];
		}
	}

}
