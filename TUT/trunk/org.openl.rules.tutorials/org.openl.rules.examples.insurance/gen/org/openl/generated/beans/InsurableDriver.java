/*
 * This class has been generated. Do not change it. 
*/

package org.openl.generated.beans;

import java.lang.String;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.ArrayUtils;

public class InsurableDriver{
  protected java.lang.String name;

  protected int age;

  protected java.lang.String gender;

  protected java.lang.String maritalStatus;

  protected int dmvPoints;



public InsurableDriver() {
    super();
}

public InsurableDriver(String name, int age, String gender, String maritalStatus, int dmvPoints) {
    super();
    this.name = name;
    this.age = age;
    this.gender = gender;
    this.maritalStatus = maritalStatus;
    this.dmvPoints = dmvPoints;
}

public int hashCode() {
    HashCodeBuilder builder = new HashCodeBuilder();
    builder.append(getName());
    builder.append(getAge());
    builder.append(getGender());
    builder.append(getMaritalStatus());
    builder.append(getDmvPoints());
    return builder.toHashCode();
}

public boolean equals(Object obj) {
    EqualsBuilder builder = new EqualsBuilder();
    if (!(obj instanceof InsurableDriver)) {;
        return false;
    }
    InsurableDriver another = (InsurableDriver)obj;
    builder.append(another.getName(),getName());
    builder.append(another.getAge(),getAge());
    builder.append(another.getGender(),getGender());
    builder.append(another.getMaritalStatus(),getMaritalStatus());
    builder.append(another.getDmvPoints(),getDmvPoints());
    return builder.isEquals();
}
  public java.lang.String getName() {
   return name;
}

public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("InsurableDriver {");
    builder.append(" name=");
    builder.append(getName());
    builder.append(" age=");
    builder.append(getAge());
    builder.append(" gender=");
    builder.append(getGender());
    builder.append(" maritalStatus=");
    builder.append(getMaritalStatus());
    builder.append(" dmvPoints=");
    builder.append(getDmvPoints());
    builder.append(" }");
    return builder.toString();
}
  public void setName(java.lang.String name) {
   this.name = name;
}
  public int getAge() {
   return age;
}
  public void setAge(int age) {
   this.age = age;
}
  public java.lang.String getGender() {
   return gender;
}
  public void setGender(java.lang.String gender) {
   this.gender = gender;
}
  public java.lang.String getMaritalStatus() {
   return maritalStatus;
}
  public void setMaritalStatus(java.lang.String maritalStatus) {
   this.maritalStatus = maritalStatus;
}
  public int getDmvPoints() {
   return dmvPoints;
}
  public void setDmvPoints(int dmvPoints) {
   this.dmvPoints = dmvPoints;
}

}