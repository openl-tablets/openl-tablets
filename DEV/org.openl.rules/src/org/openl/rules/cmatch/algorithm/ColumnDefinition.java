package org.openl.rules.cmatch.algorithm;

public class ColumnDefinition {
    private final String name;
    private final boolean isMultipleValueAllowed;

    public ColumnDefinition(String name, boolean isMultipleValueAllowed) {
        this.name = name;
        this.isMultipleValueAllowed = isMultipleValueAllowed;
    }

    public String getName() {
        return name;
    }

    public boolean isMultipleValueAllowed() {
        return isMultipleValueAllowed;
    }
}
