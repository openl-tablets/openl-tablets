package org.openl.rules.enumeration;

public enum CurrenciesEnum {

    ALL("Albania, Leke"),
    DZD("Algeria, Dinars"),
    ARS("Argentina, Pesos"),
    AUD("Australia, Dollars"),
    BHD("Bahrain, Dinars"),
    BYR("Belarus, Rubles"),
    BOB("Bolivia, Bolivianos"),
    BAM("Bosnia and Herzegovina, Convertible Marka"),
    BRL("Brazil, Real"),
    CAD("Canada, Dollars"),
    CLP("Chile, Pesos"),
    CNY("China, Yuan Renminbi"),
    COP("Colombia, Pesos"),
    CRC("Costa Rica, Colones"),
    HRK("Croatia, Kuna"),
    CZK("Czech Republic, Koruny"),
    DKK("Denmark, Kroner"),
    DOP("Dominican Republic, Pesos"),
    EGP("Egypt, Pounds"),
    EUR("Euro"),
    GTQ("Guatemala, Quetzales"),
    HNL("Honduras, Lempiras"),
    HKD("Hong Kong, Dollars"),
    HUF("Hungary, Forint"),
    ISK("Iceland, Kronur"),
    INR("India, Rupees"),
    IDR("Indonesia, Rupiahs"),
    IQD("Iraq, Dinars"),
    ILS("Israel, New Shekels"),
    JPY("Japan, Yen"),
    JOD("Jordan, Dinars"),
    KWD("Kuwait, Dinars"),
    LVL("Latvia, Lati"),
    LBP("Lebanon, Pounds"),
    LTL("Lithuania, Litai"),
    MKD("Macedonia, Denars"),
    MYR("Malaysia, Ringgits"),
    MXN("Mexico, Pesos"),
    MAD("Morocco, Dirhams"),
    NZD("New Zealand, Dollars"),
    NIO("Nicaragua, Cordobas"),
    NOK("Norway, Kroner"),
    OMR("Oman, Rials"),
    PAB("Panama, Balboa"),
    PYG("Paraguay, Guarani"),
    PEN("Peru, Nuevos Soles"),
    PHP("Philippines, Pesos"),
    PLN("Poland, Zlotych"),
    QAR("Qatar, Rials"),
    RON("Romania, New Lei"),
    RUB("Russia, Rubles"),
    SAR("Saudi Arabia, Riyals"),
    RSD("Serbia, Dinars"),
    SGD("Singapore, Dollars"),
    ZAR("South Africa, Rand"),
    SDG("Sudan, Pounds"),
    SEK("Sweden, Kronor"),
    CHF("Switzerland, Francs"),
    SYP("Syria, Pounds"),
    TWD("Taiwan, New Dollars"),
    TND("Tunisia, Dinars"),
    TRY("Turkey, Lira"),
    UAH("Ukraine, Hryvnia"),
    AED("United Arab Emirates, Dirhams"),
    GBP("United Kingdom, Pounds"),
    USD("United States of America, Dollars"),
    UYU("Uruguay, Pesos"),
    VEF("Venezuela, Bolivares Fuertes"),
    VND("Viet Nam, Dong"),
    YER("Yemen, Rials");

    private final String displayName;

    private CurrenciesEnum (String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static CurrenciesEnum fromString(String displayName) {
        for (CurrenciesEnum v : CurrenciesEnum.values()) {
            if (displayName.equalsIgnoreCase(v.displayName)) {
                return v;
            }
        }

        throw new IllegalArgumentException("No constant with displayName '" + displayName + "' is found.");
    }
}