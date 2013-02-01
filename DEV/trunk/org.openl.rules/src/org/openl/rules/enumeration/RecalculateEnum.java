package org.openl.rules.enumeration;

public enum RecalculateEnum {
    ALWAYS("Always"),
    NEVER("Never"),
    ANALYZE("Analyze");

    private final String displayName;

    private RecalculateEnum(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
