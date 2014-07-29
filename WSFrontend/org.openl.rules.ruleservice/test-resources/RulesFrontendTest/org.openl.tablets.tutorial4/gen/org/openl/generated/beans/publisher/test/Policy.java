/*
 * This class has been generated. Do not change it. 
*/

package org.openl.generated.beans.publisher.test;

import org.openl.generated.beans.publisher.test.Vehicle;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import java.lang.String;
import org.apache.commons.lang3.ArrayUtils;
import org.openl.generated.beans.publisher.test.Driver;

public class Policy{
  protected java.lang.String clientTerm;

  protected java.lang.String clientTier;

  protected org.openl.generated.beans.publisher.test.Driver[] drivers;

  protected java.lang.String name;

  protected org.openl.generated.beans.publisher.test.Vehicle[] vehicles;



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

public boolean equals(Object obj) {
    EqualsBuilder builder = new EqualsBuilder();
    if (!(obj instanceof Policy)) {;
        return false;
    }
    Policy another = (Policy)obj;
    builder.append(another.getName(),getName());
    builder.append(another.getClientTier(),getClientTier());
    builder.append(another.getClientTerm(),getClientTerm());
    builder.append(another.getDrivers(),getDrivers());
    builder.append(another.getVehicles(),getVehicles());
    return builder.isEquals();
}
  public java.lang.String getClientTerm() {
   return clientTerm;
}
  public java.lang.String getClientTier() {
   return clientTier;
}
  public org.openl.generated.beans.publisher.test.Driver[] getDrivers() {
   return drivers;
}
  public java.lang.String getName() {
   return name;
}
  public org.openl.generated.beans.publisher.test.Vehicle[] getVehicles() {
   return vehicles;
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
  public void setClientTerm(java.lang.String clientTerm) {
   this.clientTerm = clientTerm;
}
  public void setClientTier(java.lang.String clientTier) {
   this.clientTier = clientTier;
}
  public void setDrivers(org.openl.generated.beans.publisher.test.Driver[] drivers) {
   this.drivers = drivers;
}
  public void setName(java.lang.String name) {
   this.name = name;
}
  public void setVehicles(org.openl.generated.beans.publisher.test.Vehicle[] vehicles) {
   this.vehicles = vehicles;
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

}