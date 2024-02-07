package org.openl.rules.serialization.spr;

public class UpperCamelCaseStrategy extends SpreadsheetResultBeanPropertyNamingStrategyBase {
    @Override
    public String transform(String name) {
        return toUpperCamelCase(name);
    }

    @Override
    public String transform(String column, String row) {
        return toUpperCamelCase(column) + toUpperCamelCase(row);
    }
}
