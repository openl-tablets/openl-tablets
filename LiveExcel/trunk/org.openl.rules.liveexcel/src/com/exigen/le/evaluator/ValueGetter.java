/**
 * 
 */
package com.exigen.le.evaluator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.record.formula.eval.ErrorEval;
import org.apache.poi.hssf.record.formula.eval.NumberEval;
import org.apache.poi.hssf.record.formula.eval.RefEval;
import org.apache.poi.hssf.record.formula.eval.StringEval;
import org.apache.poi.hssf.record.formula.eval.ValueEval;
import org.apache.poi.hssf.record.formula.functions.FreeRefFunction;
import org.apache.poi.ss.formula.OperationEvaluationContext;

import com.exigen.le.LiveExcel;
import com.exigen.le.smodel.SMHelper;
import com.exigen.le.smodel.accessor.ValueHolder;

/**
 * Class provide access to input object's properties from Excel expression
 * @author vabramovs
 *
 */
public class ValueGetter implements FreeRefFunction {
	
	
	private static final Log LOG = LogFactory.getLog(ValueGetter.class);
	
	private String valueName;
	public ValueGetter (String valueName){
		this.valueName=valueName;
	}

	/* (non-Javadoc)
	 * @see org.apache.poi.hssf.record.formula.functions.FreeRefFunction#evaluate(org.apache.poi.hssf.record.formula.eval.ValueEval[], org.apache.poi.ss.formula.OperationEvaluationContext)
	 */
	public ValueEval evaluate(ValueEval[] arg0, OperationEvaluationContext arg1) {
	    StringEval objectID = null;
	    int index = 0;
	    
	    try {
			for(int i=0;i<arg0.length;i++){
				if(arg0[i] instanceof RefEval){
			    	arg0[i]= ((RefEval)arg0[i]).getInnerValueEval();
				}
			}
			switch(arg0.length){
				default:
			        return ErrorEval.VALUE_INVALID;
			case 2:
				if( arg0[1] instanceof StringEval){
				    objectID = (StringEval) arg0[1];
				}
				else{	
					return ErrorEval.VALUE_INVALID;
				}
			case 1:
				if(arg0[0] instanceof StringEval){
					if(objectID == null){
			    	    objectID = (StringEval) arg0[0];
					}
					else {
						
						try {
							index = (int)Integer.parseInt(((StringEval) arg0[0]).getStringValue())-1; // Excel use 1-base index, we 0-base
						} catch (Exception e) {
				       		return ErrorEval.VALUE_INVALID;
						}
					}
				} 
				else if (arg0[0] instanceof NumberEval) {
					index = (int)((NumberEval)arg0[0]).getNumberValue()-1; // Excel use 1-base index, we 0-base
				}
			   	Object result;
			   	DataPool pool = ThreadEvaluationContext.getDataPool();
				ValueHolder parent;
				parent = (ValueHolder)pool.get(	objectID.getStringValue());
				if(arg0.length==2){  // Take element from list
					result = parent.getValue(valueName, index);
				}
				else {
					result = parent.getValue(valueName);
				}
				if(LOG.isTraceEnabled()){
					String returned = "Return property "+valueName+"="+SMHelper.valueToString(result);
					LOG.trace(returned);
				}
				if(result == null){
					String msg = " Fault to get value "+valueName+" from parent "+parent.getModel().getName();
					LOG.error(msg);
					throw new RuntimeException(msg);
				}
				ValueEval answer =LiveExcelEvaluator.createEvalForObject(result, arg1.getWorkbookEvaluator(),pool);
				if(LOG.isTraceEnabled()){
					String msg = "Return property "+valueName+"="+SMHelper.valueToString(answer);
					LOG.trace(msg);
				}

				return answer;
			}
		} catch (Exception e) {
			String msg = " Fault to get value "+valueName;
			if(objectID != null){
				msg = msg+" from parent "+objectID;
			}
			LOG.error(msg);
			throw new RuntimeException(msg,e);
		}
    }
		

}
