/*
 * This class has been generated. Do not change it. 
*/

package org.openl.generated.beans;

import java.util.Vector;
import org.openl.generated.beans.InsurableDriver;
import org.openl.generated.beans.InsurableVehicle;

public class VehiclePremiumCalculator{
  private org.openl.generated.beans.InsurableVehicle vehicle;

  private org.openl.generated.beans.InsurableDriver designatedDriver;

  private java.util.Vector discountsForDriver;

  private java.util.Vector discountsForVehicle;


  public org.openl.generated.beans.InsurableVehicle getVehicle() {
   return vehicle;
}
  public void setVehicle(org.openl.generated.beans.InsurableVehicle vehicle) {
   this.vehicle = vehicle;
}
  public org.openl.generated.beans.InsurableDriver getDesignatedDriver() {
   return designatedDriver;
}
  public void setDesignatedDriver(org.openl.generated.beans.InsurableDriver designatedDriver) {
   this.designatedDriver = designatedDriver;
}
  public java.util.Vector getDiscountsForDriver() {
   return discountsForDriver;
}
  public void setDiscountsForDriver(java.util.Vector discountsForDriver) {
   this.discountsForDriver = discountsForDriver;
}
  public java.util.Vector getDiscountsForVehicle() {
   return discountsForVehicle;
}
  public void setDiscountsForVehicle(java.util.Vector discountsForVehicle) {
   this.discountsForVehicle = discountsForVehicle;
}

}