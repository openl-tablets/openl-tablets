package org.openl.rules.calculation.result.convertor;

import java.io.Serializable;

/**
 * The base abstraction for the spreadsheet step, each step is a spreadsheet
 * row.
 * 
 */
public class CalculationStep implements Serializable {

    private static final long serialVersionUID = 4067908093788043935L;

    /** step name */
    private String stepName;

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

}
