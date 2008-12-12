/**
 *  OpenL Tablets,  2006
 *  https://sourceforge.net/projects/openl-tablets/ 
 */
package org.openl.tablets.tutorial3;

/**
 * @author snshor
 *
 */
public class ZipCode {
	
	String zip1; // 5-digit part - mandatory
	String zip2; // 4-digit part - optional
	/**
	 * @return Returns the zip1.
	 */
	public String getZip1() {
		return zip1;
	}
	/**
	 * @param zip1 The zip1 to set.
	 */
	public void setZip1(String zip1) {
		this.zip1 = zip1;
	}
	/**
	 * @return Returns the zip2.
	 */
	public String getZip2() {
		return zip2;
	}
	/**
	 * @param zip2 The zip2 to set.
	 */
	public void setZip2(String zip2) {
		this.zip2 = zip2;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() 
	{
		if (zip2 == null)
			return zip1;
		return zip1 + '-' + zip2;
	}
	
	

}
