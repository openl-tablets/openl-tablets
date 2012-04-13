/**
 * 
 */
package com.exigen.le.democase;

import com.exigen.le.smodel.Property;
import com.exigen.le.smodel.Type;

/**
 * @author vabramovs
 *
 */
public class Coverage {
	String coverageCode;
	Double dedactibleAmount;
	/**
	 * @return the coverageCode
	 */
	public String getCoverageCode() {
		return coverageCode;
	}
	/**
	 * @param coverageCode the coverageCode to set
	 */
	public void setCoverageCode(String coverageCode) {
		this.coverageCode = coverageCode;
	}
	/**
	 * @return the dedactibleAmount
	 */
	public Double getDedactibleAmount() {
		return dedactibleAmount;
	}
	/**
	 * @param dedactibleAmount the dedactibleAmount to set
	 */
	public void setDedactibleAmount(Double dedactibleAmount) {
		this.dedactibleAmount = dedactibleAmount;
	}

}
