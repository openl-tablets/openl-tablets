package org.apache.poi.ss.formula;

import org.apache.poi.hssf.record.formula.eval.ValueEval;

// Contains ValueEval as elements
public class ArrayValueEval extends ArrayEval{
   
	public ArrayValueEval(ValueEval[][] array){
		super(array);
	}
	
	public ArrayValueEval(ValueEval[][] array, boolean isIllegalForAggregation){
		super(array,isIllegalForAggregation);
	}

	
	@Override 
	public ValueEval getArrayElement(int row, int col) {
		return (ValueEval)(super.getArrayElement(row, col));
	}
		
}
