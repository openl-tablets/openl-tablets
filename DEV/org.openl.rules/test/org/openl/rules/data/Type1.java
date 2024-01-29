package org.openl.rules.data;

public class Type1 {

    private String name;
    private String value;

    public Type1() {
        super();
    }

    public Type1(String name, String value) {
        super();
        this.name = name;
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setValue(String value) {
        this.value = value;
    }

}