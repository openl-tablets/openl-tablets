/*
 * This class has been generated. Do not change it. 
*/

package org.openl.eclipse.interface.based;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import java.lang.String;
import org.apache.commons.lang.ArrayUtils;

public class Vehicle{
  protected java.lang.String airbags;

  protected java.lang.String bodyType;

  protected java.lang.String[] coverage;

  protected boolean hasAlarm = false;

  protected boolean hasRollBar = false;

  protected java.lang.String model;

  protected java.lang.String name;

  protected boolean onHighTheftProbabilityList = false;

  protected double price = 0.0;

  protected java.lang.String type;

  protected int year = 0;



public Vehicle() {
    super();
}

public Vehicle(String name, String model, int year, boolean hasAlarm, String type, boolean onHighTheftProbabilityList, String airbags, double price, String bodyType, String[] coverage, boolean hasRollBar) {
    super();
    this.name = name;
    this.model = model;
    this.year = year;
    this.hasAlarm = hasAlarm;
    this.type = type;
    this.onHighTheftProbabilityList = onHighTheftProbabilityList;
    this.airbags = airbags;
    this.price = price;
    this.bodyType = bodyType;
    this.coverage = coverage;
    this.hasRollBar = hasRollBar;
}

public boolean equals(Object obj) {
    EqualsBuilder builder = new EqualsBuilder();
    if (!(obj instanceof Vehicle)) {;
        return false;
    }
    Vehicle another = (Vehicle)obj;
    builder.append(another.getName(),getName());
    builder.append(another.getModel(),getModel());
    builder.append(another.getYear(),getYear());
    builder.append(another.getHasAlarm(),getHasAlarm());
    builder.append(another.getType(),getType());
    builder.append(another.getOnHighTheftProbabilityList(),getOnHighTheftProbabilityList());
    builder.append(another.getAirbags(),getAirbags());
    builder.append(another.getPrice(),getPrice());
    builder.append(another.getBodyType(),getBodyType());
    builder.append(another.getCoverage(),getCoverage());
    builder.append(another.getHasRollBar(),getHasRollBar());
    return builder.isEquals();
}
  public java.lang.String getAirbags() {
   return airbags;
}
  public java.lang.String getBodyType() {
   return bodyType;
}
  public java.lang.String[] getCoverage() {
   return coverage;
}
  public boolean getHasAlarm() {
   return hasAlarm;
}
  public boolean getHasRollBar() {
   return hasRollBar;
}
  public java.lang.String getModel() {
   return model;
}
  public java.lang.String getName() {
   return name;
}
  public boolean getOnHighTheftProbabilityList() {
   return onHighTheftProbabilityList;
}
  public double getPrice() {
   return price;
}
  public java.lang.String getType() {
   return type;
}
  public int getYear() {
   return year;
}

public int hashCode() {
    HashCodeBuilder builder = new HashCodeBuilder();
    builder.append(getName());
    builder.append(getModel());
    builder.append(getYear());
    builder.append(getHasAlarm());
    builder.append(getType());
    builder.append(getOnHighTheftProbabilityList());
    builder.append(getAirbags());
    builder.append(getPrice());
    builder.append(getBodyType());
    builder.append(getCoverage());
    builder.append(getHasRollBar());
    return builder.toHashCode();
}
  public void setAirbags(java.lang.String airbags) {
   this.airbags = airbags;
}
  public void setBodyType(java.lang.String bodyType) {
   this.bodyType = bodyType;
}
  public void setCoverage(java.lang.String[] coverage) {
   this.coverage = coverage;
}
  public void setHasAlarm(boolean hasAlarm) {
   this.hasAlarm = hasAlarm;
}
  public void setHasRollBar(boolean hasRollBar) {
   this.hasRollBar = hasRollBar;
}
  public void setModel(java.lang.String model) {
   this.model = model;
}
  public void setName(java.lang.String name) {
   this.name = name;
}
  public void setOnHighTheftProbabilityList(boolean onHighTheftProbabilityList) {
   this.onHighTheftProbabilityList = onHighTheftProbabilityList;
}
  public void setPrice(double price) {
   this.price = price;
}
  public void setType(java.lang.String type) {
   this.type = type;
}
  public void setYear(int year) {
   this.year = year;
}

public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Vehicle {");
    builder.append(" name=");
    builder.append(getName());
    builder.append(" model=");
    builder.append(getModel());
    builder.append(" year=");
    builder.append(getYear());
    builder.append(" hasAlarm=");
    builder.append(getHasAlarm());
    builder.append(" type=");
    builder.append(getType());
    builder.append(" onHighTheftProbabilityList=");
    builder.append(getOnHighTheftProbabilityList());
    builder.append(" airbags=");
    builder.append(getAirbags());
    builder.append(" price=");
    builder.append(getPrice());
    builder.append(" bodyType=");
    builder.append(getBodyType());
    builder.append(" coverage=");
    builder.append(ArrayUtils.toString(getCoverage()));
    builder.append(" hasRollBar=");
    builder.append(getHasRollBar());
    builder.append(" }");
    return builder.toString();
}

}