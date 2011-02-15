package com.exigen.le.collections;

import com.exigen.le.collections.departament.Person;

public class Departament {
	String[] project;
	Person[] person;
	/**
	 * @return the project
	 */
	public String[] getProject() {
		return project;
	}
	/**
	 * @param project the project to set
	 */
	public void setProject(String[] project) {
		this.project = project;
	}
	/**
	 * @return the person
	 */
	public Person[] getPerson() {
		return person;
	}
	/**
	 * @param person the person to set
	 */
	public void setPerson(Person[] person) {
		this.person = person;
	}
	
	public String getProject(int index) {
		return project[index];
	}
	public void setProject(int index,String value) {
		 project[index]=value;
	}

	public Person getPerson(int index) {
		return person[index];
	}
	public void setPerson(int index,Person value) {
		 person[index]=value;
	}

}
