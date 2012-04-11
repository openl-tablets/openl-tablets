/*
 * This class has been generated. Do not change it. 
*/

package org.openl.generated.beans;

import org.openl.generated.beans.Driver;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.openl.generated.beans.Vehicle;
import java.lang.String;

public class Policy{
  protected java.lang.String name;

  protected java.lang.String clientTier;

  protected java.lang.String clientTerm;

  protected org.openl.generated.beans.Driver[] drivers;

  protected org.openl.generated.beans.Vehicle[] vehicles;



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
    builder.append(another.name,name);
    builder.append(another.clientTier,clientTier);
    builder.append(another.clientTerm,clientTerm);
    builder.append(another.drivers,drivers);
    builder.append(another.vehicles,vehicles);
    return builder.isEquals();
}

public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Policy {");
    builder.append(" name=");
    builder.append(name);
    builder.append(" clientTier=");
    builder.append(clientTier);
    builder.append(" clientTerm=");
    builder.append(clientTerm);
    builder.append(" drivers=");
    builder.append(drivers);
    builder.append(" vehicles=");
    builder.append(vehicles);
    builder.append(" }");
    return builder.toString();
}

public int hashCode() {
    HashCodeBuilder builder = new HashCodeBuilder();
    builder.append(name);
    builder.append(clientTier);
    builder.append(clientTerm);
    builder.append(drivers);
    builder.append(vehicles);
    return builder.toHashCode();
}
  public java.lang.String getName() {
   return name;
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
  public org.openl.generated.beans.Driver[] getDrivers() {
   return drivers;
}
  public void setDrivers(org.openl.generated.beans.Driver[] drivers) {
   this.drivers = drivers;
}
  public org.openl.generated.beans.Vehicle[] getVehicles() {
   return vehicles;
}
  public void setVehicles(org.openl.generated.beans.Vehicle[] vehicles) {
   this.vehicles = vehicles;
}

}