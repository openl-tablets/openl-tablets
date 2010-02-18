package org.openl.rules.table.properties.inherit;

public enum InheritanceLevel {

    MODULE("Module"),
    CATEGORY("Category"), 
    TABLE("Table");

    private String displayName;

    InheritanceLevel(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

}