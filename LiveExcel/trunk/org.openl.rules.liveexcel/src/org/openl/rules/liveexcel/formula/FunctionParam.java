package org.openl.rules.liveexcel.formula;

import org.apache.poi.hssf.record.formula.eval.RefEval;

/**
 * FunctionParam - handles parameters pairs from function definition
 * 
 */
public class FunctionParam {

    private String paramName;
    private RefEval paramCell;

    public FunctionParam(String paramName, RefEval paramCell) {
        this.paramName = paramName;
        this.paramCell = paramCell;
    }

    public String getParamName() {
        return paramName;
    }

    public void setParamName(String paramName) {
        this.paramName = paramName;
    }

    public RefEval getParamCell() {
        return paramCell;
    }

    public void setParamCell(RefEval paramCell) {
        this.paramCell = paramCell;
    }

}
