/*
 * This class has been generated. Do not change it. 
*/

package org.openl.generated.beans;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import java.lang.String;
import org.apache.commons.lang.ArrayUtils;

public class InsurableVehicle{
  protected java.lang.String make;

  protected boolean hasAbs;

  protected boolean hasAlarm;

  protected int yearlyMileage;

  protected java.lang.String id;

  protected java.lang.String model;

  protected int year;



public InsurableVehicle() {
    super();
}

public InsurableVehicle(String id, String make, String model, int year, boolean hasAbs, boolean hasAlarm, int yearlyMileage) {
    super();
    this.id = id;
    this.make = make;
    this.model = model;
    this.year = year;
    this.hasAbs = hasAbs;
    this.hasAlarm = hasAlarm;
    this.yearlyMileage = yearlyMileage;
}
  public void setId(java.lang.String id) {
   this.id = id;
}
  public java.lang.String getMake() {
   return make;
}
  public boolean getHasAbs() {
   return hasAbs;
}
  public boolean getHasAlarm() {
   return hasAlarm;
}
  public int getYearlyMileage() {
   return yearlyMileage;
}
  public void setMake(java.lang.String make) {
   this.make = make;
}
  public void setHasAbs(boolean hasAbs) {
   this.hasAbs = hasAbs;
}
  public void setHasAlarm(boolean hasAlarm) {
   this.hasAlarm = hasAlarm;
}
  public void setYearlyMileage(int yearlyMileage) {
   this.yearlyMileage = yearlyMileage;
}

public boolean equals(Object obj) {
    EqualsBuilder builder = new EqualsBuilder();
    if (!(obj instanceof InsurableVehicle)) {;
        return false;
    }
    InsurableVehicle another = (InsurableVehicle)obj;
    builder.append(another.getId(),getId());
    builder.append(another.getMake(),getMake());
    builder.append(another.getModel(),getModel());
    builder.append(another.getYear(),getYear());
    builder.append(another.getHasAbs(),getHasAbs());
    builder.append(another.getHasAlarm(),getHasAlarm());
    builder.append(another.getYearlyMileage(),getYearlyMileage());
    return builder.isEquals();
}

public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("InsurableVehicle {");
    builder.append(" id=");
    builder.append(getId());
    builder.append(" make=");
    builder.append(getMake());
    builder.append(" model=");
    builder.append(getModel());
    builder.append(" year=");
    builder.append(getYear());
    builder.append(" hasAbs=");
    builder.append(getHasAbs());
    builder.append(" hasAlarm=");
    builder.append(getHasAlarm());
    builder.append(" yearlyMileage=");
    builder.append(getYearlyMileage());
    builder.append(" }");
    return builder.toString();
}

public int hashCode() {
    HashCodeBuilder builder = new HashCodeBuilder();
    builder.append(getId());
    builder.append(getMake());
    builder.append(getModel());
    builder.append(getYear());
    builder.append(getHasAbs());
    builder.append(getHasAlarm());
    builder.append(getYearlyMileage());
    return builder.toHashCode();
}
  public java.lang.String getId() {
   return id;
}
  public java.lang.String getModel() {
   return model;
}
  public int getYear() {
   return year;
}
  public void setModel(java.lang.String model) {
   this.model = model;
}
  public void setYear(int year) {
   this.year = year;
}

}