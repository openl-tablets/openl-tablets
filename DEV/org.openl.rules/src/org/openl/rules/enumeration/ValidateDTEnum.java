package org.openl.rules.enumeration;

public enum ValidateDTEnum {

    ON("On"),
    OFF("Off");

    private final String displayName;

    private ValidateDTEnum (String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static ValidateDTEnum fromString(String displayName) {
        for (ValidateDTEnum v : ValidateDTEnum.values()) {
            if (displayName.equalsIgnoreCase(v.displayName)) {
                return v;
            }
        }

        throw new IllegalArgumentException(String.format("No constant with displayName '%s' is found.", displayName));
    }
}