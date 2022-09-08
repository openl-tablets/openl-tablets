package org.openl.rules.serialization.spr;

public class LowerCamelCaseStrategy extends SpreadsheetResultBeanPropertyNamingStrategyBase {
    @Override
    public String transform(String name) {
        return toLowerCamelCase(name);
    }

    @Override
    public String transform(String column, String row) {
        return toLowerCamelCase(column) + toUpperCamelCase(row);
    }
}
