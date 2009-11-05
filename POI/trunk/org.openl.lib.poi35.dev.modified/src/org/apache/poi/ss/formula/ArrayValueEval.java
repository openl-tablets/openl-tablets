package org.apache.poi.ss.formula;

import org.apache.poi.hssf.record.formula.eval.ValueEval;

/**
 * Array which contains ValueEval as elements
 * @author zsulkins
 *
 */
public class ArrayValueEval extends ArrayEval{
   
	public ArrayValueEval(ValueEval[][] array){
		super(array);
	}
	
	public ArrayValueEval(ValueEval[][] array, boolean isIllegalForAggregation){
		super(array,isIllegalForAggregation);
	}

	
	/* (non-Javadoc)
	 * @see org.apache.poi.ss.formula.ArrayEval#getArrayElement(int, int)
	 */
	@Override 
	public ValueEval getArrayElement(int row, int col) {
		return (ValueEval)(super.getArrayElement(row, col));
	}
		
}
