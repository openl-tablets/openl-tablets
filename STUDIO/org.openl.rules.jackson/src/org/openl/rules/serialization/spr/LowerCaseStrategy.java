package org.openl.rules.serialization.spr;

/*-
 * #%L
 * OpenL - STUDIO - Jackson
 * %%
 * Copyright (C) 2016 - 2020 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */

public class LowerCaseStrategy extends SpreadsheetResultBeanPropertyNamingStrategyBase {
    @Override
    protected String transform(String name) {
        if (name == null || name.length() == 0) {
            return name;
        }
        return name.toLowerCase();
    }

    @Override
    protected String transform(String column, String row) {
        return transform(column + row);
    }
}
