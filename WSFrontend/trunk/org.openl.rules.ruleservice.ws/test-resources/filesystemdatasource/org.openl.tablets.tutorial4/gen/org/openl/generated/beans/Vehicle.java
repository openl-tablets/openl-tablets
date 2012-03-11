/*
 * This class has been generated. Do not change it. 
*/

package org.openl.generated.beans;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import java.lang.String;
import org.apache.commons.lang.ArrayUtils;

public class Vehicle{
  protected java.lang.String name;

  protected java.lang.String type;

  protected int year;

  protected java.lang.String bodyType;

  protected boolean hasRollBar;

  protected java.lang.String model;

  protected double price;

  protected java.lang.String[] coverage;

  protected java.lang.String airbags;

  protected boolean onHighTheftProbabilityList;

  protected boolean hasAlarm;



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
  public java.lang.String getName() {
   return name;
}
  public void setName(java.lang.String name) {
   this.name = name;
}
  public java.lang.String getType() {
   return type;
}
  public int getYear() {
   return year;
}
  public void setType(java.lang.String type) {
   this.type = type;
}
  public void setModel(java.lang.String model) {
   this.model = model;
}
  public void setYear(int year) {
   this.year = year;
}
  public java.lang.String getBodyType() {
   return bodyType;
}
  public void setBodyType(java.lang.String bodyType) {
   this.bodyType = bodyType;
}
  public boolean getHasRollBar() {
   return hasRollBar;
}
  public void setHasRollBar(boolean hasRollBar) {
   this.hasRollBar = hasRollBar;
}
  public java.lang.String getModel() {
   return model;
}
  public double getPrice() {
   return price;
}
  public void setPrice(double price) {
   this.price = price;
}
  public java.lang.String[] getCoverage() {
   return coverage;
}
  public void setCoverage(java.lang.String[] coverage) {
   this.coverage = coverage;
}
  public java.lang.String getAirbags() {
   return airbags;
}
  public void setAirbags(java.lang.String airbags) {
   this.airbags = airbags;
}
  public boolean getOnHighTheftProbabilityList() {
   return onHighTheftProbabilityList;
}
  public void setOnHighTheftProbabilityList(boolean onHighTheftProbabilityList) {
   this.onHighTheftProbabilityList = onHighTheftProbabilityList;
}
  public boolean getHasAlarm() {
   return hasAlarm;
}
  public void setHasAlarm(boolean hasAlarm) {
   this.hasAlarm = hasAlarm;
}

}