package org.openl.rules.ruleservice.storelogdata.annotation;

import org.openl.rules.ruleservice.storelogdata.Converter;

public final class DefaultNumberConverter implements Converter<Object, Long> {
    @Override
    public Long apply(Object value) {
        throw new UnsupportedOperationException();
    }
}
