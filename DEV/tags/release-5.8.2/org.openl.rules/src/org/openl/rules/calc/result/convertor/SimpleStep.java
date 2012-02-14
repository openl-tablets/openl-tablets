package org.openl.rules.calc.result.convertor;


/**
 * Spreadsheet row(step) that has formula and value as column values. 
 * 
 * @author DLiauchuk
 *
 */
public class SimpleStep extends CodeStep {
    
    private Double formula;
    private Double value;
    private String text;
    
    public Double getFormula() {
        return formula;
    }
    
    public void setFormula(Double formula) {
        this.formula = formula;
    }
    
    public Double getValue() {
        return value;
    }
    
    public void setValue(Double value) {
        this.value = value;
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
}
