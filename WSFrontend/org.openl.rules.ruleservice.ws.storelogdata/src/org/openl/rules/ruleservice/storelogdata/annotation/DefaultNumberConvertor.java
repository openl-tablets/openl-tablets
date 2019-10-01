package org.openl.rules.ruleservice.storelogdata.annotation;

import org.openl.rules.ruleservice.storelogdata.Convertor;

public final class DefaultNumberConvertor implements Convertor<Object, Long> {
    @Override
    public Long convert(Object value) {
        throw new UnsupportedOperationException();
    }
}
