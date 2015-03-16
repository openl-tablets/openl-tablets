package org.openl.rules.calc.result.convertor;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;


/**
 * Spreadsheet step(row) that has the code value.
 * 
 * @author DLiauchuk
 *
 */
@Deprecated
@XmlRootElement
@XmlSeeAlso({SimpleStep.class, CompoundStep.class})
public class CodeStep extends CalculationStep {
    
    private static final long serialVersionUID = 7372598798002605558L;
    
    private String code;
    
    public String getCode() {
        return code;
    }
    public void setCode(String code) {
        this.code = code;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Step_Name: ").append(getStepName()).append(" Code: ").append(getCode());
        return sb.toString();
    }
}
