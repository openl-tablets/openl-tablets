package org.openl.rules.serialization.spr;

public class LowerCamelCaseStrategy extends SpreadsheetResultBeanPropertyNamingStrategyBase {
    @Override
    protected String transform(String name) {
        return toLowerCamelCase(name);
    }

    @Override
    protected String transform(String column, String row) {
        return toLowerCamelCase(column) + toUpperCamelCase(row);
    }
}
