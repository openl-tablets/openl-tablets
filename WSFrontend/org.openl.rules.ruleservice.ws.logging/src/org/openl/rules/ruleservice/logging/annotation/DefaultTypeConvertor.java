package org.openl.rules.ruleservice.logging.annotation;

import org.openl.rules.ruleservice.logging.TypeConvertor;

public final class DefaultTypeConvertor implements TypeConvertor<Object, Object> {
    @Override
    public Object convert(Object value) {
        return value;
    }
}
