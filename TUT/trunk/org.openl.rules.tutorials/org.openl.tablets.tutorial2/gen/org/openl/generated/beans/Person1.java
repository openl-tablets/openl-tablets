/*
 * This class has been generated. Do not change it. 
*/

package org.openl.generated.beans;

import java.lang.String;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import java.util.Date;
import org.apache.commons.lang.ArrayUtils;

public class Person1{
  protected java.lang.String ssn;

  protected java.util.Date dob;

  protected java.lang.String gender;

  protected java.lang.String maritalStatus;

  protected java.lang.String name;



public Person1() {
    super();
}

public Person1(String name, String ssn, Date dob, String gender, String maritalStatus) {
    super();
    this.name = name;
    this.ssn = ssn;
    this.dob = dob;
    this.gender = gender;
    this.maritalStatus = maritalStatus;
}
  public java.lang.String getSsn() {
   return ssn;
}
  public void setSsn(java.lang.String ssn) {
   this.ssn = ssn;
}
  public java.util.Date getDob() {
   return dob;
}
  public void setDob(java.util.Date dob) {
   this.dob = dob;
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

public int hashCode() {
    HashCodeBuilder builder = new HashCodeBuilder();
    builder.append(getName());
    builder.append(getSsn());
    builder.append(getDob());
    builder.append(getGender());
    builder.append(getMaritalStatus());
    return builder.toHashCode();
}

public boolean equals(Object obj) {
    EqualsBuilder builder = new EqualsBuilder();
    if (!(obj instanceof Person1)) {;
        return false;
    }
    Person1 another = (Person1)obj;
    builder.append(another.getName(),getName());
    builder.append(another.getSsn(),getSsn());
    builder.append(another.getDob(),getDob());
    builder.append(another.getGender(),getGender());
    builder.append(another.getMaritalStatus(),getMaritalStatus());
    return builder.isEquals();
}
  public java.lang.String getName() {
   return name;
}

public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Person1 {");
    builder.append(" name=");
    builder.append(getName());
    builder.append(" ssn=");
    builder.append(getSsn());
    builder.append(" dob=");
    builder.append(getDob());
    builder.append(" gender=");
    builder.append(getGender());
    builder.append(" maritalStatus=");
    builder.append(getMaritalStatus());
    builder.append(" }");
    return builder.toString();
}
  public void setName(java.lang.String name) {
   this.name = name;
}

}