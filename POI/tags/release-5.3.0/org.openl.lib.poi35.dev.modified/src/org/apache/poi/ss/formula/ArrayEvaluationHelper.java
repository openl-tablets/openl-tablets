/**
 * 
 */
package org.apache.poi.ss.formula;

import org.apache.poi.hssf.record.formula.eval.AreaEval;
import org.apache.poi.hssf.record.formula.eval.BlankEval;
import org.apache.poi.hssf.record.formula.eval.FunctionEval;
import org.apache.poi.hssf.record.formula.eval.OperationEval;
import org.apache.poi.hssf.record.formula.eval.ValueEval;
import org.apache.poi.hssf.record.formula.functions.ArrayMode;
import org.apache.poi.hssf.record.formula.functions.FunctionWithArraySupport;

/**
 * @author vabramovs
 *
 */
public class ArrayEvaluationHelper {
	
	 final static  int SCALAR_TYPE = 0;
	 final static  int ARRAY_TYPE = 1;
	
   // Has no instance
	private ArrayEvaluationHelper(){
	}
	
	
	public static int getParameterType(OperationEval operation,int argIndex){
		int 	answer = SCALAR_TYPE; 
		
		if(operation instanceof FunctionEval) {
			FunctionEval fe = (FunctionEval) operation;
			if(fe.getFunction() instanceof FunctionWithArraySupport){
				// ask new interface(ZS) for argument type 
				if(((FunctionWithArraySupport)fe.getFunction()).supportArray(argIndex))
					answer = ARRAY_TYPE;
			}
				
		}
		return answer;
	}
    
	
	public static ValueEval prepareEmptyResult(OperationEval operation,ValueEval[] ops, boolean arrayFormula) {
	    int rowCount = Integer.MAX_VALUE;
	    int colCount = Integer.MAX_VALUE;
	    
	    boolean illegalForAggregation = false;
	    
	    
	    for(int i=0;i<ops.length;i++){
		    int argRowCount=Integer.MAX_VALUE;
		    int argColCount=Integer.MAX_VALUE;
		    if(getParameterType(operation,i) == SCALAR_TYPE){
		    	if (ops[i] instanceof ArrayEval) 
		    		{
				    argRowCount = ((ArrayEval)ops[i]).getRowCounter();
				    argColCount = ((ArrayEval)ops[i]).getColCounter();
				    illegalForAggregation = illegalForAggregation || ((ArrayEval)ops[i]).isIllegalForAggregation();
		    		}
		    	else if (ops[i] instanceof AreaEval && arrayFormula)
		    		{
				    argRowCount = ((AreaEval)ops[i]).getHeight();
				    argColCount = ((AreaEval)ops[i]).getWidth();
		    		}
		    	else 
		    		continue;    // Arguments is not array - just skip it
		    	if(argRowCount != rowCount){
		    		if(rowCount != Integer.MAX_VALUE){
		    			illegalForAggregation = true;
		    		}
	    			rowCount = Math.min(rowCount, argRowCount);
		    	}
		    	if(argColCount != colCount){
		    		if(colCount != Integer.MAX_VALUE){
		    			illegalForAggregation = true;
		    		}	
		    		colCount = Math.min(colCount, argColCount);
		    	}
		    }
	    }
	    
	    if (colCount == Integer.MAX_VALUE || rowCount == Integer.MAX_VALUE)
	    	return null;
	    
	    ValueEval[][] emptyArray  = new ValueEval[rowCount][colCount];  
	    ValueEval answer = new ArrayEval(emptyArray);
	    
	    ((ArrayEval)answer).setIllegalForAggregation(illegalForAggregation);
		return answer;
	}
	


	public static  ValueEval[] prepareArg4Loop(OperationEval operation,ValueEval[] ops, int i, int j, boolean trackAreas){
		ValueEval[] answer = new ValueEval[ops.length];
		     for(int argIn =0; argIn <ops.length;argIn++){
		    	 if(getParameterType(operation,argIn) == SCALAR_TYPE){
			    	 if(ops[argIn] instanceof ArrayEval){
			    		 answer[argIn] = ((ArrayEval)ops[argIn]).getArrayElementAsEval(i,j);
			    	 }
			    	 else if (ops[argIn] instanceof AreaEval && trackAreas){
			    		 answer[argIn] = ((AreaEval)ops[argIn]).getRelativeValue(i, j);
			    		 
			    	 }
			    	 else {
			    		 answer[argIn] = ops[argIn];
			    	 }
		    	 }
		    	 else { // Array type
		    		 answer[argIn] = ops[argIn];
		    		 
		    	 } 
		    		 
		     }
		
		return answer;
	}
	
	
/*
 * check if params contain arrays and those should be iterated
 */
	public static boolean checkForArrays(OperationEval operation,ValueEval[] ops){
		
		for(int i=0; i<ops.length; i++){
			if ( (ops[i] instanceof ArrayEval) && ( getParameterType(operation, i) == SCALAR_TYPE) )
				return true;
		}
		return false;
		
	}
	
	public static boolean specialModeForArray(OperationEval operation){
		if(operation instanceof FunctionEval) {
			FunctionEval fe = (FunctionEval) operation;
			if(fe.getFunction() instanceof ArrayMode){
				return true;
			}
		}
		return false;
	}	

}
