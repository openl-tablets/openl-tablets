package org.openl.rules.calc.result.convertor;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;

/**
 * The base abstraction for the spreadsheet step, each step is a spreadsheet row.
 *
 */
@Deprecated
@XmlRootElement
@XmlSeeAlso({CodeStep.class})
public class CalculationStep implements Serializable{
	
    private static final long serialVersionUID = 4067908093788043935L;
    
    /** step name*/
	private String stepName;

	public String getStepName() {
		return stepName;
	}

	public void setStepName(String stepName) {
		this.stepName = stepName;
	}

}
