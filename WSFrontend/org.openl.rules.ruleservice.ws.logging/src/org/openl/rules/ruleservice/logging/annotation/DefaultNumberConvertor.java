package org.openl.rules.ruleservice.logging.annotation;

import org.openl.rules.ruleservice.logging.Convertor;

public final class DefaultNumberConvertor implements Convertor<Object, Long> {
    @Override
    public Long convert(Object value) {
        throw new UnsupportedOperationException();
    }
}
