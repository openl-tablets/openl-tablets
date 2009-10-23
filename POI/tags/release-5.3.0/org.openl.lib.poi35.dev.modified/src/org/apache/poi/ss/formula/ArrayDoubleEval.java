package org.apache.poi.ss.formula;



// array contains only Doubles - type checking and convenience 
public class ArrayDoubleEval extends ArrayEval {

	public ArrayDoubleEval(Double[][] array){
		super(array);
	}
	
	public ArrayDoubleEval(Double[][] array, boolean isIllegalForAggregation){
		super(array,isIllegalForAggregation);
	}

	
	@Override 
	public Double getArrayElement(int row, int col) {
		return (Double)(super.getArrayElement(row, col));
	}
	
	
}
