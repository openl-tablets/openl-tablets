package org.openl.generated.beans;

import java.util.Date;
import java.util.Objects;

public class Driver {

    protected String gender;
    protected Date birthDate;

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Date getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(Date birthDate) {
        this.birthDate = birthDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Driver driver = (Driver) o;
        return Objects.equals(gender, driver.gender) &&
                Objects.equals(birthDate, driver.birthDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gender, birthDate);
    }

    @Override
    public String toString() {
        return "Driver{" +
                "gender='" + gender + '\'' +
                ", birthDate=" + birthDate +
                '}';
    }
}
