/**
 *  Class to support VBA function OFFIRST
 */
package com.exigen.le.evaluator.function.addins;

import org.apache.poi.hssf.record.formula.eval.ErrorEval;
import org.apache.poi.hssf.record.formula.eval.NumberEval;
import org.apache.poi.hssf.record.formula.eval.RefEval;
import org.apache.poi.hssf.record.formula.eval.ValueEval;
import org.apache.poi.hssf.record.formula.functions.FreeRefFunction;
import org.apache.poi.ss.formula.ArrayEval;
import org.apache.poi.ss.formula.OperationEvaluationContext;

/**
 * OFFirst executor
 * @author vabramovs
 *
 */
public class OfFirst implements FreeRefFunction {

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
        int nFirst = (int)((NumberEval)args[0]).getNumberValue();
        ValueEval[][] values = new ValueEval[nFirst][1];
        for(int i=0;i<nFirst;i++){
        	values[i][0]=new NumberEval(i+1);
        }
       ArrayEval ret = new ArrayEval(values);
       return ret;
 	}

}
