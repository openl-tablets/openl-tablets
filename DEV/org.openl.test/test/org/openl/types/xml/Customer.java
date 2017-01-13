/*
 * Created on Jul 15, 2003
 *
 */
package org.openl.types.xml;

/**
 * @author Jacob
 *
 */
public class Customer {

    protected String name;
    protected String maritalStatus;
    protected String gender;

    Customer spouse;

    public Customer() {
    }

    /**
     * @return
     */
    public String getGender() {
        return gender;
    }

    /**
     * @return
     */
    public String getMaritalStatus() {
        return maritalStatus;
    }

    /**
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * @return
     */
    public Customer getSpouse() {
        return spouse;
    }

    /**
     * @param string
     */
    public void setGender(String string) {
        gender = string;
    }

    /**
     * @param string
     */
    public void setMaritalStatus(String string) {
        maritalStatus = string;
    }

    /**
     * @param string
     */
    public void setName(String string) {
        name = string;
    }

    /**
     * @param customer
     */
    public void setSpouse(Customer customer) {
        spouse = customer;
    }

}
