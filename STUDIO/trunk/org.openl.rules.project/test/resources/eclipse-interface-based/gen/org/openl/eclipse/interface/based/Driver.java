/*
 * This class has been generated. Do not change it. 
*/

package org.openl.eclipse.interface.based;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import java.lang.String;
import org.apache.commons.lang.ArrayUtils;

public class Driver{
  protected int age = 0;

  protected java.lang.String gender;

  protected boolean hadTraining = false;

  protected java.lang.String maritalStatus;

  protected java.lang.String name;

  protected int numAccidents = 0;

  protected int numDUI = 0;

  protected int numMovingViolations = 0;

  protected java.lang.String state;



public Driver() {
    super();
}

public Driver(String name, String gender, int age, String maritalStatus, String state, int numAccidents, int numMovingViolations, int numDUI, boolean hadTraining) {
    super();
    this.name = name;
    this.gender = gender;
    this.age = age;
    this.maritalStatus = maritalStatus;
    this.state = state;
    this.numAccidents = numAccidents;
    this.numMovingViolations = numMovingViolations;
    this.numDUI = numDUI;
    this.hadTraining = hadTraining;
}

public boolean equals(Object obj) {
    EqualsBuilder builder = new EqualsBuilder();
    if (!(obj instanceof Driver)) {;
        return false;
    }
    Driver another = (Driver)obj;
    builder.append(another.getName(),getName());
    builder.append(another.getGender(),getGender());
    builder.append(another.getAge(),getAge());
    builder.append(another.getMaritalStatus(),getMaritalStatus());
    builder.append(another.getState(),getState());
    builder.append(another.getNumAccidents(),getNumAccidents());
    builder.append(another.getNumMovingViolations(),getNumMovingViolations());
    builder.append(another.getNumDUI(),getNumDUI());
    builder.append(another.getHadTraining(),getHadTraining());
    return builder.isEquals();
}
  public int getAge() {
   return age;
}
  public java.lang.String getGender() {
   return gender;
}
  public boolean getHadTraining() {
   return hadTraining;
}
  public java.lang.String getMaritalStatus() {
   return maritalStatus;
}
  public java.lang.String getName() {
   return name;
}
  public int getNumAccidents() {
   return numAccidents;
}
  public int getNumDUI() {
   return numDUI;
}
  public int getNumMovingViolations() {
   return numMovingViolations;
}
  public java.lang.String getState() {
   return state;
}

public int hashCode() {
    HashCodeBuilder builder = new HashCodeBuilder();
    builder.append(getName());
    builder.append(getGender());
    builder.append(getAge());
    builder.append(getMaritalStatus());
    builder.append(getState());
    builder.append(getNumAccidents());
    builder.append(getNumMovingViolations());
    builder.append(getNumDUI());
    builder.append(getHadTraining());
    return builder.toHashCode();
}
  public void setAge(int age) {
   this.age = age;
}
  public void setGender(java.lang.String gender) {
   this.gender = gender;
}
  public void setHadTraining(boolean hadTraining) {
   this.hadTraining = hadTraining;
}
  public void setMaritalStatus(java.lang.String maritalStatus) {
   this.maritalStatus = maritalStatus;
}
  public void setName(java.lang.String name) {
   this.name = name;
}
  public void setNumAccidents(int numAccidents) {
   this.numAccidents = numAccidents;
}
  public void setNumDUI(int numDUI) {
   this.numDUI = numDUI;
}
  public void setNumMovingViolations(int numMovingViolations) {
   this.numMovingViolations = numMovingViolations;
}
  public void setState(java.lang.String state) {
   this.state = state;
}

public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Driver {");
    builder.append(" name=");
    builder.append(getName());
    builder.append(" gender=");
    builder.append(getGender());
    builder.append(" age=");
    builder.append(getAge());
    builder.append(" maritalStatus=");
    builder.append(getMaritalStatus());
    builder.append(" state=");
    builder.append(getState());
    builder.append(" numAccidents=");
    builder.append(getNumAccidents());
    builder.append(" numMovingViolations=");
    builder.append(getNumMovingViolations());
    builder.append(" numDUI=");
    builder.append(getNumDUI());
    builder.append(" hadTraining=");
    builder.append(getHadTraining());
    builder.append(" }");
    return builder.toString();
}

}