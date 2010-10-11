/*
 * This class has been generated. Do not change it. 
*/

package org.openl.generated.beans;

import java.lang.String;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.ArrayUtils;

public class InsurableVehicle{
  protected java.lang.String id;

  protected int year;

  protected java.lang.String make;

  protected java.lang.String model;

  protected boolean hasAbs;

  protected boolean hasAlarm;

  protected int yearlyMileage;



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

public int hashCode() {
    HashCodeBuilder builder = new HashCodeBuilder();
    builder.append(id);
    builder.append(make);
    builder.append(model);
    builder.append(year);
    builder.append(hasAbs);
    builder.append(hasAlarm);
    builder.append(yearlyMileage);
    return builder.toHashCode();
}

public boolean equals(Object obj) {
    EqualsBuilder builder = new EqualsBuilder();
    if (!(obj instanceof InsurableVehicle)) {;
        return false;
    }
    InsurableVehicle another = (InsurableVehicle)obj;
    builder.append(another.id,id);
    builder.append(another.make,make);
    builder.append(another.model,model);
    builder.append(another.year,year);
    builder.append(another.hasAbs,hasAbs);
    builder.append(another.hasAlarm,hasAlarm);
    builder.append(another.yearlyMileage,yearlyMileage);
    return builder.isEquals();
}

public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("InsurableVehicle {");
    builder.append(" id=");
    builder.append(id);
    builder.append(" make=");
    builder.append(make);
    builder.append(" model=");
    builder.append(model);
    builder.append(" year=");
    builder.append(year);
    builder.append(" hasAbs=");
    builder.append(hasAbs);
    builder.append(" hasAlarm=");
    builder.append(hasAlarm);
    builder.append(" yearlyMileage=");
    builder.append(yearlyMileage);
    builder.append(" }");
    return builder.toString();
}
  public java.lang.String getId() {
   return id;
}
  public int getYear() {
   return year;
}
  public void setId(java.lang.String id) {
   this.id = id;
}
  public void setModel(java.lang.String model) {
   this.model = model;
}
  public void setYear(int year) {
   this.year = year;
}
  public java.lang.String getMake() {
   return make;
}
  public void setMake(java.lang.String make) {
   this.make = make;
}
  public java.lang.String getModel() {
   return model;
}
  public boolean getHasAbs() {
   return hasAbs;
}
  public void setHasAbs(boolean hasAbs) {
   this.hasAbs = hasAbs;
}
  public boolean getHasAlarm() {
   return hasAlarm;
}
  public void setHasAlarm(boolean hasAlarm) {
   this.hasAlarm = hasAlarm;
}
  public int getYearlyMileage() {
   return yearlyMileage;
}
  public void setYearlyMileage(int yearlyMileage) {
   this.yearlyMileage = yearlyMileage;
}

}