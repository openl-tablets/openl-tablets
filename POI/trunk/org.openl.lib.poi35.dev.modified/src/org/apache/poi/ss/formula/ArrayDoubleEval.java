package org.apache.poi.ss.formula;
/**
 * Class to support evaluated array of doubles 
 * @author zsulkins(ZS)
 */
//array contains only Doubles - type checking and convenience 
public class ArrayDoubleEval extends ArrayEval {

	public ArrayDoubleEval(Double[][] array){
		super(array);
	}
	
	public ArrayDoubleEval(Double[][] array, boolean isIllegalForAggregation){
		super(array,isIllegalForAggregation);
	}
	
	/* (non-Javadoc)
	 * @see org.apache.poi.ss.formula.ArrayEval#getArrayElement(int, int)
	 */
	@Override 
	public Double getArrayElement(int row, int col) {
		return (Double)(super.getArrayElement(row, col));
	}
	
	
}
