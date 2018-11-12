package org.openl.generated.beans;

import java.util.Arrays;
import java.util.Objects;

public class Vehicle {

    protected String id;
    protected Driver driver;
    protected Coverage[] coverages;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Driver getDriver() {
        return driver;
    }

    public void setDriver(Driver driver) {
        this.driver = driver;
    }

    public Coverage[] getCoverages() {
        return coverages;
    }

    public void setCoverages(Coverage[] coverages) {
        this.coverages = coverages;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vehicle vehicle = (Vehicle) o;
        return Objects.equals(id, vehicle.id) &&
                Objects.equals(driver, vehicle.driver) &&
                Arrays.equals(coverages, vehicle.coverages);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(id, driver);
        result = 31 * result + Arrays.hashCode(coverages);
        return result;
    }

    @Override
    public String toString() {
        return "Vehicle{" +
                "id='" + id + '\'' +
                ", driver=" + driver +
                ", coverages=" + Arrays.toString(coverages) +
                '}';
    }
}
