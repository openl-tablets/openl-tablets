package org.openl.rules.liveexcel.formula;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.record.formula.eval.ErrorEval;
import org.apache.poi.hssf.record.formula.eval.Eval;
import org.apache.poi.hssf.record.formula.eval.RefEval;
import org.apache.poi.hssf.record.formula.eval.ValueEval;
import org.apache.poi.hssf.record.formula.functions.FreeRefFunction;
import org.apache.poi.ss.formula.EvaluationWorkbook;

/**
 * Common class for LiveExcel functions.
 * 
 * @author PUdalau
 */
public abstract class LiveExcelFunction implements FreeRefFunction {
    private static final Log log = LogFactory.getLog(LiveExcelFunction.class);
    protected String declFuncName;

    /**
     * @return Name of function.
     */
    public String getDeclFuncName() {
        return declFuncName;
    }

    /**
     * Sets name of function.
     * 
     * @param declFuncName Name of function.
     */
    public void setDeclFuncName(String declFuncName) {
        this.declFuncName = declFuncName;
    }

    public ValueEval evaluate(Eval[] args, EvaluationWorkbook workbook, int srcCellSheet, int srcCellRow, int srcCellCol) {
        Eval[] processedArgs = prepareArguments(args);
        log.info("Execution of function [" + declFuncName + "]started. Arguments: " + printArguments(processedArgs));
        ValueEval result = execute(processedArgs, workbook, srcCellSheet, srcCellRow, srcCellCol);
        if (result instanceof ErrorEval) {
            if (result == ErrorEval.VALUE_INVALID) {
                log.error("Wrong arguments for function [" + declFuncName + "]");
            } else if (result == ErrorEval.NA) {
                log.warn("No output available from function [" + declFuncName + "]");
            } else {
                log.error("Error in function [" + declFuncName + "]" + "Error message :"
                        + ErrorEval.getText(((ErrorEval) result).getErrorCode()));
            }
        } else {
            log.info("Result of function [" + declFuncName + "] : " + result);
        }
        return result;
    }

    public Eval[] prepareArguments(Eval[] args) {
        Eval[] result = args.clone();
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof RefEval) {
                result[i] = ((RefEval) args[i]).getInnerValueEval();
            } else {
                result[i] = args[i];
            }
        }
        return result;
    }

    private static String printArguments(Eval[] args) {
        StringBuffer buffer = new StringBuffer("[");
        for (Eval argument : args) {
            buffer.append(argument);
            buffer.append(';');
        }
        buffer.append(']');
        return buffer.toString();
    }

    protected abstract ValueEval execute(Eval[] args, EvaluationWorkbook workbook, int srcCellSheet, int srcCellRow,
            int srcCellCol);
}
