package org.openl.rules.enumeration;

public enum RecalculateEnum {

    ALWAYS("Always"),
    NEVER("Never"),
    ANALYZE("Analyze");

    private final String displayName;

    RecalculateEnum(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static RecalculateEnum fromString(String displayName) {
        for (RecalculateEnum v : RecalculateEnum.values()) {
            if (displayName.equalsIgnoreCase(v.displayName)) {
                return v;
            }
        }

        throw new IllegalArgumentException("No constant with displayName '%s' is found.".formatted(displayName));
    }
}
