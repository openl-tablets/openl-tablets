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

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlTransient;

/**
 * The base abstraction for the spreadsheet step, each step is a spreadsheet row.
 * 
 * @author DLiauchuk, Marat Kamalov
 */

@XmlRootElement
@XmlSeeAlso({ CodeStep.class })
public class CalculationStep implements Serializable {

    private static final long serialVersionUID = 4067908093788043935L;

    private Double formula;

    /** step name */
    private String stepName;

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public Double getFormula() {
        return formula;
    }

    public void setFormula(Double formula) {
        this.formula = formula;
    }

    @XmlTransient
    private ConvertationMetadata convertationMetadata = null;

    @XmlTransient
    public ConvertationMetadata getConvertationMetadata() {
        return convertationMetadata;
    }

    public void setConvertationMetadata(ConvertationMetadata convertationMetadata) {
        this.convertationMetadata = convertationMetadata;
    }
}
