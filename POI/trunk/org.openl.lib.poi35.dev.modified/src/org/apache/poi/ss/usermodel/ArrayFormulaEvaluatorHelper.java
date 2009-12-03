/**
 * 
 */
package org.apache.poi.ss.usermodel;

import org.apache.poi.hssf.record.formula.eval.AreaEval;
import org.apache.poi.hssf.record.formula.eval.BoolEval;
import org.apache.poi.hssf.record.formula.eval.ErrorEval;
import org.apache.poi.hssf.record.formula.eval.NumberEval;
import org.apache.poi.hssf.record.formula.eval.StringEval;
import org.apache.poi.hssf.record.formula.eval.ValueEval;
import org.apache.poi.hssf.record.formula.functions.Function;
import org.apache.poi.hssf.record.formula.functions.FunctionBase;
import org.apache.poi.hssf.record.formula.functions.FunctionWithArraySupport;
import org.apache.poi.ss.formula.ArrayEval;
import org.apache.poi.ss.util.CellRangeAddress;

/**
 * Helper class to manipulate with array formula
 * This class contains methods, common for HSSF and XSSF FormulaEvaluator
 * Better solution - to have abstract class for FormulaEvaluator Interface Implementation
 *  All method need to be static 
 * @author Vladimirs Abramovs(VIA) (Vladimirs.Abramovs at exigenservices.com)
 *
 */
public class ArrayFormulaEvaluatorHelper {
    final static int SCALAR_TYPE = 0;
    final static int ARRAY_TYPE = 1;

	
	private ArrayFormulaEvaluatorHelper (){
		// has no instance
	}
	
	
	/** Transform evaluated Array Values according aimed Range
	 *   Depending on dimensions correlation, 
	 *   result array may be restricted 
	 *    or his single row/column will cloned 
	 *    or some cell will set to #N/A 
	 * @param cvs
	 * @param range
	 * @return
	 */
	public static Object[][] transformToRange(Object[][] cvs,CellRangeAddress range){
		
		Object[][] answer = null;
		if(cvs[0][0] instanceof CellValue)
			answer = new CellValue[range.getLastRow()-range.getFirstRow()+1][range.getLastColumn()-range.getFirstColumn()+1];
		else if (cvs[0][0] instanceof ValueEval)
			answer = new ValueEval[range.getLastRow()-range.getFirstRow()+1][range.getLastColumn()-range.getFirstColumn()+1];
		else
			throw new RuntimeException("transform2Range does not support type "+cvs[0][0].getClass().getName());
		int rowStart = range.getFirstRow();
		int colStart = range.getFirstColumn();
		for(int i=rowStart;i<=range.getLastRow();i++ )
			for(int j=colStart; j<=range.getLastColumn();j++)
			{
				if((i-rowStart)<cvs.length && (j-colStart)<cvs[i-rowStart].length){
					answer[i-rowStart][j-colStart] = cvs[i-rowStart][j-colStart];
				}
				else
				{  
					boolean needClone = false;
					int cloneRow =  0;
					int cloneCol = 0;
					if(cvs.length == 1)
					{  // Need to clone first colm of  cvs
						cloneCol = j-colStart;
						needClone = true;
						
					}
					if(cvs[0].length == 1 )
					{  // Need to clone first row of  cvs
						cloneRow = i-rowStart;
						needClone = true;
						
					}
					if(needClone &&  cloneCol <cvs[0].length && cloneRow <cvs.length) 
					{
						
						answer[i-rowStart][j-colStart] = cvs[cloneRow][cloneCol];
					}	
					else 
					{
						//  For other cases set cell value to #N/A
						// For those cells we changes also their type to Error
						if(cvs[0][0] instanceof CellValue){
								CellValue cvError = CellValue.getError(org.apache.poi.ss.usermodel.ErrorConstants.ERROR_NA);
								answer[i-rowStart][j-colStart] = cvError;
						}	
						else 
							 if (cvs[0][0] instanceof ValueEval)
								 answer[i-rowStart][j-colStart] = ErrorEval.NA;
					}
				}	
			}
		return answer;
	}
	/**
	 * Convert Eval value to CellValue
	 * @param val
	 * @return
	 */
	public static CellValue eval2Cell(ValueEval val){
		if(val instanceof BoolEval)
			return CellValue.valueOf(((BoolEval)val).getBooleanValue());
		if(val instanceof NumberEval)
			return  new CellValue(((NumberEval)val).getNumberValue());
		if(val instanceof StringEval)
			return  new CellValue(((StringEval)val).getStringValue());
		if(val instanceof ErrorEval)
			return  new CellValue(((ErrorEval)val).getErrorCode());
		return new CellValue(ErrorEval.VALUE_INVALID.getErrorCode());
	}
	/**
	 * Get single value from ArrayEval  for desired cell 
	 * @param evaluationResult
	 * @param cell
	 * @return
	 */
	public static ValueEval dereferenceValue(ArrayEval evaluationResult, Cell cell) {
		CellRangeAddress range = cell.getArrayFormulaRange();
		Object[][] rangeVal = ArrayFormulaEvaluatorHelper.transformToRange(evaluationResult.getArrayValues(),range);
		int rowInArray = cell.getRowIndex()- range.getFirstRow();
		int colInArray = cell.getColumnIndex() - range.getFirstColumn();
		return  (ValueEval)rangeVal[rowInArray][colInArray];
	}
    /**
     * Get type of parameter (SCALAR_TYPE or ARRAY_TYPE) which support function
     * for given argument
     * 
     * @param function
     * @param argIndex
     * @return
     */
    public static int getParameterType(FunctionBase function, int argIndex) {
        int answer = SCALAR_TYPE;

        if (function instanceof FunctionWithArraySupport) {
            // ask new interface(ZS) for argument type
            if (((FunctionWithArraySupport) function).supportArray(argIndex))
                answer = ARRAY_TYPE;
        }
        return answer;
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
     * @param ops
     * @param arrayFormula
     * @return
     */
    public static ValueEval prepareEmptyResult(FunctionBase function, ValueEval[] ops, boolean arrayFormula) {
        int rowCount = Integer.MAX_VALUE;
        int colCount = Integer.MAX_VALUE;

        boolean illegalForAggregation = false;

        for (int i = 0; i < ops.length; i++) {
            int argRowCount = Integer.MAX_VALUE;
            int argColCount = Integer.MAX_VALUE;
            if (getParameterType(function, i) == SCALAR_TYPE) {
                if (ops[i] instanceof ArrayEval) {
                    argRowCount = ((ArrayEval) ops[i]).getRowCounter();
                    argColCount = ((ArrayEval) ops[i]).getColCounter();
                    illegalForAggregation = illegalForAggregation || ((ArrayEval) ops[i]).isIllegalForAggregation();
                } else if (ops[i] instanceof AreaEval && arrayFormula) {
                    argRowCount = ((AreaEval) ops[i]).getHeight();
                    argColCount = ((AreaEval) ops[i]).getWidth();
                } else
                    continue; // Arguments is not array - just skip it
                if (argRowCount != rowCount) {
                    if (rowCount != Integer.MAX_VALUE) {
                        illegalForAggregation = true;
                    }
                    rowCount = Math.min(rowCount, argRowCount);
                }
                if (argColCount != colCount) {
                    if (colCount != Integer.MAX_VALUE) {
                        illegalForAggregation = true;
                    }
                    colCount = Math.min(colCount, argColCount);
                }
            }
        }

        if (colCount == Integer.MAX_VALUE || rowCount == Integer.MAX_VALUE)
            return null;

        ValueEval[][] emptyArray = new ValueEval[rowCount][colCount];
        ValueEval answer = new ArrayEval(emptyArray);

        ((ArrayEval) answer).setIllegalForAggregation(illegalForAggregation);
        return answer;
    }

    /**
     * Prepare arguments for next iteration to call function
     *  Each argument may be scalar, full array or element(according iteration) of array
     * 
     * @param function
     * @param ops
     * @param i
     * @param j
     * @param trackAreas
     * @return
     */
    public static ValueEval[] prepareArgsForLoop(FunctionBase function, ValueEval[] ops, int i, int j, boolean trackAreas) {
        ValueEval[] answer = new ValueEval[ops.length];
        for (int argIn = 0; argIn < ops.length; argIn++) {
            if (getParameterType(function, argIn) == SCALAR_TYPE) {
                if (ops[argIn] instanceof ArrayEval) {
                    answer[argIn] = ((ArrayEval) ops[argIn]).getArrayElementAsEval(i, j);
                } else if (ops[argIn] instanceof AreaEval && trackAreas) {
                    answer[argIn] = ((AreaEval) ops[argIn]).getRelativeValue(i, j);

                } else {
                    answer[argIn] = ops[argIn];
                }
            } else { // Array type
                answer[argIn] = ops[argIn];

            }

        }

        return answer;
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
            if ((ops[i] instanceof ArrayEval) && (getParameterType(function, i) == SCALAR_TYPE))
                return true;
        }
        return false;

    }

}
