/**
 *  OpenL Tablets,  2006
 *  https://sourceforge.net/projects/openl-tablets/ 
 */
package org.openl.tablets.tutorial3;

/**
 * @author snshor
 *
 */
public class Address {
	String street1;
	String street2;
	String city;
	ZipCode zip;
	USState state;
	/**
	 * @return Returns the city.
	 */
	public String getCity() {
		return city;
	}
	/**
	 * @param city The city to set.
	 */
	public void setCity(String city) {
		this.city = city;
	}
	/**
	 * @return Returns the state.
	 */
	public USState getState() {
		return state;
	}
	/**
	 * @param state The state to set.
	 */
	public void setState(USState state) {
		this.state = state;
	}
	/**
	 * @return Returns the street1.
	 */
	public String getStreet1() {
		return street1;
	}
	/**
	 * @param street1 The street1 to set.
	 */
	public void setStreet1(String street1) {
		this.street1 = street1;
	}
	/**
	 * @return Returns the street2.
	 */
	public String getStreet2() {
		return street2;
	}
	/**
	 * @param street2 The street2 to set.
	 */
	public void setStreet2(String street2) {
		this.street2 = street2;
	}
	/**
	 * @return Returns the zip.
	 */
	public ZipCode getZip() {
		return zip;
	}
	/**
	 * @param zip The zip to set.
	 */
	public void setZip(ZipCode zip) {
		this.zip = zip;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		
		String res = street1 + '\n';
		if (street2 != null)
			res += street2 + '\n';
		return res + city + ", " + state.getCode() + ' ' + zip;
		
	}
	
	

}
