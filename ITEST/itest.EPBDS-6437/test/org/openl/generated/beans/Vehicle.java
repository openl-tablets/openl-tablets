package org.openl.generated.beans;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(namespace = "http://beans.generated.openl.org", name = "Vehicle")
@XmlType(namespace = "http://beans.generated.openl.org", name = "Vehicle")
public class Vehicle implements Serializable {
    protected int modelYear = 0;

    protected Date vehEffectiveYear;

    public Vehicle() {
        super();
    }

    public Vehicle(int modelYear, Date vehEffectiveYear) {
        super();
        this.modelYear = modelYear;
        this.vehEffectiveYear = vehEffectiveYear;
    }

    @XmlElement(name = "modelYear")
    public int getModelYear() {
        return modelYear;
    }

    public void setModelYear(int modelYear) {
        this.modelYear = modelYear;
    }

    @XmlElement(name = "vehEffectiveYear", nillable = true)
    public Date getVehEffectiveYear() {
        return vehEffectiveYear;
    }

    public void setVehEffectiveYear(Date vehEffectiveYear) {
        this.vehEffectiveYear = vehEffectiveYear;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Vehicle))
            return false;
        Vehicle vehicle = (Vehicle) o;
        return modelYear == vehicle.modelYear && Objects.equals(vehEffectiveYear, vehicle.vehEffectiveYear);
    }

    @Override
    public int hashCode() {

        return Objects.hash(modelYear, vehEffectiveYear);
    }
}