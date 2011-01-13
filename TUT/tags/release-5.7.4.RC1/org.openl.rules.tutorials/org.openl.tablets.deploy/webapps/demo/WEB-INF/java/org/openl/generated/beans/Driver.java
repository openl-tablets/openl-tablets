/**
 *  OpenL Tablets,  2006
 *  https://sourceforge.net/projects/openl-tablets/
 */
package org.openl.generated.beans;

import org.openl.base.NamedThing;
public class Driver extends NamedThing
{

	String gender;
	int age;
	String maritalStatus;
	String state;
	int numAccidents;
	int numMovingViolations;
	int numDUI;

	boolean hadTraining;






	/**
	 * @return Returns the age.
	 */
	public int getAge() {
		return age;
	}

	/**
	 * @param age The age to set.
	 */
	public void setAge(int age) {
		this.age = age;
	}

	/**
	 * @return Returns the gender.
	 */
	public String getGender() {
		return gender;
	}

	/**
	 * @param gender The gender to set.
	 */
	public void setGender(String gender) {
		this.gender = gender;
	}

	/**
	 * @return Returns the hadTraining.
	 */
	public boolean isHadTraining() {
		return hadTraining;
	}

	/**
	 * @param hadTraining The hadTraining to set.
	 */
	public void setHadTraining(boolean hadTraining) {
		this.hadTraining = hadTraining;
	}

	/**
	 * @return Returns the maritalStatus.
	 */
	public String getMaritalStatus() {
		return maritalStatus;
	}

	/**
	 * @param maritalStatus The maritalStatus to set.
	 */
	public void setMaritalStatus(String maritalStatus) {
		this.maritalStatus = maritalStatus;
	}

	/**
	 * @return Returns the numAccidents.
	 */
	public int getNumAccidents() {
		return numAccidents;
	}

	/**
	 * @param accidents The numAccidents to set.
	 */
	public void setNumAccidents(int accidents) {
		numAccidents = accidents;
	}

	/**
	 * @return Returns the nDUI.
	 */
	public int getNumDUI() {
		return numDUI;
	}

	/**
	 * @param ndui The nDUI to set.
	 */
	public void setNumDUI(int ndui) {
		numDUI = ndui;
	}

	/**
	 * @return Returns the numMovingViolations.
	 */
	public int getNumMovingViolations() {
		return numMovingViolations;
	}

	/**
	 * @param movingViolations The numMovingViolations to set.
	 */
	public void setNumMovingViolations(int movingViolations) {
		numMovingViolations = movingViolations;
	}

	/**
	 * @return Returns the state.
	 */
	public String getState() {
		return state;
	}

	/**
	 * @param state The state to set.
	 */
	public void setState(String state) {
		this.state = state;
	}

}
