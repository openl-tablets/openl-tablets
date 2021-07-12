package org.openl.rules.ruleservice.storelogdata.annotation;

import org.openl.rules.ruleservice.storelogdata.Converter;

public final class NoConverter implements Converter<Object, Object> {
    @Override
    public Object apply(Object value) {
        throw new UnsupportedOperationException();
    }
}
