package org.openl.rules.data;

public class Type2 {

    private String name;
    private Type1[] types;

    public Type2() {
        super();
    }

    public Type2(String name, Type1[] types) {
        super();
        this.name = name;
        this.types = types;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Type1[] getTypes() {
        return types;
    }

    public void setTypes(Type1[] types) {
        this.types = types;
    }

}
