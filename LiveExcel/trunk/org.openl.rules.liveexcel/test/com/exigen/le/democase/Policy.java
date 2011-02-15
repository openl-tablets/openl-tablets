/**
 * 
 */
package com.exigen.le.democase;

import java.util.Calendar;

import com.exigen.le.smodel.Property;
import com.exigen.le.smodel.Type;

/**
 * @author vabramovs
 *
 */
public class Policy {
	Calendar effectiveDate;
	Calendar expirationDate;
	Boolean multiplePolicyDiscount;
	/**
	 * @return the effectiveDate
	 */
	public Calendar getEffectiveDate() {
		return effectiveDate;
	}
	/**
	 * @param effectiveDate the effectiveDate to set
	 */
	public void setEffectiveDate(Calendar effectiveDate) {
		this.effectiveDate = effectiveDate;
	}
	/**
	 * @return the expirationDate
	 */
	public Calendar getExpirationDate() {
		return expirationDate;
	}
	/**
	 * @param expirationDate the expirationDate to set
	 */
	public void setExpirationDate(Calendar expirationDate) {
		this.expirationDate = expirationDate;
	}
	/**
	 * @return the multiplePolicyDiscount
	 */
	public Boolean getMultiplePolicyDiscount() {
		return multiplePolicyDiscount;
	}
	/**
	 * @param multiplePolicyDiscount the multiplePolicyDiscount to set
	 */
	public void setMultiplePolicyDiscount(Boolean multiplePolicyDiscount) {
		this.multiplePolicyDiscount = multiplePolicyDiscount;
	}

}
