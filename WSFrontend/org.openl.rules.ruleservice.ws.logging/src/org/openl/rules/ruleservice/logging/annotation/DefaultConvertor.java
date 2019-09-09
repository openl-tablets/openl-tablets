package org.openl.rules.ruleservice.logging.annotation;

import org.openl.rules.ruleservice.logging.Convertor;

public final class DefaultConvertor implements Convertor<Object, Object> {
    @Override
    public Object convert(Object value) {
        throw new UnsupportedOperationException();
    }
}
