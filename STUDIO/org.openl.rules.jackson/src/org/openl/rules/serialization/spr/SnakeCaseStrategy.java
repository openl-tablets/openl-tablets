package org.openl.rules.serialization.spr;

public class SnakeCaseStrategy extends SpreadsheetResultBeanPropertyNamingStrategyBase {
    @Override
    protected String transform(String name) {
        if (name == null || name.length() == 0) {
            return name;
        }
        return name.toLowerCase();
    }

    @Override
    protected String transform(String column, String row) {
        return transform(column) + "_" + transform(row);
    }
}
