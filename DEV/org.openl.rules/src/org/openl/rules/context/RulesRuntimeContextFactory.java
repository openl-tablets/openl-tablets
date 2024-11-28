package org.openl.rules.context;

import java.util.Locale;

public class RulesRuntimeContextFactory {

    private static final ThreadLocal<Locale> LOCALE_HOLDER = new ThreadLocal<>();

    public static void setLocale(Locale locale) {
        LOCALE_HOLDER.set(locale);
    }

    public static void removeLocale() {
        LOCALE_HOLDER.remove();
    }

    public static IRulesRuntimeContext buildRulesRuntimeContext() {
        DefaultRulesRuntimeContext rulesRuntimeContext = new DefaultRulesRuntimeContext();
        rulesRuntimeContext.setLocale(LOCALE_HOLDER.get());
        return rulesRuntimeContext;
    }
}
