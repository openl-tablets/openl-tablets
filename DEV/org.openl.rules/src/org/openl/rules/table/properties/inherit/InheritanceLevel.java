package org.openl.rules.table.properties.inherit;

public enum InheritanceLevel {
    PROJECT("Project"),
    FOLDER("Folder"),
    EXTERNAL("External"),
    FILE("File"),
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

    public static InheritanceLevel getEnumByValue(String value) {
        for (InheritanceLevel level : InheritanceLevel.values()) {
            if (level.getDisplayName().equals(value)) {
                return level;
            }
        }
        return null;
    }

}