/*
 * This class has been generated. Do not change it. 
*/

package org.openl.generated.test.beans;

import java.lang.String;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.openl.generated.test.beans.Driver;
import org.openl.generated.test.beans.Vehicle;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.ArrayUtils;

public class Policy {
    protected java.lang.String name;

    protected java.lang.String clientTier;

    protected java.lang.String clientTerm;

    protected org.openl.generated.test.beans.Driver[] drivers;

    protected org.openl.generated.test.beans.Vehicle[] vehicles;

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
            ;
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

    public java.lang.String getName() {
        return name;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Policy {");
        builder.append(" name=");
        builder.append(getName());
        builder.append(" clientTier=");
        builder.append(getClientTier());
        builder.append(" clientTerm=");
        builder.append(getClientTerm());
        builder.append(" drivers=");
        builder.append(ArrayUtils.toString(getDrivers()));
        builder.append(" vehicles=");
        builder.append(ArrayUtils.toString(getVehicles()));
        builder.append(" }");
        return builder.toString();
    }

    public void setName(java.lang.String name) {
        this.name = name;
    }

    public java.lang.String getClientTier() {
        return clientTier;
    }

    public void setClientTier(java.lang.String clientTier) {
        this.clientTier = clientTier;
    }

    public java.lang.String getClientTerm() {
        return clientTerm;
    }

    public void setClientTerm(java.lang.String clientTerm) {
        this.clientTerm = clientTerm;
    }

    public org.openl.generated.test.beans.Driver[] getDrivers() {
        return drivers;
    }

    public void setDrivers(org.openl.generated.test.beans.Driver[] drivers) {
        this.drivers = drivers;
    }

    public org.openl.generated.test.beans.Vehicle[] getVehicles() {
        return vehicles;
    }

    public void setVehicles(org.openl.generated.test.beans.Vehicle[] vehicles) {
        this.vehicles = vehicles;
    }

}