package org.openl.rules.convertor;

import java.util.Locale;

/**
 * Converts {@link String} representation of language tag to {@link Locale}
 */
class String2LocaleConverter implements IString2DataConvertor<Locale> {

    @Override
    public Locale parse(String data, String format) {
        if (data == null) {
            return null;
        }
        return Locale.forLanguageTag(data);
    }
}
