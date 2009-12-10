package org.openl;

public class Severity {

    private String name;
    private int ordinal;
    
    public Severity(String name, int ordinal) {
        this.name = name;
        this.ordinal = ordinal;
    }

    public String getName() {
        return name;
    }

    public int getOrdinal() {
        return ordinal;
    }
}
