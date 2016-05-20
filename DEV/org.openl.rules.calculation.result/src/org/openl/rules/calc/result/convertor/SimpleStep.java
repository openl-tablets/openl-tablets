package org.openl.rules.calc.result.convertor;

/*
 * #%L
 * OpenL - DEV - Rules - Calculation Result
 * %%
 * Copyright (C) 2015 - 2016 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */


import javax.xml.bind.annotation.XmlRootElement;


/**
 * Spreadsheet row(step) that has formula and value as column values. 
 * 
 * @author DLiauchuk
 *
 */
@Deprecated
@XmlRootElement
public class SimpleStep extends CodeStep {
    
    private static final long serialVersionUID = 3906469763279262117L;
    
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
