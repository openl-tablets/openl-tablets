package org.openl.rules.enumeration;

public enum CaRegionsEnum {

    QC("Québec"),
    HQ("Hors Québec");

    private final String displayName;

    private CaRegionsEnum (String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static CaRegionsEnum fromString(String displayName) {
        for (CaRegionsEnum v : CaRegionsEnum.values()) {
            if (displayName.equalsIgnoreCase(v.displayName)) {
                return v;
            }
        }

        throw new IllegalArgumentException(String.format("No constant with displayName '%s' is found.", displayName));
    }
}