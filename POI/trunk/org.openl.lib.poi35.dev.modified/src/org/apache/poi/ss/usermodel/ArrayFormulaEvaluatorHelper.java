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

package org.apache.poi.ss.usermodel;

import org.apache.poi.hssf.record.formula.eval.AreaEval;
import org.apache.poi.hssf.record.formula.eval.BoolEval;
import org.apache.poi.hssf.record.formula.eval.ErrorEval;
import org.apache.poi.hssf.record.formula.eval.NumberEval;
import org.apache.poi.hssf.record.formula.eval.StringEval;
import org.apache.poi.hssf.record.formula.eval.ValueEval;
import org.apache.poi.hssf.record.formula.functions.Function;
import org.apache.poi.hssf.record.formula.functions.FunctionWithArraySupport;
import org.apache.poi.ss.formula.ArrayEval;
import org.apache.poi.ss.formula.TwoDEval;
import org.apache.poi.ss.util.CellRangeAddress;

/**
 * Helper class to manipulate with array formula
 * This class contains methods, common for HSSF and XSSF FormulaEvaluator
 * Better solution - to have abstract class for FormulaEvaluator Interface Implementation
 *  All method need to be static
 *
 * @author Vladimirs Abramovs (Vladimirs.Abramovs at exigenservices.com)
 */
public class ArrayFormulaEvaluatorHelper {
	private static final CellValue CELL_VALUE_NA = CellValue.getError(ErrorConstants.ERROR_NA);
	final static int SCALAR_TYPE = 0;
	final static int ARRAY_TYPE = 1;
	private static final ValueEval ERROR_EVAL_NA = ErrorEval.NA;


	private ArrayFormulaEvaluatorHelper () {
		// has no instance
	}


	/** Transform evaluated Array Values according aimed Range
	 *   Depending on dimensions correlation,
	 *   result array may be restricted
	 *	or his single row/column will cloned
	 *	or some cell will set to #N/A
	 * @param cvs
	 * @param range
	 * @return
	 */
	public static ValueEval[][] transformToRange(ValueEval[][] cvs, CellRangeAddress range) {
		return (ValueEval[][]) transformToRange(cvs, range, false);
	}
	/** Transform evaluated Array Values according aimed Range
	 *   Depending on dimensions correlation,
	 *   result array may be restricted
	 *	or his single row/column will cloned
	 *	or some cell will set to #N/A
	 * @param cvs
	 * @param range
	 * @return
	 */
	public static CellValue[][] transformToRange(CellValue[][] cvs, CellRangeAddress range) {
		return (CellValue[][]) transformToRange(cvs, range, true);
	}
	/** Transform evaluated Array Values according aimed Range
	 *   Depending on dimensions correlation,
	 *   result array may be restricted
	 *	or his single row/column will cloned
	 *	or some cell will set to #N/A
	 * @param cvs
	 * @param range
	 * @return
	 */
	private static Object[][] transformToRange(Object[][] cvs, CellRangeAddress range,
			boolean isCellValue) {

		int nRows = range.getLastRow() - range.getFirstRow() + 1;
		int nColumns = range.getLastColumn() - range.getFirstColumn() + 1;
		Object[][] result = isCellValue ? new CellValue[nRows][nColumns] : new ValueEval[nRows][nColumns];
		int rowStart = range.getFirstRow();
		int colStart = range.getFirstColumn();
		for (int i = rowStart; i <= range.getLastRow(); i++) {
			for (int j = colStart; j <= range.getLastColumn(); j++) {
				Object value;
				if (i - rowStart < cvs.length && j - colStart < cvs[i - rowStart].length) {
					value = cvs[i - rowStart][j - colStart];
				} else {
					boolean needClone = false;
					int cloneRow = 0;
					int cloneCol = 0;
					if (cvs.length == 1) { // Need to clone first colm of cvs
						cloneCol = j - colStart;
						needClone = true;
					}
					if (cvs[0].length == 1) { // Need to clone first row of cvs
						cloneRow = i - rowStart;
						needClone = true;
					}
					if (needClone && cloneCol < cvs[0].length && cloneRow < cvs.length) {
						value = cvs[cloneRow][cloneCol];
					} else {
						// For other cases set cell value to #N/A
						// For those cells we changes also their type to Error
						value = isCellValue ? CELL_VALUE_NA : ErrorEval.NA;
					}
				}
				result[i - rowStart][j - colStart] = value;
			}
		}
		return result;
	}

	/**
	 * Convert Eval value to CellValue
	 * @param val
	 * @return
	 */
	public static CellValue evalToCellValue(ValueEval val) {
		if (val instanceof BoolEval) {
			return CellValue.valueOf(((BoolEval)val).getBooleanValue());
		}
		if (val instanceof NumberEval) {
			return  new CellValue(((NumberEval)val).getNumberValue());
		}
		if (val instanceof StringEval) {
			return  new CellValue(((StringEval)val).getStringValue());
		}
		if (val instanceof ErrorEval) {
			return  new CellValue(((ErrorEval)val).getErrorCode());
		}
		throw new IllegalStateException("Unexpected value (" + val + ")");
	}
	/**
	 * Get single value from ArrayEval  for desired cell
	 * @param evaluationResult
	 * @param cell
	 * @return
	 */
	public static ValueEval dereferenceValue(ArrayEval evaluationResult, Cell cell) {
		CellRangeAddress range = cell.getArrayFormulaRange();
		int rowInArray = cell.getRowIndex()- range.getFirstRow();
		int colInArray = cell.getColumnIndex() - range.getFirstColumn();
		return evaluationResult.getValue(rowInArray,colInArray);
	}
	/**
	 * Get type of parameter (SCALAR_TYPE or ARRAY_TYPE) which support function
	 * for given argument
	 *
	 * @param function
	 * @param argIndex
	 * @return
	 */
	public static int getParameterType(Function function, int argIndex) {

		if (function instanceof FunctionWithArraySupport) {
			// ask new interface(ZS) for argument type
			if (((FunctionWithArraySupport) function).supportArray(argIndex)) {
				return ARRAY_TYPE;
			}
		}
		return SCALAR_TYPE;
	}

	/**
	 * Prepare empty template, which will keep result of evaluation
	 *   A few arguments of function may be array.
	 *   In this case result array will have
	 *   dimension as  such arguments intersection.
	 *   This method calculates result's dimension and prepare empty results
	 *
	 *
	 * @param function
	 * @param args
	 * @param arrayFormula
	 * @return <code>null</code> if function returns a scalar result
	 */
	public static ArrayEval prepareEmptyResult(Function function, ValueEval[] args, boolean arrayFormula) {
		boolean foundArrayArg = false;
		int rowCount = Integer.MIN_VALUE;
		int colCount = Integer.MIN_VALUE;

		for (int i = 0; i < args.length; i++) {
			int argRowCount = Integer.MIN_VALUE;
			int argColCount = Integer.MIN_VALUE;
			if (getParameterType(function, i) == SCALAR_TYPE) {
				ValueEval arg = args[i];
				if (arg instanceof ArrayEval) {
					ArrayEval aa = (ArrayEval) arg;
					argRowCount = aa.getHeight();
					argColCount = aa.getWidth();
				} else if (arg instanceof AreaEval && arrayFormula) {
					AreaEval aa = (AreaEval) arg;
					argRowCount = aa.getHeight();
					argColCount = aa.getWidth();
				} else {
					continue; // Arguments is not array - just skip it
				}
				foundArrayArg = true;
				rowCount = Math.max(rowCount, argRowCount);
				colCount = Math.max(colCount, argColCount);
			}
		}

		if (!foundArrayArg) {
			return null;
		}

		ValueEval[][] emptyArray = new ValueEval[rowCount][colCount];
		return new ArrayEval(emptyArray);
	}

	/**
	 * Prepare arguments for next iteration to call function
	 *  Each argument may be scalar, full array or element(according iteration) of array
	 *
	 * @param function
	 * @param args
	 * @param i
	 * @param j
	 * @return
	 */
	public static ValueEval[] prepareArgsForLoop(Function function, ValueEval[] args, int i, int j) {
		ValueEval[] answer = new ValueEval[args.length];
		for (int argIn = 0; argIn < args.length; argIn++) {
			ValueEval arg = args[argIn];

			if (getParameterType(function, argIn) == SCALAR_TYPE) {
				if (arg instanceof TwoDEval) {
					arg = getArrayValue((TwoDEval) arg, i, j);
				}
			}
			answer[argIn] = arg;
		}

		return answer;
	}

	private static ValueEval getArrayValue(TwoDEval tde, int pRowIndex, int pColIndex) {
		int rowIndex;
		if (pRowIndex >= tde.getHeight()) {
			if (!tde.isRow()) {
				return ERROR_EVAL_NA;
			}
			rowIndex = 0;
		} else {
			rowIndex = pRowIndex;
		}
		int colIndex;
		if (pColIndex >= tde.getWidth()) {
			if (!tde.isColumn()) {
				return ERROR_EVAL_NA;
			}
			colIndex = 0;
		} else {
			colIndex = pColIndex;
		}
		return tde.getValue(rowIndex, colIndex);
	}


	/**
	 * check if ops contain arrays and those should be iterated
	 *
	 * @param function
	 * @param ops
	 * @return
	 */
	public static boolean checkForArrays(Function function, ValueEval[] ops) {

		for (int i = 0; i < ops.length; i++) {
			if ((ops[i] instanceof ArrayEval) && (getParameterType(function, i) == SCALAR_TYPE)) {
				return true;
			}
		}
		return false;
	}
}
