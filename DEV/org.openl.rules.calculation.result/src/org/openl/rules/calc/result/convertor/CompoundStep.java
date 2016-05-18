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


import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;


/**
 * Spreadsheet row(step) that has nested SpreadsheetResult or SpreadsheetResult[] value for one of the columns.
 * 
 * @author DLiauchuk
 *
 */
@Deprecated
@XmlRootElement
public class CompoundStep extends CodeStep {
	
    private static final long serialVersionUID = -7072660266573768559L;
    /**
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
