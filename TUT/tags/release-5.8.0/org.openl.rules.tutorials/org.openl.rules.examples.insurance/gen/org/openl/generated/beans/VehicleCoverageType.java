/*
 * This class has been generated. Do not change it. 
*/

package org.openl.generated.beans;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.ArrayUtils;
import org.openl.generated.beans.LimitsAndFactors;
import org.openl.generated.beans.InsurableVehicle;

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
  public org.openl.generated.beans.InsurableVehicle getVehicle() {
   return vehicle;
}
  public void setVehicle(org.openl.generated.beans.InsurableVehicle vehicle) {
   this.vehicle = vehicle;
}
  public org.openl.generated.beans.LimitsAndFactors getLimitBI() {
   return limitBI;
}
  public org.openl.generated.beans.LimitsAndFactors getLimitPD() {
   return limitPD;
}
  public org.openl.generated.beans.LimitsAndFactors getLimitMP() {
   return limitMP;
}
  public void setLimitBI(org.openl.generated.beans.LimitsAndFactors limitBI) {
   this.limitBI = limitBI;
}
  public void setLimitPD(org.openl.generated.beans.LimitsAndFactors limitPD) {
   this.limitPD = limitPD;
}
  public void setLimitMP(org.openl.generated.beans.LimitsAndFactors limitMP) {
   this.limitMP = limitMP;
}

public boolean equals(Object obj) {
    EqualsBuilder builder = new EqualsBuilder();
    if (!(obj instanceof VehicleCoverageType)) {;
        return false;
    }
    VehicleCoverageType another = (VehicleCoverageType)obj;
    builder.append(another.getVehicle(),getVehicle());
    builder.append(another.getLimitBI(),getLimitBI());
    builder.append(another.getLimitPD(),getLimitPD());
    builder.append(another.getLimitMP(),getLimitMP());
    return builder.isEquals();
}

public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("VehicleCoverageType {");
    builder.append(" vehicle=");
    builder.append(getVehicle());
    builder.append(" limitBI=");
    builder.append(getLimitBI());
    builder.append(" limitPD=");
    builder.append(getLimitPD());
    builder.append(" limitMP=");
    builder.append(getLimitMP());
    builder.append(" }");
    return builder.toString();
}

public int hashCode() {
    HashCodeBuilder builder = new HashCodeBuilder();
    builder.append(getVehicle());
    builder.append(getLimitBI());
    builder.append(getLimitPD());
    builder.append(getLimitMP());
    return builder.toHashCode();
}

}