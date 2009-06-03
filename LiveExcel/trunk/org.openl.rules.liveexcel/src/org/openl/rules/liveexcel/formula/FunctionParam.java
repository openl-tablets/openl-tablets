package org.openl.rules.liveexcel.formula;

import org.apache.poi.hssf.record.formula.RefPtg;

/**
* FunctionParam  - handles parameters pairs from function definition
* 
*/
public class FunctionParam {
    
    private String paramName;
    private RefPtg paramCell;
    
    
    public FunctionParam(String paramName, RefPtg paramCell) {        
        this.paramName = paramName;
        this.paramCell = paramCell;
    }

    public String getParamName() {
        return paramName;
    }
    
    public void setParamName(String paramName) {
        this.paramName = paramName;
    }
    
    public RefPtg getParamCell() {
        return paramCell;
    }
    
    public void setParamCell(RefPtg paramCell) {
        this.paramCell = paramCell;
    }

}
