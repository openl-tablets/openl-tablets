/*
 * This class has been generated. Do not change it. 
*/

package org.openl.generated.beans;

import org.openl.generated.beans.Address;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.openl.generated.beans.InsurableVehicle;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.openl.generated.beans.Usage;
import org.openl.generated.beans.InsurableDriver;
import org.apache.commons.lang.ArrayUtils;

public class InsurancePolicy{
  protected org.openl.generated.beans.Address address;

  protected org.openl.generated.beans.InsurableDriver[] drivers;

  protected org.openl.generated.beans.InsurableVehicle[] vehicles;

  protected org.openl.generated.beans.Usage[] usages;



public InsurancePolicy() {
    super();
}

public InsurancePolicy(InsurableDriver[] drivers, InsurableVehicle[] vehicles, Usage[] usages, Address address) {
    super();
    this.drivers = drivers;
    this.vehicles = vehicles;
    this.usages = usages;
    this.address = address;
}

public int hashCode() {
    HashCodeBuilder builder = new HashCodeBuilder();
    builder.append(drivers);
    builder.append(vehicles);
    builder.append(usages);
    builder.append(address);
    return builder.toHashCode();
}
  public org.openl.generated.beans.Address getAddress() {
   return address;
}

public boolean equals(Object obj) {
    EqualsBuilder builder = new EqualsBuilder();
    if (!(obj instanceof InsurancePolicy)) {;
        return false;
    }
    InsurancePolicy another = (InsurancePolicy)obj;
    builder.append(another.drivers,drivers);
    builder.append(another.vehicles,vehicles);
    builder.append(another.usages,usages);
    builder.append(another.address,address);
    return builder.isEquals();
}

public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("InsurancePolicy {");
    builder.append(" drivers=");
    builder.append(ArrayUtils.toString(drivers));
    builder.append(" vehicles=");
    builder.append(ArrayUtils.toString(vehicles));
    builder.append(" usages=");
    builder.append(ArrayUtils.toString(usages));
    builder.append(" address=");
    builder.append(address);
    builder.append(" }");
    return builder.toString();
}
  public void setAddress(org.openl.generated.beans.Address address) {
   this.address = address;
}
  public org.openl.generated.beans.InsurableDriver[] getDrivers() {
   return drivers;
}
  public void setDrivers(org.openl.generated.beans.InsurableDriver[] drivers) {
   this.drivers = drivers;
}
  public org.openl.generated.beans.InsurableVehicle[] getVehicles() {
   return vehicles;
}
  public void setVehicles(org.openl.generated.beans.InsurableVehicle[] vehicles) {
   this.vehicles = vehicles;
}
  public org.openl.generated.beans.Usage[] getUsages() {
   return usages;
}
  public void setUsages(org.openl.generated.beans.Usage[] usages) {
   this.usages = usages;
}

}