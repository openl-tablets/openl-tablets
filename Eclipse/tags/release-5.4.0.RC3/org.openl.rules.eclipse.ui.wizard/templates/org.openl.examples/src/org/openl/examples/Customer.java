/*
 * Created on Sep 10, 2003
 *
 * Developed by OpenRules Inc. 2003-2004
 */
package org.openl.examples;

/**
 * @author Jacob
 *
 * A simple Java class Customer to be greeted from HelloCustomer.openl
 */
public class Customer 
{
	protected String name;
	protected String maritalStatus;
	protected String gender;
	protected Customer spouse;

	public String getGender() {
		return gender;
	}

	public String getMaritalStatus() {
		return maritalStatus;
	}

	public String getName() {
		return name;
	}

	public Customer getSpouse() {
		return spouse;
	}

	public void setGender(String string) {
		gender = string;
	}
	public void setMaritalStatus(String string) {
		maritalStatus = string;
	}

	public void setName(String string) {
		name = string;
	}

	public void setSpouse(Customer customer) {
		spouse = customer;
	}

}
