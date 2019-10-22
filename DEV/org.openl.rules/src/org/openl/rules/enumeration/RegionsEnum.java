package org.openl.rules.enumeration;

public enum RegionsEnum {

    NCSA("Americas"),
    EU("European Union"),
    EMEA("Europe; Middle East; Africa"),
    APJ("Asia Pacific; Japan");

    private final String displayName;

    private RegionsEnum (String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static RegionsEnum fromString(String displayName) {
        for (RegionsEnum v : RegionsEnum.values()) {
            if (displayName.equalsIgnoreCase(v.displayName)) {
                return v;
            }
        }

        throw new IllegalArgumentException(String.format("No constant with displayName '%s' is found.", displayName));
    }
}