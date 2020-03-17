package org.openl.rules.enumeration;

public enum OriginsEnum {

    Base("Base"),
    Deviation("Deviation");

    private final String displayName;

    private OriginsEnum(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static OriginsEnum fromString(String displayName) {
        for (OriginsEnum v : OriginsEnum.values()) {
            if (displayName.equalsIgnoreCase(v.displayName)) {
                return v;
            }
        }

        throw new IllegalArgumentException(String.format("No constant with displayName '%s' is found.", displayName));
    }
}
