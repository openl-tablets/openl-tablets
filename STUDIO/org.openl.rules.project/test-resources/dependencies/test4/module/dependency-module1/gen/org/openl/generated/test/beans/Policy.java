/*
 * This class has been generated. Do not change it. 
*/

package org.openl.generated.test.beans;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class Policy {
    protected String name;

    protected String clientTier;

    protected String clientTerm;

    protected Driver[] drivers;

    protected Vehicle[] vehicles;

    public Policy() {
        super();
    }

    public Policy(String name, String clientTier, String clientTerm, Driver[] drivers, Vehicle[] vehicles) {
        super();
        this.name = name;
        this.clientTier = clientTier;
        this.clientTerm = clientTerm;
        this.drivers = drivers;
        this.vehicles = vehicles;
    }

    public int hashCode() {
        HashCodeBuilder builder = new HashCodeBuilder();
        builder.append(getName());
        builder.append(getClientTier());
        builder.append(getClientTerm());
        builder.append(getDrivers());
        builder.append(getVehicles());
        return builder.toHashCode();
    }

    public boolean equals(Object obj) {
        EqualsBuilder builder = new EqualsBuilder();
        if (!(obj instanceof Policy)) {
            return false;
        }
        Policy another = (Policy) obj;
        builder.append(another.getName(), getName());
        builder.append(another.getClientTier(), getClientTier());
        builder.append(another.getClientTerm(), getClientTerm());
        builder.append(another.getDrivers(), getDrivers());
        builder.append(another.getVehicles(), getVehicles());
        return builder.isEquals();
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return "Policy {" + " name=" + getName() + " clientTier=" + getClientTier() + " clientTerm=" + getClientTerm() + " drivers=" + ArrayUtils
                .toString(getDrivers()) + " vehicles=" + ArrayUtils.toString(getVehicles()) + " }";
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClientTier() {
        return clientTier;
    }

    public void setClientTier(String clientTier) {
        this.clientTier = clientTier;
    }

    public String getClientTerm() {
        return clientTerm;
    }

    public void setClientTerm(String clientTerm) {
        this.clientTerm = clientTerm;
    }

    public Driver[] getDrivers() {
        return drivers;
    }

    public void setDrivers(Driver[] drivers) {
        this.drivers = drivers;
    }

    public Vehicle[] getVehicles() {
        return vehicles;
    }

    public void setVehicles(Vehicle[] vehicles) {
        this.vehicles = vehicles;
    }

}