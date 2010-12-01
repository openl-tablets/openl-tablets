/*
 * This class has been generated. Do not change it. 
*/

package org.openl.generated.beans;

import java.lang.String;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.openl.generated.beans.InsurableVehicle;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.ArrayUtils;

public class VehicleSymbol{
  protected java.lang.String symbol;

  protected org.openl.generated.beans.InsurableVehicle vehicle;



public VehicleSymbol() {
    super();
}

public VehicleSymbol(InsurableVehicle vehicle, String symbol) {
    super();
    this.vehicle = vehicle;
    this.symbol = symbol;
}

public int hashCode() {
    HashCodeBuilder builder = new HashCodeBuilder();
    builder.append(getVehicle());
    builder.append(getSymbol());
    return builder.toHashCode();
}

public boolean equals(Object obj) {
    EqualsBuilder builder = new EqualsBuilder();
    if (!(obj instanceof VehicleSymbol)) {;
        return false;
    }
    VehicleSymbol another = (VehicleSymbol)obj;
    builder.append(another.getVehicle(),getVehicle());
    builder.append(another.getSymbol(),getSymbol());
    return builder.isEquals();
}

public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("VehicleSymbol {");
    builder.append(" vehicle=");
    builder.append(getVehicle());
    builder.append(" symbol=");
    builder.append(getSymbol());
    builder.append(" }");
    return builder.toString();
}
  public java.lang.String getSymbol() {
   return symbol;
}
  public org.openl.generated.beans.InsurableVehicle getVehicle() {
   return vehicle;
}
  public void setVehicle(org.openl.generated.beans.InsurableVehicle vehicle) {
   this.vehicle = vehicle;
}
  public void setSymbol(java.lang.String symbol) {
   this.symbol = symbol;
}

}