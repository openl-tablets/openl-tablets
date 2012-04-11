// !! new class  ZS 
package org.apache.poi.ss.formula;

import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hssf.record.UnicodeString;
import org.apache.poi.hssf.record.constant.ErrorConstant;
import org.apache.poi.hssf.record.formula.ArrayPtg;
import org.apache.poi.hssf.record.formula.eval.AreaEval;
import org.apache.poi.hssf.record.formula.eval.BoolEval;
import org.apache.poi.hssf.record.formula.eval.NumberEval;
import org.apache.poi.hssf.record.formula.eval.StringEval;
import org.apache.poi.hssf.record.formula.eval.ValueEval;

public class ArrayEval  implements ValueEval {

	//ArrayPtg thePtg;
	Object[][] values = null;
	boolean illegalForAggregation = false; // if result is invalid for aggregation. it could be true, if "uncompatible in size" arrays were used 
	
	
	public boolean isIllegalForAggregation(){
		return illegalForAggregation;
	}
	
	public void setIllegalForAggregation(boolean value){
		illegalForAggregation = value;
	}
	
	public ArrayEval(ArrayPtg ptg){
		if (ptg == null)
			throw new IllegalArgumentException("ArrayPtg should not be null");
		values = ptg.getTokenArrayValues();
	}
	
    public ArrayEval(Object[][] array){
		if (array == null)
			throw new IllegalArgumentException("null is not allowed");
		values = array;
    }
    
    public ArrayEval(Object[][] array, boolean isIllegalForAggregation){
    	this(array);
    	illegalForAggregation = isIllegalForAggregation;
    }
	
	@Override
	public String toString(){
		StringBuffer b = new StringBuffer();
		b.append("{");
	  	for (int r=0;r<values.length;r++) {
			if (r > 0) {
				b.append(";");
			}
			for (int c=0;c<values[r].length;r++) {
			  	if (c > 0) {
					b.append(",");
				}
		  		Object o = values[r][c];
		  		b.append(getConstantText(o));
		  	}
		  }
		b.append("}");
		return b.toString();
	}

		
	public Object[][] getArrayValues(){
		return values;
	}
	
	public Object getArrayElement(int row, int col){
		return values[row][col];
	}
	
	public ValueEval getArrayElementAsEval(int row, int col){
		return constructEval(getArrayElement(row,col));
	}
	
	
	public static ValueEval constructEval(Object o){
		if (o == null) {
			throw new RuntimeException("Array item cannot be null");
		}
		if (o instanceof ValueEval)
			return (ValueEval)o;
		
		if (o instanceof String) {
			return new StringEval( (String)o );
		}
		if (o instanceof Double) {
			return new NumberEval((Double)o);
		}
		if (o instanceof Boolean) {
			return new BoolEval((Boolean)o);
		}
		// I don't know what should we do if error is an array. I throw an exception for now
		if (o instanceof ErrorConstant) {
			throw new IllegalArgumentException("Error in array" + ((ErrorConstant)o).getText());
		}
		// if string constants in ArrayPtg are encodes as UnicodeString
		if (o instanceof UnicodeString){
			return new StringEval( ((UnicodeString)o).getString());
		}
		
		throw new IllegalArgumentException("Unexpected constant class (" + o.getClass());
	}
	

	public static String getConstantText(Object o) {

		if (o == null) {
			throw new RuntimeException("Array item cannot be null");
		}
		if (o instanceof String) {
			return "\"" + (String)o + "\"";
		}
		if (o instanceof Double) {
			return ((Double)o).toString();
		}
		if (o instanceof Boolean) {
			return ((Boolean)o).booleanValue() ? "TRUE" : "FALSE";
		}
		if (o instanceof ErrorConstant) {
			return ((ErrorConstant)o).getText();
		}
		throw new IllegalArgumentException("Unexpected constant class (" + o.getClass().getName() + ")");
	}
	
	public List<ValueEval> getArrayAsEval(){
		
		List<ValueEval> l = new ArrayList<ValueEval>();
		for(int r=0; r< values.length; r++){
			for (int c=0; c<values[r].length; c++){
				l.add(ArrayEval.constructEval(values[r][c]));
			}
		}
		return l;
	}
	
	public Object[] getSingleDimensionalArray(){
		Object[] l = new Object[getRowCounter()*getColCounter()];
		for(int r=0; r< values.length; r++){
			for (int c=0; c<values[r].length; c++){
				l[r*getColCounter()+c] = values[r][c];
			}
		}
		return l;		
	}
	
	public Object[][] getEmptyArray(){
		
		return new Object[getRowCounter()][getColCounter()];
	}
	
	public int getRowCounter(){
		return values.length;
	}
	
	public int getColCounter(){
		if (getRowCounter() == 0)
			return 0;
		return values[0].length;
	}
	
	/*
	 * offset from the array
	 */
	public ArrayEval offset(int rowFrom, int rowTo, int colFrom, int colTo){
		
		if (rowFrom<=0 || rowFrom >= getRowCounter() || rowTo<rowFrom || 
			colFrom<=0 || colFrom >= getColCounter() || colTo<colFrom
		)
			throw new IllegalArgumentException("rowFrom: " + rowFrom + "  rowTo: " + rowTo + " colFrom: " + colFrom + " colTo: " + colTo );
		
		int row = Math.min(getRowCounter(), rowTo);
		int col = Math.min(getColCounter(), colTo);
		
		Object[][] result = new Object[row - rowFrom+1][col - colFrom+1];
		for (int r=rowFrom; r<=row; r++){
			for (int c=colFrom; c<=col; c++) {
				result[r][c] = values[r][c];
			}
		}
		
		return new ArrayEval(result);
	}
	
	// expose Array as area on sheet (top-left)
	// convenience methods to reuse existing code
	public AreaEval arrayAsArea(){
		
		return new AreaEval(){
			
			public int getFirstRow(){
				return 0;
			}
			
			public int getLastRow(){
				return getRowCounter()-1;
			}
			
			public int getFirstColumn(){
				return 0;
			}
			
			public int getLastColumn(){
				return getColCounter()-1;
			}
			
		    public boolean isRow(){
		    	return (getRowCounter()==1);
		    }
		    
		    public boolean isColumn(){
		    	return (getColCounter() == 1);
		    }

		    public ValueEval getValueAt(int row, int col){
		    	return getArrayElementAsEval(row, col);
		    }

		    public boolean contains(int row, int col){
		    	if ( (row < getRowCounter()) && (col < getColCounter()) ){
		    		return true;
		    	}
		    	return false;
		    }

		    public boolean containsColumn(short col){
		    	if (col < getColCounter())
		    		return true;
		    	return false;
		    }
		    
		    public boolean containsRow(int row){
		    	if (row < getRowCounter() )
		    		return true;
		    	return false;
		    }

		    public int getWidth(){
		    	return getColCounter(); 
		    	
		    }
		    
		    public int getHeight(){
		    	return getRowCounter();
		    }

		    public ValueEval getRelativeValue(int relativeRowIndex, int relativeColumnIndex){
		    	return getArrayElementAsEval(relativeRowIndex, relativeColumnIndex);
		    }

		    public AreaEval offset(int relFirstRowIx, int relLastRowIx, int relFirstColIx, int relLastColIx){
		    	ArrayEval offset = ArrayEval.this.offset(relFirstRowIx, relLastRowIx, relFirstColIx, relLastColIx);
		    	return offset.arrayAsArea();
		    }
		    
			
		};
	}
	
	
}
