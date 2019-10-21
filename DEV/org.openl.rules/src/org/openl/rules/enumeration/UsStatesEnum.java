package org.openl.rules.enumeration;

public enum UsStatesEnum {

    AL("Alabama"),
    AK("Alaska"),
    AZ("Arizona"),
    AR("Arkansas"),
    CA("California"),
    CO("Colorado"),
    CT("Connecticut"),
    DC("District of Columbia"),
    DE("Delaware"),
    FL("Florida"),
    GA("Georgia"),
    HI("Hawaii"),
    ID("Idaho"),
    IL("Illinois"),
    IN("Indiana"),
    IA("Iowa"),
    KS("Kansas"),
    KY("Kentucky"),
    LA("Louisiana"),
    ME("Maine"),
    MD("Maryland"),
    MA("Massuchusets"),
    MI("Michigan"),
    MN("Minnesota"),
    MS("Mississipi"),
    MO("Missouri"),
    MT("Montana"),
    NE("Nebraska"),
    NV("Nevada"),
    NH("New Hampshire"),
    NJ("New Jersey"),
    NM("New Mexico"),
    NY("New York"),
    NC("North California"),
    ND("North Dakota"),
    OH("Ohio"),
    OK("Oklahoma"),
    OR("Oregon"),
    PA("Pennsylvania"),
    PR("Puerto Rico"),
    RI("Rhode Island"),
    SC("South California"),
    SD("South Dakota"),
    TN("Tennessee"),
    TX("Texas"),
    UT("Utah"),
    VT("Vermont"),
    VA("Virginia"),
    WA("Washington"),
    WV("West Virginia"),
    WI("Wisconsin"),
    WY("Wyoming");

    private final String displayName;

    private UsStatesEnum (String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static UsStatesEnum fromString(String displayName) {
        for (UsStatesEnum v : UsStatesEnum.values()) {
            if (displayName.equalsIgnoreCase(v.displayName)) {
                return v;
            }
        }

        throw new IllegalArgumentException("No constant with displayName '" + displayName + "' is found.");
    }
}