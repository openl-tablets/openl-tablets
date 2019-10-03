package org.openl.rules.ruleservice.storelogdata.annotation;

import org.openl.rules.ruleservice.storelogdata.Converter;

public final class DefaultStringConverter implements Converter<Object, String> {
    @Override
    public String apply(Object value) {
        throw new UnsupportedOperationException();
    }
}
