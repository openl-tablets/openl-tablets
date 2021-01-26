package org.openl.rules.enumeration;

public enum DTEmptyResultProcessingEnum {

    SKIP("skip"),
    RETURN("return");

    private final String displayName;

    DTEmptyResultProcessingEnum(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static DTEmptyResultProcessingEnum fromString(String displayName) {
        for (DTEmptyResultProcessingEnum v : DTEmptyResultProcessingEnum.values()) {
            if (displayName.equalsIgnoreCase(v.displayName)) {
                return v;
            }
        }

        throw new IllegalArgumentException(String.format("No constant with displayName '%s' is found.", displayName));
    }
}
