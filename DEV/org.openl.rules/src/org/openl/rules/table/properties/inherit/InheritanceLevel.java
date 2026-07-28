package org.openl.rules.table.properties.inherit;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum InheritanceLevel {
    GLOBAL("Global"),
    PROJECT("Project"),
    FOLDER("Folder"),
    EXTERNAL("External"),
    FILE("File"),
    MODULE("Module"),
    CATEGORY("Category"),
    TABLE("Table");

    @Getter
    private final String displayName;

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
