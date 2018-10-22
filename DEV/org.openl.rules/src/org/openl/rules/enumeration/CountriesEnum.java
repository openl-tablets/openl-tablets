package org.openl.rules.enumeration;

public enum CountriesEnum {

    AL("Albania"),
    DZ("Algeria"),
    AR("Argentina"),
    AU("Australia"),
    AT("Austria"),
    BH("Bahrain"),
    BY("Belarus"),
    BE("Belgium"),
    BO("Bolivia"),
    BA("Bosnia And Herzegowina"),
    BR("Brazil"),
    BG("Bulgaria"),
    CA("Canada"),
    CL("Chile"),
    CN("China"),
    CO("Colombia"),
    CR("Costa Rica"),
    HR("Croatia"),
    CY("Cyprus"),
    CZ("Czech Republic"),
    DK("Denmark"),
    DO("Dominican Republic"),
    EC("Ecuador"),
    EG("Egypt"),
    SV("El Salvador"),
    EE("Estonia"),
    FI("Finland"),
    FR("France"),
    DE("Germany"),
    GR("Greece"),
    GT("Guatemala"),
    HN("Honduras"),
    HK("Hong Kong"),
    HU("Hungary"),
    IS("Iceland"),
    IN("India"),
    ID("Indonesia"),
    IQ("Iraq"),
    IE("Ireland"),
    IL("Israel"),
    IT("Italy"),
    JP("Japan"),
    JO("Jordan"),
    KW("Kuwait"),
    LV("Latvia"),
    LB("Lebanon"),
    LT("Lithuania"),
    LU("Luxembourg"),
    MK("Macedonia"),
    MY("Malaysia"),
    MT("Malta"),
    MX("Mexico"),
    MA("Morocco"),
    NL("Netherlands"),
    NZ("New Zealand"),
    NI("Nicaragua"),
    NO("Norway"),
    OM("Oman"),
    PA("Panama"),
    PY("Paraguay"),
    PE("Peru"),
    PH("Philippines"),
    PL("Poland"),
    PT("Portugal"),
    PR("Puerto Rico"),
    QA("Qatar"),
    RO("Romania"),
    RU("Russian Federation"),
    SA("Saudi Arabia"),
    CS("Serbia And Montenegro"),
    SG("Singapore"),
    SK("Slovakia"),
    SI("Slovenia"),
    ZA("South Africa"),
    ES("Spain"),
    SD("Sudan"),
    SE("Sweden"),
    CH("Switzerland"),
    SY("Syrian Arab Republic"),
    TW("Taiwan"),
    TH("Thailand"),
    TN("Tunisia"),
    TR("Turkey"),
    UA("Ukraine"),
    AE("United Arab Emirates"),
    GB("United Kingdom"),
    US("United States"),
    UY("Uruguay"),
    VE("Venezuela"),
    VN("Viet Nam"),
    YE("Yemen");

    private final String displayName;

    private CountriesEnum (String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static CountriesEnum fromString(String displayName) {
        for (CountriesEnum v : CountriesEnum.values()) {
            if (displayName.equalsIgnoreCase(v.displayName)) {
                return v;
            }
        }

        throw new IllegalArgumentException("No constant with displayName " + displayName + " found");
    }
}