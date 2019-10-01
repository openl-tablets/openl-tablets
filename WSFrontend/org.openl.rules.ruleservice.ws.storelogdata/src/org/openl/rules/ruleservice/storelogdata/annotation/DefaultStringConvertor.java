package org.openl.rules.ruleservice.storelogdata.annotation;

import org.openl.rules.ruleservice.storelogdata.Convertor;

public final class DefaultStringConvertor implements Convertor<Object, String> {
    @Override
    public String convert(Object value) {
        throw new UnsupportedOperationException();
    }
}
