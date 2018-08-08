package org.openl.rules.webstudio.web.test.export;

public class A {
    private String name;
    private Integer[] values;

    public A(String name, Integer... values) {
        this.name = name;
        this.values = values;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer[] getValues() {
        return values;
    }

    public void setValues(Integer[] values) {
        this.values = values;
    }
}
