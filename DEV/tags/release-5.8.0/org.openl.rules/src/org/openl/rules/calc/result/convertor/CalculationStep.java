package org.openl.rules.calc.result.convertor;

/**
 * The base abstraction for the spreadsheet step, each step is a spreadsheet row.
 *
 */
public class CalculationStep {
	
    /** step name*/
	private String stepName;

	public String getStepName() {
		return stepName;
	}

	public void setStepName(String stepName) {
		this.stepName = stepName;
	}

}
