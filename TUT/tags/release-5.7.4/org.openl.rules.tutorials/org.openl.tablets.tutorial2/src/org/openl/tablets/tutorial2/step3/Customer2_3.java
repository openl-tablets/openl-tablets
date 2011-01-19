/**
 *  OpenL Tablets,  2006
 *  https://sourceforge.net/projects/openl-tablets/ 
 */
package org.openl.tablets.tutorial2.step3;

import java.util.Date;

/**
 * @author snshor
 *
 */
public class Customer2_3 
{
	String name;
	String ssn;
	Date dob;
	String billing;
	String shipping;
	/**
	 * @return Returns the billing.
	 */
	public String getBilling() {
		return billing;
	}
	/**
	 * @param billing The billing to set.
	 */
	public void setBilling(String billing) {
		this.billing = billing;
	}
	/**
	 * @return Returns the dob.
	 */
	public Date getDob() {
		return dob;
	}
	/**
	 * @param dob The dob to set.
	 */
	public void setDob(Date dob) {
		this.dob = dob;
	}
	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name The name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return Returns the shipping.
	 */
	public String getShipping() {
		return shipping;
	}
	/**
	 * @param shipping The shipping to set.
	 */
	public void setShipping(String shipping) {
		this.shipping = shipping;
	}
	/**
	 * @return Returns the ssn.
	 */
	public String getSsn() {
		return ssn;
	}
	/**
	 * @param ssn The ssn to set.
	 */
	public void setSsn(String ssn) {
		this.ssn = ssn;
	}
	
	
}
