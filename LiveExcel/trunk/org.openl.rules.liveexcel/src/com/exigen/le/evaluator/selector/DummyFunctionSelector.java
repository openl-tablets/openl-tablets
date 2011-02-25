/**
 *  Dummy selector - return first occurance of function
 */
package com.exigen.le.evaluator.selector;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.le.evaluator.ThreadEvaluationContext;
import com.exigen.le.smodel.Function;

/**
 * Select first (random) function from same-named
 * @author vabramovs
 *
 */
public class DummyFunctionSelector implements FunctionSelector {
	private static final Log LOG = LogFactory.getLog(DummyFunctionSelector.class);

	/* (non-Javadoc)
	 * @see com.exigen.le.evaluator.FunctionSelector#selectFunction(java.util.List, java.util.Map)
	 */
	
	public Function selectFunction(String functionName,List<Function> functions,
			ThreadEvaluationContext context) {
		for(Function func:functions){
			if(func.getName().equalsIgnoreCase(functionName))
				return func;
		}
		String msg = "Function "+functionName+" not found ";
		LOG.error(msg);
		throw new RuntimeException(msg);
	}
	
}
