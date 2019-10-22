package org.openl.rules.enumeration;

public enum RegionsOperationEnum {

    QC("Québec"),
    HQ("Hors Québec");

    private final String displayName;

    private RegionsOperationEnum(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static RegionsOperationEnum fromString(String displayName) {
        for (RegionsOperationEnum v : RegionsOperationEnum.values()) {
            if (displayName.equalsIgnoreCase(v.displayName)) {
                return v;
            }
        }

        throw new IllegalArgumentException(String.format("No constant with displayName %s found", displayName));
    }
}