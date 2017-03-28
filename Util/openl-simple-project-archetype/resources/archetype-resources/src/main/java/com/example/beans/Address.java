package com.example.beans;

import com.example.beans.openl.Auto;

/**
 * This class is an external bean for OpenL rules.
 */
public class Address {

    private String street;
    private String building;
    private Auto auto;

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

    public void setAuto(Auto auto) {
        this.auto = auto;
    }

    public Auto getAuto() {
        return auto;
    }
}
