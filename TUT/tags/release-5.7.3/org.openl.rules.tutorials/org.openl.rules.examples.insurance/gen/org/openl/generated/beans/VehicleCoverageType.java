/*
 * This class has been generated. Do not change it. 
*/

package org.openl.generated.beans;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.openl.generated.beans.InsurableVehicle;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.ArrayUtils;
import org.openl.generated.beans.LimitsAndFactors;

public class VehicleCoverageType{
  protected org.openl.generated.beans.InsurableVehicle vehicle;

  protected org.openl.generated.beans.LimitsAndFactors limitBI;

  protected org.openl.generated.beans.LimitsAndFactors limitPD;

  protected org.openl.generated.beans.LimitsAndFactors limitMP;



public VehicleCoverageType() {
    super();
}

public VehicleCoverageType(InsurableVehicle vehicle, LimitsAndFactors limitBI, LimitsAndFactors limitPD, LimitsAndFactors limitMP) {
    super();
    this.vehicle = vehicle;
    this.limitBI = limitBI;
    this.limitPD = limitPD;
    this.limitMP = limitMP;
}

public int hashCode() {
    HashCodeBuilder builder = new HashCodeBuilder();
    builder.append(vehicle);
    builder.append(limitBI);
    builder.append(limitPD);
    builder.append(limitMP);
    return builder.toHashCode();
}

public boolean equals(Object obj) {
    EqualsBuilder builder = new EqualsBuilder();
    if (!(obj instanceof VehicleCoverageType)) {;
        return false;
    }
    VehicleCoverageType another = (VehicleCoverageType)obj;
    builder.append(another.vehicle,vehicle);
    builder.append(another.limitBI,limitBI);
    builder.append(another.limitPD,limitPD);
    builder.append(another.limitMP,limitMP);
    return builder.isEquals();
}

public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("VehicleCoverageType {");
    builder.append(" vehicle=");
    builder.append(vehicle);
    builder.append(" limitBI=");
    builder.append(limitBI);
    builder.append(" limitPD=");
    builder.append(limitPD);
    builder.append(" limitMP=");
    builder.append(limitMP);
    builder.append(" }");
    return builder.toString();
}
  public org.openl.generated.beans.InsurableVehicle getVehicle() {
   return vehicle;
}
  public void setVehicle(org.openl.generated.beans.InsurableVehicle vehicle) {
   this.vehicle = vehicle;
}
  public org.openl.generated.beans.LimitsAndFactors getLimitBI() {
   return limitBI;
}
  public void setLimitBI(org.openl.generated.beans.LimitsAndFactors limitBI) {
   this.limitBI = limitBI;
}
  public org.openl.generated.beans.LimitsAndFactors getLimitPD() {
   return limitPD;
}
  public void setLimitPD(org.openl.generated.beans.LimitsAndFactors limitPD) {
   this.limitPD = limitPD;
}
  public org.openl.generated.beans.LimitsAndFactors getLimitMP() {
   return limitMP;
}
  public void setLimitMP(org.openl.generated.beans.LimitsAndFactors limitMP) {
   this.limitMP = limitMP;
}

}