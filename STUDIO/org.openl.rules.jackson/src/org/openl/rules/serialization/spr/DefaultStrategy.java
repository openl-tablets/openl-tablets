package org.openl.rules.serialization.spr;

import org.openl.util.JavaKeywordUtils;

public class DefaultStrategy extends SpreadsheetResultBeanPropertyNamingStrategyBase {
    @Override
    public String transform(String name) {
        return JavaKeywordUtils.toJavaIdentifier(name);
    }

    @Override
    public String transform(String column, String row) {
        return transform(column) + "_" + transform(row);
    }
}
