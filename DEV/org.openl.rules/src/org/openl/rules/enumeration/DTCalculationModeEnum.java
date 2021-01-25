package org.openl.rules.enumeration;

public enum DTCalculationModeEnum {
    AVOID_EMPTY_RESULT("Avoid empty results and empty cells in the table ACTION and RET columns"),
    ALLOW_EMPTY_RESULT("Take into account empty results and empty cells in the table ACTION and RET columns");

    private final String displayName;

    DTCalculationModeEnum(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static DTCalculationModeEnum fromString(String displayName) {
        for (DTCalculationModeEnum v : DTCalculationModeEnum.values()) {
            if (displayName.equalsIgnoreCase(v.displayName)) {
                return v;
            }
        }

        throw new IllegalArgumentException(String.format("No constant with displayName '%s' is found.", displayName));
    }
}
