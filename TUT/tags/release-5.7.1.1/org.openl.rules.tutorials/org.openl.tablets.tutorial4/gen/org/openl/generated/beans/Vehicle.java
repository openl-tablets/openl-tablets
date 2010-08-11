/*
 * This class has been generated. Do not change it. 
*/

package org.openl.generated.beans;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import java.lang.String;

public class Vehicle{
  private java.lang.String name;

  private java.lang.String type;

  private int year;

  private java.lang.String model;

  private boolean hasAlarm;

  private boolean onHighTheftProbabilityList;

  private java.lang.String airbags;

  private double price;

  private java.lang.String bodyType;

  private java.lang.String[] coverage;

  private boolean hasRollBar;



public Vehicle() {
}

public Vehicle(String name, String model, int year, boolean hasAlarm, String type, boolean onHighTheftProbabilityList, String airbags, double price, String bodyType, String[] coverage, boolean hasRollBar) {
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
    Vehicle another = (Vehicle)obj;    builder.append(another.name,name);
    builder.append(another.model,model);
    builder.append(another.year,year);
    builder.append(another.hasAlarm,hasAlarm);
    builder.append(another.type,type);
    builder.append(another.onHighTheftProbabilityList,onHighTheftProbabilityList);
    builder.append(another.airbags,airbags);
    builder.append(another.price,price);
    builder.append(another.bodyType,bodyType);
    builder.append(another.coverage,coverage);
    builder.append(another.hasRollBar,hasRollBar);
    return builder.isEquals();
}

public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Vehicle {");
    builder.append(" name=");
    builder.append(name);
    builder.append(" model=");
    builder.append(model);
    builder.append(" year=");
    builder.append(year);
    builder.append(" hasAlarm=");
    builder.append(hasAlarm);
    builder.append(" type=");
    builder.append(type);
    builder.append(" onHighTheftProbabilityList=");
    builder.append(onHighTheftProbabilityList);
    builder.append(" airbags=");
    builder.append(airbags);
    builder.append(" price=");
    builder.append(price);
    builder.append(" bodyType=");
    builder.append(bodyType);
    builder.append(" coverage=");
    builder.append(coverage);
    builder.append(" hasRollBar=");
    builder.append(hasRollBar);
    builder.append(" }");
    return builder.toString();
}

public int hashCode() {
    HashCodeBuilder builder = new HashCodeBuilder();
    builder.append(name);
    builder.append(model);
    builder.append(year);
    builder.append(hasAlarm);
    builder.append(type);
    builder.append(onHighTheftProbabilityList);
    builder.append(airbags);
    builder.append(price);
    builder.append(bodyType);
    builder.append(coverage);
    builder.append(hasRollBar);
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
  public java.lang.String getModel() {
   return model;
}
  public boolean getHasAlarm() {
   return hasAlarm;
}
  public void setHasAlarm(boolean hasAlarm) {
   this.hasAlarm = hasAlarm;
}
  public boolean getOnHighTheftProbabilityList() {
   return onHighTheftProbabilityList;
}
  public void setOnHighTheftProbabilityList(boolean onHighTheftProbabilityList) {
   this.onHighTheftProbabilityList = onHighTheftProbabilityList;
}
  public java.lang.String getAirbags() {
   return airbags;
}
  public void setAirbags(java.lang.String airbags) {
   this.airbags = airbags;
}
  public double getPrice() {
   return price;
}
  public void setPrice(double price) {
   this.price = price;
}
  public java.lang.String getBodyType() {
   return bodyType;
}
  public void setBodyType(java.lang.String bodyType) {
   this.bodyType = bodyType;
}
  public java.lang.String[] getCoverage() {
   return coverage;
}
  public void setCoverage(java.lang.String[] coverage) {
   this.coverage = coverage;
}
  public boolean getHasRollBar() {
   return hasRollBar;
}
  public void setHasRollBar(boolean hasRollBar) {
   this.hasRollBar = hasRollBar;
}

}