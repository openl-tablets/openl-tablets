package org.openl.rules.liveexcel.formula;

import org.apache.poi.hssf.record.formula.eval.RefEval;

/**
 * FunctionParam - handles parameters pairs from function definition
 * 
 */
public class FunctionParam {

    private String paramName;
    private RefEval paramCell;    
    private Class<?> paramType;    

    public Class<?> getParamType() {
        return paramType;
    }

    public void setParamType(Class<?> paramType) {
        this.paramType = paramType;
    }

    public FunctionParam(String paramName, RefEval paramCell) {
        this.paramName = paramName;
        this.paramCell = paramCell;
        paramType = Object.class;
    }

    public FunctionParam(String paramName, RefEval paramCell, Class<?> paramType) {
        this.paramName = paramName;
        this.paramCell = paramCell;
        this.paramType = paramType;
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
