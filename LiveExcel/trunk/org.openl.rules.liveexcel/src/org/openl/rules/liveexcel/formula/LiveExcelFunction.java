package org.openl.rules.liveexcel.formula;

import org.apache.poi.hssf.record.formula.eval.Eval;
import org.apache.poi.hssf.record.formula.eval.RefEval;
import org.apache.poi.hssf.record.formula.functions.FreeRefFunction;

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

    public void prepareArguments(Eval[] args) {
        for (Eval eval : args) {
            if (eval instanceof RefEval) {
                eval = ((RefEval) eval).getInnerValueEval();
            }
        }
    }
}
