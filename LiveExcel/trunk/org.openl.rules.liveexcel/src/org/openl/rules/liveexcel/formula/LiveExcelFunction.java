package org.openl.rules.liveexcel.formula;

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
        return execute(prepareArguments(args), workbook, srcCellSheet, srcCellRow, srcCellCol);
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

    protected abstract ValueEval execute(Eval[] args, EvaluationWorkbook workbook, int srcCellSheet, int srcCellRow,
            int srcCellCol);
}
