/**
 *  Class to support Excel NORMSDIST * 
 */
package com.exigen.le.evaluator.function.addins;

import org.apache.poi.hssf.record.formula.eval.BoolEval;
import org.apache.poi.hssf.record.formula.eval.ErrorEval;
import org.apache.poi.hssf.record.formula.eval.NumberEval;
import org.apache.poi.hssf.record.formula.eval.RefEval;
import org.apache.poi.hssf.record.formula.eval.ValueEval;
import org.apache.poi.hssf.record.formula.functions.FreeRefFunction;
import org.apache.poi.ss.formula.OperationEvaluationContext;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistributionImpl;


/**
 * NormsDist Executor
 * @author vabramovs
 *
 */
public class NormsDist  implements FreeRefFunction{

	public ValueEval evaluate(ValueEval[] args, OperationEvaluationContext ec) {
        for (int i = 0; i < args.length; i++) {
    		// Resolve all reference before calculating
	    	if(args[i] instanceof RefEval){
	    		args[i]= ((RefEval)args[i]).getInnerValueEval();
	    	}
        }
        
        NormalDistributionImpl nDistr = new NormalDistributionImpl(0,1);
       
	try {
		return new NumberEval(nDistr.cumulativeProbability(((NumberEval)args[0]).getNumberValue()));
	} catch (MathException e) {
		return  ErrorEval.VALUE_INVALID;
	}
  }

}
