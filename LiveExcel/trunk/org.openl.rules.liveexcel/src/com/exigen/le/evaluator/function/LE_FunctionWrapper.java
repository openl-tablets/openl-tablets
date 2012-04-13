/**
 * 
 */
package com.exigen.le.evaluator.function;

import org.apache.poi.hssf.record.formula.eval.RefEval;
import org.apache.poi.hssf.record.formula.eval.ValueEval;
import org.apache.poi.hssf.record.formula.functions.FreeRefFunction;
import org.apache.poi.ss.formula.OperationEvaluationContext;

import com.exigen.le.FunctionEvaluationContext;
import com.exigen.le.LE_Function;
import com.exigen.le.LE_Value;

/**
 * Provide invoking of external function executor if registered 
 * @author vabramovs
 *
 */
public class LE_FunctionWrapper implements FreeRefFunction {
	
	LE_Function externalExecutor;
	String functionName;
	

	/**
	 * @param externalExecutor
	 * @param functionName
	 */
	public LE_FunctionWrapper(LE_Function externalExecutor, String functionName) {
		this.externalExecutor = externalExecutor;
		this.functionName = functionName;
	}


	/* (non-Javadoc)
	 * @see org.apache.poi.hssf.record.formula.functions.FreeRefFunction#evaluate(org.apache.poi.hssf.record.formula.eval.ValueEval[], org.apache.poi.ss.formula.OperationEvaluationContext)
	 */
	public ValueEval evaluate(ValueEval[] args, OperationEvaluationContext ec) {
		// Resolve all args
		LE_Value[] params = new LE_Value[args.length];
		for(int i=0;i<args.length;i++){
	    	if(args[i] instanceof RefEval){
		    	args[i]= ((RefEval)args[i]).getInnerValueEval();
	    	}
	    	params[i] = LE_Value.fromValueEval(args[i]);
		}
		// Build Evaluation context
		FunctionEvaluationContext fec = new FunctionEvaluationContext(functionName,ec);
		// Invoke external executor and convert result
		LE_Value value =  externalExecutor.evaluate(params, fec);
		return LE_Value.createValueEval(value);
		
	}

}
