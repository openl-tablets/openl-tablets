package org.openl.rules.enumeration;

public enum CountriesEnum {

    AE("United Arab Emirates"),
    AL("Albania"),
    AR("Argentina"),
    AS("American Samoa"),
    AT("Austria"),
    AU("Australia"),
    BA("Bosnia And Herzegowina"),
    BE("Belgium"),
    BG("Bulgaria"),
    BH("Bahrain"),
    BO("Bolivia"),
    BR("Brazil"),
    BY("Belarus"),
    CA("Canada"),
    CH("Switzerland"),
    CK("Cook Islands"),
    CL("Chile"),
    CN("China"),
    CO("Colombia"),
    CR("Costa Rica"),
    CS("Serbia And Montenegro"),
    CY("Cyprus"),
    CZ("Czech Republic"),
    DE("Germany"),
    DK("Denmark"),
    DO("Dominican Republic"),
    DZ("Algeria"),
    EC("Ecuador"),
    EE("Estonia"),
    EG("Egypt"),
    ES("Spain"),
    FI("Finland"),
    FJ("Fiji"),
    FR("France"),
    GB("United Kingdom"),
    GR("Greece"),
    GT("Guatemala"),
    HK("Hong Kong"),
    HN("Honduras"),
    HR("Croatia"),
    HU("Hungary"),
    ID("Indonesia"),
    IE("Ireland"),
    IL("Israel"),
    IN("India"),
    IQ("Iraq"),
    IS("Iceland"),
    IT("Italy"),
    JO("Jordan"),
    JP("Japan"),
    KW("Kuwait"),
    LB("Lebanon"),
    LT("Lithuania"),
    LU("Luxembourg"),
    LV("Latvia"),
    MA("Morocco"),
    MK("Macedonia"),
    MT("Malta"),
    MX("Mexico"),
    MY("Malaysia"),
    NI("Nicaragua"),
    NL("Netherlands"),
    NO("Norway"),
    NZ("New Zealand"),
    OM("Oman"),
    PA("Panama"),
    PE("Peru"),
    PG("Papua New Guinea"),
    PH("Philippines"),
    PL("Poland"),
    PR("Puerto Rico"),
    PT("Portugal"),
    PY("Paraguay"),
    QA("Qatar"),
    RO("Romania"),
    RU("Russian Federation"),
    SA("Saudi Arabia"),
    SB("Solomon Islands"),
    SD("Sudan"),
    SE("Sweden"),
    SG("Singapore"),
    SI("Slovenia"),
    SK("Slovakia"),
    SV("El Salvador"),
    SY("Syrian Arab Republic"),
    TH("Thailand"),
    TN("Tunisia"),
    TO("Tonga"),
    TR("Turkey"),
    TW("Taiwan"),
    UA("Ukraine"),
    US("United States"),
    UY("Uruguay"),
    VE("Venezuela"),
    VN("Viet Nam"),
    VU("Vanuatu"),
    WS("Samoa"),
    YE("Yemen"),
    ZA("South Africa");

    private final String displayName;

    CountriesEnum(String displayName) {
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

        throw new IllegalArgumentException(String.format("No constant with displayName '%s' is found.", displayName));
    }
}
