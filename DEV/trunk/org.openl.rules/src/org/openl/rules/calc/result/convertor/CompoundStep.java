package org.openl.rules.calc.result.convertor;

import java.util.ArrayList;
import java.util.List;


/**
 * Spreadsheet row(step) that has nested SpreadsheetResult or SpreadsheetResult[] value for one of the columns.
 * 
 * @author DLiauchuk
 *
 */
public class CompoundStep extends CodeStep {
    
    private List<CodeStep> steps = new ArrayList<CodeStep>();
    
    public CompoundStep() {}
    
    public void addStep(CodeStep step) {
        if (step != null) {
            steps.add(step);
        }
    }

    public List<CodeStep> getSteps() {
        return new ArrayList<CodeStep>(steps);
    }

    public void setSteps(List<CodeStep> steps) {
        this.steps = new ArrayList<CodeStep>(steps);
    }
    
    
}
