package org.openl.rules.liveexcel.formula;

import org.apache.poi.ss.usermodel.Sheet;

public class DeclaredFunction {
    private Sheet sheet;
    private String cellAdress;    
    private String functionText;
    
    public DeclaredFunction(Sheet sheet, String cellAdress, String functionText) {
        this.cellAdress = cellAdress;
        this.sheet = sheet;
        this.functionText = functionText;
    }

    public Sheet getSheet() {
        return sheet;
    }

    public void setSheet(Sheet sheet) {
        this.sheet = sheet;
    }

    public String getCellAdress() {
        return cellAdress;
    }

    public void setCellAdress(String cellAdress) {
        this.cellAdress = cellAdress;
    }

    public String getFunctionText() {
        return functionText;
    }

    public void setFunctionText(String functionText) {
        this.functionText = functionText;
    }    
    
}
