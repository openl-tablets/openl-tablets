package org.openl.rules.serialization.spr;

import java.util.Locale;

public class LowerCaseStrategy extends SpreadsheetResultBeanPropertyNamingStrategyBase {
    @Override
    public String transform(String name) {
        if (name == null || name.length() == 0) {
            return name;
        }
        return name.toLowerCase(Locale.ENGLISH);
    }

    @Override
    public String transform(String column, String row) {
        return transform(column + row);
    }
}
