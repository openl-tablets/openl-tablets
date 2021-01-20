package org.openl.rules.serialization.spr;

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
