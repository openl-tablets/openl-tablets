/*
 * This class has been generated. Do not change it. 
*/

package org.openl.generated.beans;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.openl.generated.beans.InsurableVehicle;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.openl.generated.beans.InsurableDriver;
import org.apache.commons.lang.ArrayUtils;

public class Usage{
  protected int usage;

  protected org.openl.generated.beans.InsurableVehicle vehicle;

  protected org.openl.generated.beans.InsurableDriver driver;



public Usage() {
    super();
}

public Usage(InsurableDriver driver, InsurableVehicle vehicle, int usage) {
    super();
    this.driver = driver;
    this.vehicle = vehicle;
    this.usage = usage;
}

public int hashCode() {
    HashCodeBuilder builder = new HashCodeBuilder();
    builder.append(getDriver());
    builder.append(getVehicle());
    builder.append(getUsage());
    return builder.toHashCode();
}

public boolean equals(Object obj) {
    EqualsBuilder builder = new EqualsBuilder();
    if (!(obj instanceof Usage)) {;
        return false;
    }
    Usage another = (Usage)obj;
    builder.append(another.getDriver(),getDriver());
    builder.append(another.getVehicle(),getVehicle());
    builder.append(another.getUsage(),getUsage());
    return builder.isEquals();
}

public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Usage {");
    builder.append(" driver=");
    builder.append(getDriver());
    builder.append(" vehicle=");
    builder.append(getVehicle());
    builder.append(" usage=");
    builder.append(getUsage());
    builder.append(" }");
    return builder.toString();
}
  public int getUsage() {
   return usage;
}
  public void setUsage(int usage) {
   this.usage = usage;
}
  public org.openl.generated.beans.InsurableVehicle getVehicle() {
   return vehicle;
}
  public void setVehicle(org.openl.generated.beans.InsurableVehicle vehicle) {
   this.vehicle = vehicle;
}
  public org.openl.generated.beans.InsurableDriver getDriver() {
   return driver;
}
  public void setDriver(org.openl.generated.beans.InsurableDriver driver) {
   this.driver = driver;
}

}