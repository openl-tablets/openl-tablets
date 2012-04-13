package com.exigen.le.collections.departament;

public class Person {
	String name;
	Double[] salary;
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return the salary
	 */
	public Double[] getSalary() {
		return salary;
	}
	/**
	 * @param salary the salary to set
	 */
	public void setSalary(Double[] salary) {
		this.salary = salary;
	}
	public Double getSalary(int index) {
		return salary[index];
	}
	public void setSalary(int index,Double value) {
		 salary[index]=value;
	}

}
