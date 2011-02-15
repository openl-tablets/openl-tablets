/**
 *  Class to support Excel NORMDIST * 
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
 * NormDist Executor
 * @author vabramovs
 *
 */
public class NormDist  implements FreeRefFunction{

	public ValueEval evaluate(ValueEval[] args, OperationEvaluationContext ec) {
        for (int i = 0; i < args.length; i++) {
    		// Resolve all reference before calculating
	    	if(args[i] instanceof RefEval){
	    		args[i]= ((RefEval)args[i]).getInnerValueEval();
	    	}
        }
        
        NormalDistributionImpl nDistr = new NormalDistributionImpl(((NumberEval)args[1]).getNumberValue(),((NumberEval)args[2]).getNumberValue());
       
        if(((BoolEval)args[3]).getBooleanValue()){ // Cumulative = true
        	try {
				return new NumberEval(nDistr.cumulativeProbability(((NumberEval)args[1]).getNumberValue()));
			} catch (MathException e) {
				return  ErrorEval.VALUE_INVALID;
			}
        }
        else{
			try {
				return new NumberEval(nDistr.density(((NumberEval)args[1]).getNumberValue()));
			} catch (Exception e) {
				return  ErrorEval.VALUE_INVALID;
			}
        	
        }
	}

}
