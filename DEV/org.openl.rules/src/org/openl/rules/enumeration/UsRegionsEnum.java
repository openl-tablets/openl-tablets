package org.openl.rules.enumeration;

public enum UsRegionsEnum {

    MW("Midwest"),
    NE("Northeast"),
    SE("Southeast"),
    SW("Southwest"),
    W("West");

    private final String displayName;

    private UsRegionsEnum (String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static UsRegionsEnum fromString(String displayName) {
        for (UsRegionsEnum v : UsRegionsEnum.values()) {
            if (displayName.equalsIgnoreCase(v.displayName)) {
                return v;
            }
        }

        throw new IllegalArgumentException("No constant with displayName " + displayName + " found");
    }
}