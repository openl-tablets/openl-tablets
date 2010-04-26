package org.openl.tablets.tutorial10.domain;

public class Address {
    private Country country;
    private String region;

    public Address() {

    }

    public Address(Country country, String region) {
        this.country = country;
        this.region = region;
    }

    /**
     * @return the country
     */
    public Country getCountry() {
        return country;
    }

    /**
     * @param country the country to set
     */
    public void setCountry(Country country) {
        this.country = country;
    }

    /**
     * @return the region
     */
    public String getRegion() {
        return region;
    }

    /**
     * @param region the region to set
     */
    public void setRegion(String region) {
        this.region = region;
    }
}
