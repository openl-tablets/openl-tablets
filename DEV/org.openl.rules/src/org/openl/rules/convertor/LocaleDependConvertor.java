package org.openl.rules.convertor;

import java.util.Locale;

class LocaleDependConvertor {
    
    private LocaleDependConvertor() {
    }

    private static final String LOCALE_COUNTRY = "US";
    private static final String LOCALE_LANG = "en";

    private static Locale locale = null;

    static Locale getLocale() {
        if (locale == null) {
            String country = System.getProperty("org.openl.locale.country");
            String lang = System.getProperty("org.openl.locale.lang");

            locale = new Locale(lang == null ? LOCALE_LANG : lang, country == null ? LOCALE_COUNTRY : country);
        }

        return locale;
    }
}
