/*
 * This class has been generated. Do not change it. 
*/

package org.openl.generated.test.beans;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class Driver {
    protected String name;

    protected String state;

    protected String gender;

    protected int age;

    protected String maritalStatus;

    protected int numAccidents;

    protected int numMovingViolations;

    protected int numDUI;

    protected boolean hadTraining;

    public Driver() {
        super();
    }

    public Driver(String name,
            String gender,
            int age,
            String maritalStatus,
            String state,
            int numAccidents,
            int numMovingViolations,
            int numDUI,
            boolean hadTraining) {
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

    public boolean equals(Object obj) {
        EqualsBuilder builder = new EqualsBuilder();
        if (!(obj instanceof Driver)) {
            return false;
        }
        Driver another = (Driver) obj;
        builder.append(another.getName(), getName());
        builder.append(another.getGender(), getGender());
        builder.append(another.getAge(), getAge());
        builder.append(another.getMaritalStatus(), getMaritalStatus());
        builder.append(another.getState(), getState());
        builder.append(another.getNumAccidents(), getNumAccidents());
        builder.append(another.getNumMovingViolations(), getNumMovingViolations());
        builder.append(another.getNumDUI(), getNumDUI());
        builder.append(another.getHadTraining(), getHadTraining());
        return builder.isEquals();
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return "Driver {" + " name=" + getName() + " gender=" + getGender() + " age=" + getAge() + " maritalStatus=" + getMaritalStatus() + " state=" + getState() + " numAccidents=" + getNumAccidents() + " numMovingViolations=" + getNumMovingViolations() + " numDUI=" + getNumDUI() + " hadTraining=" + getHadTraining() + " }";
    }

    public String getState() {
        return state;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getMaritalStatus() {
        return maritalStatus;
    }

    public void setMaritalStatus(String maritalStatus) {
        this.maritalStatus = maritalStatus;
    }

    public int getNumAccidents() {
        return numAccidents;
    }

    public void setNumAccidents(int numAccidents) {
        this.numAccidents = numAccidents;
    }

    public int getNumMovingViolations() {
        return numMovingViolations;
    }

    public void setNumMovingViolations(int numMovingViolations) {
        this.numMovingViolations = numMovingViolations;
    }

    public int getNumDUI() {
        return numDUI;
    }

    public void setNumDUI(int numDUI) {
        this.numDUI = numDUI;
    }

    public boolean getHadTraining() {
        return hadTraining;
    }

    public void setHadTraining(boolean hadTraining) {
        this.hadTraining = hadTraining;
    }

}
