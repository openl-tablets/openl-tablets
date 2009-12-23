/* ====================================================================
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== */

package org.apache.poi.ss.formula;

import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hssf.record.constant.ErrorConstant;
import org.apache.poi.hssf.record.formula.ArrayPtg;
import org.apache.poi.hssf.record.formula.eval.AreaEval;
import org.apache.poi.hssf.record.formula.eval.BoolEval;
import org.apache.poi.hssf.record.formula.eval.ErrorEval;
import org.apache.poi.hssf.record.formula.eval.NumberEval;
import org.apache.poi.hssf.record.formula.eval.RefEval;
import org.apache.poi.hssf.record.formula.eval.StringEval;
import org.apache.poi.hssf.record.formula.eval.ValueEval;
import org.apache.poi.ss.util.NumberToTextConverter;

/**
 *  Class to support evaluated array of values
 *
 * @author Zahars Sulkins(Zahars.Sulkins at exigenservices.com)
 */
public final class ArrayEval implements TwoDEval {

	private final ValueEval[][] _values;
	private byte componentError = 0; 

	public ArrayEval(ArrayPtg ptg) {
		if (ptg == null) {
			throw new IllegalArgumentException("ArrayPtg should not be null");
		}
		Object[][] tokenValues = ptg.getTokenArrayValues();
		int nRows = tokenValues.length;
		int nCols = tokenValues[0].length;
		ValueEval[][] values = new ValueEval[nRows][nCols];
		for (int r=0; r< nRows; r++) {
			Object[] tokenRow = tokenValues[r];
			ValueEval[] row = values[r];
			for (int c=0; c< nCols; c++) {
				row[c] = constructEval(tokenRow[c]);
			}
		}
		_values = values;
	}

	public ArrayEval(ValueEval[][] values) {
		if (values == null) {
			throw new IllegalArgumentException("null is not allowed");
		}
		int nRows = values.length;
		int nCols = values[0].length;
		for (int r=0; r< nRows; r++) {
			ValueEval[] row = values[r];
			for (int c=0; c< nCols; c++) {
				validateValueEval(row[c]);
			}
		}

		_values = values;
	}

	private void validateValueEval(ValueEval valueEval) {
		if (valueEval instanceof NumberEval) {
			return;
		}
		if (valueEval instanceof StringEval) {
			return;
		}
		if (valueEval instanceof BoolEval) {
			return;
		}
		if (valueEval instanceof ErrorEval) {
			return;
		}

		if (valueEval == null) {
			if (false) {
				// TODO throw new IllegalArgumentException("Array elements cannot be null.");
			}
			return;
		}

		if (valueEval instanceof RefEval) {
			throw new IllegalArgumentException("Array elements cannot be of type RefEval");
		}
		if (valueEval instanceof AreaEval) {
			throw new IllegalArgumentException("Array elements cannot be of type AreaEval");
		}
		throw new IllegalArgumentException("Unexpected eval type ("
				+ valueEval.getClass().getSimpleName() + ").");
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getClass().getName()).append(" [");
		sb.append("{");
		for (int r = 0; r < _values.length; r++) {
			if (r > 0) {
				sb.append(";");
			}
			for (int c = 0; c < _values[r].length; c++) {
				if (c > 0) {
					sb.append(",");
				}
				Object o = _values[r][c];
				sb.append(getConstantText(o));
			}
		}
		sb.append("}]");
		return sb.toString();
	}

	/**
	 * TODO - remove this method
	 */
	public ValueEval[][] getArrayValues() {
		return _values;
	}

	/**
	 * get element of array as Value Eval
	 * @param row
	 * @param col
	 * @return
	 */
	public ValueEval getValue(int row, int col) {
		return _values[row][col];
	}

	/**
	 * Convert Object to ValueEval
	 */
	private static ValueEval constructEval(Object o) {
		if (o == null) {
			throw new RuntimeException("Array item cannot be null");
		}
		if (o instanceof String) {
			return new StringEval( (String)o );
		}
		if (o instanceof Double) {
			return new NumberEval(((Double)o).doubleValue());
		}
		if (o instanceof Boolean) {
			return BoolEval.valueOf(((Boolean)o).booleanValue());
		}
		if (o instanceof ErrorConstant) {
			return ErrorEval.valueOf(((ErrorConstant)o).getErrorCode());
		}
		throw new IllegalArgumentException("Unexpected constant class (" + o.getClass());
	}


	/**
	 * get String contains object's value
	 * @param o
	 * @return
	 */
	private static String getConstantText(Object o) {

		if (o == null) {
			return "Error - null";
// TODO			throw new RuntimeException("Array item cannot be null");
		}
		if (o instanceof StringEval) {
			return "\"" + ((StringEval)o).getStringValue() + "\"";
		}
		if (o instanceof NumberEval) {
			return NumberToTextConverter.toText(((NumberEval)o).getNumberValue());
		}
		if (o instanceof Boolean) {
			return ((Boolean)o).booleanValue() ? "TRUE" : "FALSE";
		}
		if (o instanceof ErrorEval) {
			return ErrorEval.getText(((ErrorEval)o).getErrorCode());
		}
		throw new IllegalArgumentException("Unexpected constant class (" + o.getClass().getName() + ")");
	}

	/**
	 * return Array as ValueEval list
	 * @return
	 */
	public List<ValueEval> getArrayAsEval() {

		List<ValueEval> l = new ArrayList<ValueEval>();
		for(int r=0; r< _values.length; r++) {
			ValueEval[] row = _values[r];
			for (int c=0; c<row.length; c++) {
				l.add(row[c]);
			}
		}
		return l;
	}

	/**
	 * get row count
	 * @return
	 */
	public int getHeight() {
		return _values.length;
	}

	/**
	 * get column count
	 * @return
	 */
	public int getWidth() {
		return _values[0].length;
	}

	public boolean isRow() {
		return _values.length == 1;
	}

	public boolean isColumn() {
		return _values[0].length == 1;
	}
	public enum BooleanContent{ONLY_FALSE,ONLY_TRUE,MIXED}

	/**
	 *  Check if content of boolean array ONLY_FALSE, ONLY_TRUE or MIXED
	 *  if content is not boolean then return MIXED
	 * @return
	 */
	public BooleanContent checkBooleanContent() {
		try {
			BoolEval first = (BoolEval) _values[0][0];
			for (int i = 0; i < _values.length; i++) {
				for (int j = 0; j < _values[i].length; j++) {
					if (first.equals(_values[i][j])) {
						return BooleanContent.MIXED;
					}
				}
			}

			if (first.getBooleanValue()) {
				return BooleanContent.ONLY_TRUE;
			}
			return BooleanContent.ONLY_FALSE;
		} catch (Exception e) {
			return BooleanContent.MIXED;
		}
	}

	public byte getComponentError() {
		return componentError;
	}

	public void setComponentError(byte componentError) {
		this.componentError = componentError;
	}
	
	public ArrayEval offset(int relFirstRowIx, int relLastRowIx, int relFirstColIx, int relLastColIx){
		if ( (relFirstRowIx < 0 || relFirstRowIx > relLastRowIx || relLastRowIx>= getHeight()) || 
			 (relFirstColIx <0 || relFirstColIx > relLastColIx || relLastColIx >= getWidth() )){
			 throw new IllegalArgumentException("Irregular params: " + relFirstRowIx + ";" + relLastRowIx + ";" 
					                            + relFirstColIx + ";" + relLastRowIx ); 
		}
		ValueEval[][] result = new ValueEval[relLastRowIx - relFirstRowIx + 1][relLastColIx - relFirstColIx +1];
		for (int r=relFirstRowIx; r<=relLastRowIx; r++){
			for (int c=relFirstColIx; c<=relLastColIx; c++ ){
				result[r-relFirstRowIx][c-relFirstColIx] = _values[r][c];
			}
		}
		return new ArrayEval(result);
		
	}

    public TwoDEval getColumn(int columnIndex) {
        // TODO Auto-generated method stub
        return null;
    }

    public TwoDEval getRow(int rowIndex) {
        // TODO Auto-generated method stub
        return null;
    }	
}
