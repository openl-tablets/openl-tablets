package org.openl.rules.beans;

class AccessBean {
    public static final String instance = "instance";
    public static final String staticField = "staticField";
    public static final String getId = "getId";
    public static final String id = "id";
    public final String name = "name";
    private String privateField;

    String getInstance() {
        return "getInstance()";
    }

    String getField() {
        return "getField()";
    }

    String getId() {
        return "getId()";
    }

    String id() {
        return "id()";
    }

    static String name() {
        return "static name()";
    }

    String getPrivateField() {
        return privateField;
    }

    void setPrivateField(String privateField) {
        this.privateField = privateField;
    }

    static void main(String[] args) {
        AccessBean s = null;
        System.out.println(s.name());
    }
}
