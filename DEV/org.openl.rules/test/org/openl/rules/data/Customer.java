package org.openl.rules.data;

public class Customer {

    private String firstName;
    private String lastName;
    private int age;
    private String[] products;
    private String[] problems;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String[] getProducts() {
        return products;
    }

    public void setProducts(String[] products) {
        this.products = products;
    }

    public String[] getProblems() {
        return problems;
    }

    public void setProblems(String[] problems) {
        this.problems = problems;
    }

}
