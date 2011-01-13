/*
 * This class has been generated. Do not change it. 
*/

package org.openl.generated.beans;

import org.openl.generated.beans.Address;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.openl.generated.beans.Usage;
import org.apache.commons.lang.ArrayUtils;
import org.openl.generated.beans.InsurableDriver;
import org.openl.generated.beans.InsurableVehicle;

public class InsurancePolicy{
  protected org.openl.generated.beans.InsurableDriver[] drivers;

  protected org.openl.generated.beans.InsurableVehicle[] vehicles;

  protected org.openl.generated.beans.Usage[] usages;

  protected org.openl.generated.beans.Address address;



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
  public org.openl.generated.beans.InsurableDriver[] getDrivers() {
   return drivers;
}
  public org.openl.generated.beans.InsurableVehicle[] getVehicles() {
   return vehicles;
}
  public org.openl.generated.beans.Usage[] getUsages() {
   return usages;
}
  public void setDrivers(org.openl.generated.beans.InsurableDriver[] drivers) {
   this.drivers = drivers;
}
  public void setVehicles(org.openl.generated.beans.InsurableVehicle[] vehicles) {
   this.vehicles = vehicles;
}
  public void setUsages(org.openl.generated.beans.Usage[] usages) {
   this.usages = usages;
}

public boolean equals(Object obj) {
    EqualsBuilder builder = new EqualsBuilder();
    if (!(obj instanceof InsurancePolicy)) {;
        return false;
    }
    InsurancePolicy another = (InsurancePolicy)obj;
    builder.append(another.getDrivers(),getDrivers());
    builder.append(another.getVehicles(),getVehicles());
    builder.append(another.getUsages(),getUsages());
    builder.append(another.getAddress(),getAddress());
    return builder.isEquals();
}

public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("InsurancePolicy {");
    builder.append(" drivers=");
    builder.append(ArrayUtils.toString(getDrivers()));
    builder.append(" vehicles=");
    builder.append(ArrayUtils.toString(getVehicles()));
    builder.append(" usages=");
    builder.append(ArrayUtils.toString(getUsages()));
    builder.append(" address=");
    builder.append(getAddress());
    builder.append(" }");
    return builder.toString();
}

public int hashCode() {
    HashCodeBuilder builder = new HashCodeBuilder();
    builder.append(getDrivers());
    builder.append(getVehicles());
    builder.append(getUsages());
    builder.append(getAddress());
    return builder.toHashCode();
}
  public org.openl.generated.beans.Address getAddress() {
   return address;
}
  public void setAddress(org.openl.generated.beans.Address address) {
   this.address = address;
}

}