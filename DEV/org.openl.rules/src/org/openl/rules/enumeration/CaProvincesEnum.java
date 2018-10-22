package org.openl.rules.enumeration;

public enum CaProvincesEnum {

    AB("Alberta"),
    BC("Colombie-Britannique"),
    PE("Île-du-Prince-Édouard"),
    MB("Manitoba"),
    NB("Nouveau-Brunswick"),
    NS("Nouvelle-Écosse"),
    NU("Nunavut"),
    ON("Ontario"),
    QC("Québec"),
    SK("Saskatchewan"),
    NL("Terre-Neuve-et-Labrador"),
    YT("Yukon"),
    NT("Territoires du Nord-Ouest");

    private final String displayName;

    private CaProvincesEnum (String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static CaProvincesEnum fromString(String displayName) {
        for (CaProvincesEnum v : CaProvincesEnum.values()) {
            if (displayName.equalsIgnoreCase(v.displayName)) {
                return v;
            }
        }

        throw new IllegalArgumentException("No constant with displayName " + displayName + " found");
    }
}