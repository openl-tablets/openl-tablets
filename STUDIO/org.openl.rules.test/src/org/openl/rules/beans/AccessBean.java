package org.openl.rules.beans;

public class AccessBean {
    public static final String instance = "instance";
    public static final String staticField = "staticField";
    public static final String getId = "getId";
    public static final String id = "id";
    public final String name = "name";
    private String privateField;

    public String getInstance() {
        return "getInstance()";
    }

    public String getField() {
        return "getField()";
    }

    public String getId() {
        return "getId()";
    }

    public String id() {
        return "id()";
    }

    public static String name() {
        return "static name()";
    }

    public String getPrivateField() {
        return privateField;
    }

    public void setPrivateField(String privateField) {
        this.privateField = privateField;
    }

    public static void main(String[] args) {
        AccessBean s = null;
        System.out.println(s.name());
    }
}
