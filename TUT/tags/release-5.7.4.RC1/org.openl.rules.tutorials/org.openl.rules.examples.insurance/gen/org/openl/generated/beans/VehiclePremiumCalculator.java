/*
 * This class has been generated. Do not change it. 
*/

package org.openl.generated.beans;

import org.apache.commons.lang.builder.EqualsBuilder;
import java.util.Vector;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.ArrayUtils;
import org.openl.generated.beans.InsurableDriver;
import org.openl.generated.beans.InsurableVehicle;

public class VehiclePremiumCalculator{
  protected org.openl.generated.beans.InsurableVehicle vehicle;

  protected java.util.Vector discountsForVehicle;

  protected java.util.Vector discountsForDriver;

  protected org.openl.generated.beans.InsurableDriver designatedDriver;



public VehiclePremiumCalculator() {
    super();
}

public VehiclePremiumCalculator(InsurableVehicle vehicle, Vector discountsForVehicle, Vector discountsForDriver, InsurableDriver designatedDriver) {
    super();
    this.vehicle = vehicle;
    this.discountsForVehicle = discountsForVehicle;
    this.discountsForDriver = discountsForDriver;
    this.designatedDriver = designatedDriver;
}
  public org.openl.generated.beans.InsurableVehicle getVehicle() {
   return vehicle;
}
  public java.util.Vector getDiscountsForVehicle() {
   return discountsForVehicle;
}
  public java.util.Vector getDiscountsForDriver() {
   return discountsForDriver;
}
  public org.openl.generated.beans.InsurableDriver getDesignatedDriver() {
   return designatedDriver;
}
  public void setVehicle(org.openl.generated.beans.InsurableVehicle vehicle) {
   this.vehicle = vehicle;
}
  public void setDiscountsForVehicle(java.util.Vector discountsForVehicle) {
   this.discountsForVehicle = discountsForVehicle;
}
  public void setDiscountsForDriver(java.util.Vector discountsForDriver) {
   this.discountsForDriver = discountsForDriver;
}
  public void setDesignatedDriver(org.openl.generated.beans.InsurableDriver designatedDriver) {
   this.designatedDriver = designatedDriver;
}

public boolean equals(Object obj) {
    EqualsBuilder builder = new EqualsBuilder();
    if (!(obj instanceof VehiclePremiumCalculator)) {;
        return false;
    }
    VehiclePremiumCalculator another = (VehiclePremiumCalculator)obj;
    builder.append(another.getVehicle(),getVehicle());
    builder.append(another.getDiscountsForVehicle(),getDiscountsForVehicle());
    builder.append(another.getDiscountsForDriver(),getDiscountsForDriver());
    builder.append(another.getDesignatedDriver(),getDesignatedDriver());
    return builder.isEquals();
}

public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("VehiclePremiumCalculator {");
    builder.append(" vehicle=");
    builder.append(getVehicle());
    builder.append(" discountsForVehicle=");
    builder.append(getDiscountsForVehicle());
    builder.append(" discountsForDriver=");
    builder.append(getDiscountsForDriver());
    builder.append(" designatedDriver=");
    builder.append(getDesignatedDriver());
    builder.append(" }");
    return builder.toString();
}

public int hashCode() {
    HashCodeBuilder builder = new HashCodeBuilder();
    builder.append(getVehicle());
    builder.append(getDiscountsForVehicle());
    builder.append(getDiscountsForDriver());
    builder.append(getDesignatedDriver());
    return builder.toHashCode();
}

}