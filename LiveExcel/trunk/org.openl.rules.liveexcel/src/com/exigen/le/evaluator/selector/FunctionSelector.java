/**
 * 
 */
package com.exigen.le.evaluator.selector;

import java.util.List;
import java.util.Map;

import com.exigen.le.evaluator.ThreadEvaluationContext;
import com.exigen.le.smodel.Function;

/**
 * @author vabramovs
 *
 */
public interface FunctionSelector {
	
	/** 
	 * Select function matching this context
	 * @param functions
	 * @param context This parameters always is ThreadEvaluationContext.getInstance() to indicate that ThreadEvaluationContext must be fulfilled before
	 * @return
	 */
	public Function selectFunction(String functionName,List<Function> functions,ThreadEvaluationContext context );

}
