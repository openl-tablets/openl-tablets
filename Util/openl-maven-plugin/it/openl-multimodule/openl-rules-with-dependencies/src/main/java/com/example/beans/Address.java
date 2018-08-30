package com.example.beans;

/**
 * This class is an external bean for OpenL rules.
 */
public class Address {

    private String street;
    private String building;

    public void setStreet(String street) {
        this.street = street;
    }

    public String getStreet() {
        return street;
    }

    public void setBuilding(String building) {
        this.building = building;
    }

    public String getBuilding() {
        return building;
    }
}
