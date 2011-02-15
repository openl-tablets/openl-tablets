/**
 *  Dummy selector - return first occurance of function
 */
package com.exigen.le.evaluator.selector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.le.evaluator.ThreadEvaluationContext;
import com.exigen.le.smodel.Function;

/**
 * Select function by "Effective" date
 * @author vabramovs
 *
 */
public class FunctionByDateSelector implements FunctionSelector,Comparator<Function> {
	private static final Log LOG = LogFactory.getLog(FunctionByDateSelector.class);

	/* (non-Javadoc)
	 * @see com.exigen.le.evaluator.FunctionSelector#selectFunction(java.util.List, java.util.Map)
	 */
	
	 public Function selectFunction(String functionName,List<Function> functions,
			ThreadEvaluationContext context) {
		List<Function> rivals = new ArrayList<Function>();
		for(Function func:functions){
			if(func.getName().equals(functionName))
				if(func.getEffectiveDate()!= null)
					rivals.add(func);
		}
		Function[] array= rivals.toArray(new Function[rivals.size()]);
		Arrays.sort(array, this);
		Date date;
		int i= 0;
		try {
			date = ThreadEvaluationContext.getVersion().getDate();
			for(;i<array.length && array[i].getEffectiveDate().before(date) ;i++);
			i=i-1;
		} catch (Exception e) {
			i=(-1);
		}
		if(i<0){
			Function result = new DummyFunctionSelector().selectFunction(functionName, functions, context);
			String msg ="Function "+functionName+" is choosen random. Calculation date is before first effective date";
			if(array.length>0)
				msg = msg+":"+array[0].getEffectiveDate();
			LOG.trace(msg);
			return result;
		}
		return array[i];
	}

	public int compare(Function o1, Function o2) {
		return o1.getEffectiveDate().compareTo(o2.getEffectiveDate());
	}
	
}
