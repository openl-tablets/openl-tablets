package org.openl.rules.liveexcel.formula;

import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hssf.record.formula.RefPtg;

/**
* ParsedDeclaredFunction  - handles parsed ol_declare_function
* 
*/
public class ParsedDeclaredFunction {
    
    private String declFuncName;
    private String description;
    private RefPtg returnCell;
    private List<FunctionParam> parameters = new ArrayList<FunctionParam>();
    
    public String getDeclFuncName() {
        return declFuncName;
    }
    
    public void setDeclFuncName(String declFuncName) {
        this.declFuncName = declFuncName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public RefPtg getReturnCell() {
        return returnCell;
    }
    
    public void setReturnCell(RefPtg returnCell) {
        this.returnCell = returnCell;
    }
    
    public List<FunctionParam> getParameters() {
        return parameters;
    }
    
    public void setParameters(List<FunctionParam> parameters) {
        this.parameters = parameters;
    }
}
