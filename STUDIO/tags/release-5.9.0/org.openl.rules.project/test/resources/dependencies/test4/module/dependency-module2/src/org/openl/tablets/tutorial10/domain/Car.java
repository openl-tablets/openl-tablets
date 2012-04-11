/**
 * 
 */
package org.openl.tablets.tutorial10.domain;

/**
 *
 */
public class Car {
    private CarBrand brand;
    private String model;

    public Car() {

    }

    public Car(CarBrand brand, String model) {
        this.brand = brand;
        this.model = model;
    }

    /**
     * @return the brand
     */
    public CarBrand getBrand() {
        return brand;
    }

    /**
     * @param brand the brand to set
     */
    public void setBrand(CarBrand brand) {
        this.brand = brand;
    }

    /**
     * @return the model
     */
    public String getModel() {
        return model;
    }

    /**
     * @param model the model to set
     */
    public void setModel(String model) {
        this.model = model;
    }
}
