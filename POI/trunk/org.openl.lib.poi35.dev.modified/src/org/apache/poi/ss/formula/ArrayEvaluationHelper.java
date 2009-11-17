/**
 * 
 */
package org.apache.poi.ss.formula;

import org.apache.poi.hssf.record.formula.eval.AreaEval;
import org.apache.poi.hssf.record.formula.eval.ValueEval;
import org.apache.poi.hssf.record.formula.functions.ArrayMode;
import org.apache.poi.hssf.record.formula.functions.Function;
import org.apache.poi.hssf.record.formula.functions.FunctionWithArraySupport;

/**
 * Helper class to manipulate with array formula
 * 
 * @author vabramovs
 * 
 */
public class ArrayEvaluationHelper {

    final static int SCALAR_TYPE = 0;
    final static int ARRAY_TYPE = 1;

    // Has no instance
    private ArrayEvaluationHelper() {
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
     * 
     * @param function
     * @param ops
     * @param arrayFormula
     * @return
     */
    public static ValueEval prepareEmptyResult(Function function, ValueEval[] ops, boolean arrayFormula) {
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
     * 
     * @param function
     * @param ops
     * @param i
     * @param j
     * @param trackAreas
     * @return
     */
    public static ValueEval[] prepareArg4Loop(Function function, ValueEval[] ops, int i, int j, boolean trackAreas) {
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

    /**
     * Can operation will be done in Array Mode
     * 
     * @param function
     * @return
     */
    public static boolean specialModeForArray(Function function) {
        if (function instanceof ArrayMode) {
            return true;
        }
        return false;
    }

}
