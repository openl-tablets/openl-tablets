package org.openl.rules.serialization.spr;

public class DefaultStrategy extends SpreadsheetResultBeanPropertyNamingStrategyBase {
    @Override
    public String transform(String name) {
        return name;
    }

    @Override
    public String transform(String column, String row) {
        return transform(column) + "_" + transform(row);
    }
}
