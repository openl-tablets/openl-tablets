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

public class UpperCamelCaseStrategy extends SpreadsheetResultBeanPropertyNamingStrategyBase {
    @Override
    protected String transform(String name) {
        return toUpperCamelCase(name);
    }

    @Override
    protected String transform(String column, String row) {
        return toUpperCamelCase(column) + toUpperCamelCase(row);
    }
}
