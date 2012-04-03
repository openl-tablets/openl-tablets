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
import org.apache.poi.hssf.record.formula.eval.RefEval;
import org.apache.poi.hssf.record.formula.eval.StringEval;
import org.apache.poi.hssf.record.formula.eval.ValueEval;
import org.apache.poi.hssf.record.formula.function.FunctionMetadata;
import org.apache.poi.hssf.record.formula.functions.Function;
import org.apache.poi.hssf.record.formula.functions.FunctionWithArraySupport;
import org.apache.poi.ss.formula.ArrayEval;
import org.apache.poi.ss.formula.TwoDEval;
import org.apache.poi.ss.formula.WorkbookEvaluator;
import org.apache.poi.ss.util.CellRangeAddress;

/**
 * Helper class to manipulate with array formula
 * This class contains methods, common for HSSF and XSSF FormulaEvaluator
 * Better solution - to have abstract class for FormulaEvaluator Interface Implementation
 *  All method need to be static
 *
 * @author Vladimirs Abramovs (Vladimirs.Abramovs at exigenservices.com)
 */
/**
 * @author vabramovs
 *
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
		ValueEval[][] rangeVal =ArrayFormulaEvaluatorHelper.transformToRange(evaluationResult.getArrayValues(),range);
		int rowInArray = cell.getRowIndex()- range.getFirstRow();
		int colInArray = cell.getColumnIndex() - range.getFirstColumn();
		return rangeVal[rowInArray][colInArray];
	}
	/**
	 * Get type of parameter (SCALAR_TYPE or ARRAY_TYPE) which support function
	 * for given argument
	 *
	 * @param function
	 * @param functionMetaData
	 * @param argIndex
	 * @return
	 */
	public static int getParameterType(Function function, FunctionMetadata functionMetaData, int argIndex) {
		int oldanswer = SCALAR_TYPE;
		if (function instanceof FunctionWithArraySupport) {
			// ask new interface(ZS) for argument type
			if (((FunctionWithArraySupport) function).supportArray(argIndex)) {
				oldanswer = ARRAY_TYPE;
			}
		}
//		int answer = SCALAR_TYPE;;
//		int index = argIndex;
//		if (functionMetaData != null) {
//				byte[] parameterClassCodes = functionMetaData.getParameterClassCodes();
//				if(index >= parameterClassCodes.length){ // In case of unlimited Vargs
//					index = parameterClassCodes.length-1;
//				}
//					
//			// If function may accept reference it means that it may accept array as result of ref evaluation?
////			if (parameterClassCodes[index] != OperationPtg.CLASS_VALUE) {
//			if (parameterClassCodes[index] == OperationPtg.CLASS_ARRAY) {
////				return ARRAY_TYPE;
//				answer = ARRAY_TYPE;
//			}
//		}
////      return  SCALAR_TYPE;
//		if(answer != oldanswer){
//			String oldtype = oldanswer==ARRAY_TYPE?"Array":"Scalar";
//			System.out.println("Diff in function "+functionMetaData.getName()+";parameter "+(index+1)+" was "+ oldtype);
//		}
		return oldanswer;
	}

	/**
	 * Prepare empty template, which will keep result of evaluation
	 *   A few arguments of function may be array.
	 *   In this case result array will have  dimension as  such arguments union(not intersection).
	 *   Union provides correct error setting, while arrays have no concordant dimensions .
	 *   This method calculates result's dimension and prepare empty result's holder
	 *
	 *
	 * @param function
	 * @param args
	 * @param arrayFormula
	 * @return <code>null</code> if function returns a scalar result
	 */

	public static ArrayEval prepareEmptyResult(Function function, FunctionMetadata functionMetaData,ValueEval[] args, boolean arrayFormula) {
		boolean foundArrayArgThatNeedIterated = false;
		boolean criticalError = false;
		int rowCount = Integer.MIN_VALUE;
		int colCount = Integer.MIN_VALUE;
		byte aggregationError = 0;

		for (int i = 0; i < args.length; i++) {
			int argRowCount = Integer.MIN_VALUE;
			int argColCount = Integer.MIN_VALUE;
			ValueEval arg = args[i];
			if (getParameterType(function,functionMetaData, i) == SCALAR_TYPE) {
				if (arg instanceof ArrayEval) {
					ArrayEval aa = (ArrayEval) arg;
					argRowCount = aa.getHeight();
					argColCount = aa.getWidth();
					if(aa.getComponentError()!= 0)
						aggregationError = aa.getComponentError();
					else{
						if(isArrayArgContainsRef(aa))
							aggregationError = ErrorConstants.ERROR_VALUE;
					}
						
				} else if (arg instanceof AreaEval && arrayFormula) {
					AreaEval aa = (AreaEval) arg;
					argRowCount = aa.getHeight();
					argColCount = aa.getWidth();
				} else {
					continue; // Arguments is not array - just skip it
				}
				foundArrayArgThatNeedIterated = true;
				rowCount = Math.max(rowCount, argRowCount);
				colCount = Math.max(colCount, argColCount);
			}
			else {  // function accepts  Array
				
				if (arg instanceof ArrayEval) { //  Argument already array  
					ArrayEval aa = (ArrayEval) arg;
					boolean thisArrayRequreIteration = false;
					  if(aa.getComponentError() !=0)
					  {
						  // This error situation - in this case we don't need to invoke function - simply fill returned array by error code, taking from  from parameters
						 criticalError = true;
						 thisArrayRequreIteration = true;
						 aggregationError = aa.getComponentError();
					  }
					
					if(isArrayArgContainsRef(aa)){
						// Element is reference  that means that in future results will be "aggregate" only once							
						thisArrayRequreIteration = true;
						aggregationError = ErrorConstants.ERROR_NA;
					}
					if(thisArrayRequreIteration){
						foundArrayArgThatNeedIterated = true;
						argRowCount = aa.getHeight();
						argColCount = aa.getWidth();
						rowCount = Math.max(rowCount, argRowCount);
						colCount = Math.max(colCount, argColCount);
					}
				  
				}   // ArraEval
			}   // Array type of argument
		} // by arguments
		if (!foundArrayArgThatNeedIterated) {
			return null;
		}
		
		if(criticalError){
			ErrorEval[][] errorArray = new ErrorEval[rowCount][colCount];
			for(int row=0;row<rowCount;row++){
				for(int col=0;col<colCount;col++){
					errorArray[row][col] = ErrorEval.valueOf(aggregationError);
					}
				}
			return  new ArrayEval(errorArray);
		}

		ValueEval[][] emptyArray = new ValueEval[rowCount][colCount];
		ArrayEval answer =  new ArrayEval(emptyArray);
		if(aggregationError != 0)
			answer.setComponentError(aggregationError);
		return answer;
	}
	
/**
 * Does array contain any reference ?
 *  Some Excel function change it's behaviour when  it "array" argument contains reference 
 * @param aa
 * @return
 */
private static boolean isArrayArgContainsRef(ArrayEval aa){
		
	for(int j=0;j<aa.getHeight();j++){
		for(int jj=0;jj<aa.getWidth();jj++){
			ValueEval elem = aa.getValue(j,jj);
			if(elem instanceof AreaEval || elem instanceof RefEval ){
				// Element is reference  that means that in future results will be "aggregate" only once
				return true;
				}
			}
		}
	return false;
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
	public static ValueEval[] prepareArgsForLoop(Function function,FunctionMetadata functionMetaData, ValueEval[] args, int i, int j,boolean arrayFormula  ) {
		ValueEval[] answer = new ValueEval[args.length];
		for (int argIn = 0; argIn < args.length; argIn++) {
			ValueEval arg = args[argIn];

			if (getParameterType(function,functionMetaData, argIn) == SCALAR_TYPE ) {
				if (arg instanceof TwoDEval) {
					arg = getArrayValue((TwoDEval) arg, i, j);
				}
			}
			else{
				if (arg instanceof ArrayEval) {
					ValueEval elem = getArrayValue((TwoDEval) arg, i, j);
					if(elem instanceof AreaEval){
						arg = WorkbookEvaluator.dereferenceValue(elem, ((AreaEval)elem).getFirstRow()+i, ((AreaEval)elem).getFirstColumn()+j);
					}
					else if(elem instanceof RefEval){
						arg = WorkbookEvaluator.dereferenceValue(elem, 0, 0);
					}
//				System.out.println("Function "+functionMetaData.getName()+" got "+ (argIn+1)+" parameter as array");
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
	public static boolean checkForArrays(Function function,FunctionMetadata functionMetaData, ValueEval[] ops) {

		for (int i = 0; i < ops.length; i++) {
			if ((ops[i] instanceof ArrayEval) && (getParameterType(function,functionMetaData, i) == SCALAR_TYPE)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Transfer component error (if any) from argument to result 
	 * @param to
	 * @param ops
	 * @return
	 */
	public static ValueEval transferComponentError(ValueEval to, ValueEval[] ops) {
		ValueEval result = to;
		if(result instanceof ArrayEval){
			for(int i=0;i<ops.length;i++){
				if(ops[i] instanceof ArrayEval){
					if(((ArrayEval)ops[i]).getComponentError()!= 0) {
						((ArrayEval)result).setComponentError(((ArrayEval)ops[i]).getComponentError());
						return result;
					}
				}
			}	
		}
		return result;
	}
}
