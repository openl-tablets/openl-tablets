package org.openl.rules.calculation.result.convertor2;

/*
 * #%L
 * OpenL - DEV - Rules - Calculation Result
 * %%
 * Copyright (C) 2013 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */

import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Spreadsheet row(step) that has nested SpreadsheetResult or SpreadsheetResult[] value for one of the columns.
 *
 * @author DLiauchuk
 */

@XmlRootElement
@Deprecated
public class CompoundStep extends CodeStep {

    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    private List<CalculationStep> steps = new ArrayList<>();

    /**
     * Add a step
     *
     * @param step
     */
    public void addStep(CalculationStep step) {
        if (step != null) {
            steps.add(step);
        }
    }

    /**
     * Returns steps
     *
     * @return
     */
    public List<CalculationStep> getSteps() {
        return new ArrayList<>(steps);
    }

    /**
     * Sets steps
     *
     * @param steps
     */
    public void setSteps(List<CalculationStep> steps) {
        this.steps = new ArrayList<>(steps);
    }

}
