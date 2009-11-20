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

/** 
 *  Class to support evaluated array of values
 * @author zsulkins(ZS)
 *
 */
public class ArrayEval  implements ValueEval {

	//ArrayPtg thePtg;
	Object[][] values = null;
	boolean illegalForAggregation = false; // if result is invalid for aggregation. it could be true, if "uncompatible in size" arrays were used 
	
	
	/**
	 *  is array unsuitable for future aggregation? 
	 * @return
	 */
	public boolean isIllegalForAggregation(){
		return illegalForAggregation;
	}
	
	/**
	 *  set feature "unsuitable for future aggregation
	 *  it could be true, if "uncompatible in size" arrays were used
	 * @param value
	 */
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
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
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

		
	/**
	 * get array content
	 * @return
	 */
	public Object[][] getArrayValues(){
		return values;
	}
	
	/**
	 * get element of array
	 * @param row
	 * @param col
	 * @return
	 */
	public Object getArrayElement(int row, int col){
		return values[row][col];
	}
	
	/**
	 * get element of array as Value Eval
	 * @param row
	 * @param col
	 * @return
	 */
	public ValueEval getArrayElementAsEval(int row, int col){
		return constructEval(getArrayElement(row,col));
	}
	
	
	/**
	 * Convert Object to ValueEval
	 * @param o
	 * @return
	 */
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
			return BoolEval.valueOf((Boolean)o);
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
	

	/**
	 * get String contains object's value
	 * @param o
	 * @return
	 */
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
	
	/**
	 * return Array as ValueEval list
	 * @return
	 */
	public List<ValueEval> getArrayAsEval(){
		
		List<ValueEval> l = new ArrayList<ValueEval>();
		for(int r=0; r< values.length; r++){
			for (int c=0; c<values[r].length; c++){
				l.add(ArrayEval.constructEval(values[r][c]));
			}
		}
		return l;
	}
	
	/** 
	 * Convert 2D array to 1D array
	 * 	 * @return
	 */
	public Object[] getSingleDimensionalArray(){
		Object[] l = new Object[getRowCounter()*getColCounter()];
		for(int r=0; r< values.length; r++){
			for (int c=0; c<values[r].length; c++){
				l[r*getColCounter()+c] = values[r][c];
			}
		}
		return l;		
	}
	
	/**
	 * Is array empty?
	 * @return
	 */
	public Object[][] getEmptyArray(){
		
		return new Object[getRowCounter()][getColCounter()];
	}
	
	/**
	 * get row count
	 * @return
	 */
	public int getRowCounter(){
		return values.length;
	}
	
	/**
	 * get column count
	 * @return
	 */
	public int getColCounter(){
		if (getRowCounter() == 0)
			return 0;
		return values[0].length;
	}
	
	/*
	 * offset from the array
	 */
	/**
	 * get subarray
	 * @param rowFrom
	 * @param rowTo
	 * @param colFrom
	 * @param colTo
	 * @return
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
	
	public enum BooleanContent{ONLY_FALSE,ONLY_TRUE,MIXED};
	
	/**
	 *  Check if content of boolean array ONLY_FALSE, ONLY_TRUE or MIXED
	 *  if content is not boolean then return MIXED
	 * @return
	 */
	public BooleanContent checkBooleanContent(){
		try{
		BoolEval first =  (BoolEval)values[0][0];
		for(int i=0;i<values.length;i++)
			for(int j=0;j<values[i].length;j++)
				if(first.equals((BoolEval)values[i][j]))
						return BooleanContent.MIXED;
						
		if(first.getBooleanValue())
			return BooleanContent.ONLY_TRUE;
		else
			return BooleanContent.ONLY_FALSE;
		}
		catch (Exception e){
			return BooleanContent.MIXED;
		}
	}
	
	
	/**
	 * expose Array as area on sheet (top-left)
	 * convenience methods to reuse existing code
	 * @return
	 */
	public AreaEval arrayAsArea(){
		
		return new AreaEval(){
			
			/* (non-Javadoc)
			 * @see org.apache.poi.hssf.record.formula.eval.AreaEval#getFirstRow()
			 */
			public int getFirstRow(){
				return 0;
			}
			
			/* (non-Javadoc)
			 * @see org.apache.poi.hssf.record.formula.eval.AreaEval#getLastRow()
			 */
			public int getLastRow(){
				return getRowCounter()-1;
			}
			
			/* (non-Javadoc)
			 * @see org.apache.poi.hssf.record.formula.eval.AreaEval#getFirstColumn()
			 */
			public int getFirstColumn(){
				return 0;
			}
			
			/* (non-Javadoc)
			 * @see org.apache.poi.hssf.record.formula.eval.AreaEval#getLastColumn()
			 */
			public int getLastColumn(){
				return getColCounter()-1;
			}
			
		    /* (non-Javadoc)
		     * @see org.apache.poi.hssf.record.formula.eval.AreaEval#isRow()
		     */
		    public boolean isRow(){
		    	return (getRowCounter()==1);
		    }
		    
		    /* (non-Javadoc)
		     * @see org.apache.poi.hssf.record.formula.eval.AreaEval#isColumn()
		     */
		    public boolean isColumn(){
		    	return (getColCounter() == 1);
		    }

		    /* (non-Javadoc)
		     * @see org.apache.poi.hssf.record.formula.eval.AreaEval#getValueAt(int, int)
		     */
		    public ValueEval getValueAt(int row, int col){
		    	return getArrayElementAsEval(row, col);
		    }

		    /* (non-Javadoc)
		     * @see org.apache.poi.hssf.record.formula.eval.AreaEval#contains(int, int)
		     */
		    public boolean contains(int row, int col){
		    	if ( (row < getRowCounter()) && (col < getColCounter()) ){
		    		return true;
		    	}
		    	return false;
		    }

		    /* (non-Javadoc)
		     * @see org.apache.poi.hssf.record.formula.eval.AreaEval#containsColumn(short)
		     */
		    public boolean containsColumn(short col){
		    	if (col < getColCounter())
		    		return true;
		    	return false;
		    }
		    
		    /* (non-Javadoc)
		     * @see org.apache.poi.hssf.record.formula.eval.AreaEval#containsRow(int)
		     */
		    public boolean containsRow(int row){
		    	if (row < getRowCounter() )
		    		return true;
		    	return false;
		    }

		    /* (non-Javadoc)
		     * @see org.apache.poi.hssf.record.formula.eval.AreaEval#getWidth()
		     */
		    public int getWidth(){
		    	return getColCounter(); 
		    	
		    }
		    
		    public int getHeight(){
		    	return getRowCounter();
		    }

		    /* (non-Javadoc)
		     * @see org.apache.poi.hssf.record.formula.eval.AreaEval#getRelativeValue(int, int)
		     */
		    public ValueEval getRelativeValue(int relativeRowIndex, int relativeColumnIndex){
		    	return getArrayElementAsEval(relativeRowIndex, relativeColumnIndex);
		    }

		    /* (non-Javadoc)
		     * @see org.apache.poi.hssf.record.formula.eval.AreaEval#offset(int, int, int, int)
		     */
		    public AreaEval offset(int relFirstRowIx, int relLastRowIx, int relFirstColIx, int relLastColIx){
		    	ArrayEval offset = ArrayEval.this.offset(relFirstRowIx, relLastRowIx, relFirstColIx, relLastColIx);
		    	return offset.arrayAsArea();
		    }
		    
			
		};
	}
	
	
}
