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
	
	/** These 2 fields currently added for Chartis case.
	 * In future any customizations of {@link CompoundStep} and {@link SimpleStep}
	 * will be done by generated datatypes.
	 * And fields formula, and id will be removed. 
	 */
	private Double formula;
	private String id;

	public Double getFormula() {
		return formula;
	}

	public void setFormula(Double formula) {
		this.formula = formula;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
    
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
