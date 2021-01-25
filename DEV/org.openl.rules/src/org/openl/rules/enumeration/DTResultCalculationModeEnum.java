package org.openl.rules.enumeration;

public enum DTResultCalculationModeEnum {
    AVOID_EMPTY("Avoid empty results and empty cells in the table ACTION and RET columns"),
    ALLOW_EMPTY("Take into account empty results and empty cells in the table ACTION and RET columns");

    private final String displayName;

    DTResultCalculationModeEnum(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static DTResultCalculationModeEnum fromString(String displayName) {
        for (DTResultCalculationModeEnum v : DTResultCalculationModeEnum.values()) {
            if (displayName.equalsIgnoreCase(v.displayName)) {
                return v;
            }
        }

        throw new IllegalArgumentException(String.format("No constant with displayName '%s' is found.", displayName));
    }
}
