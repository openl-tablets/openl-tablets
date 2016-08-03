package org.openl.rules.ruleservice.logging.annotation;

import org.openl.rules.ruleservice.logging.TypeConvertor;

public final class DefaultNumberConvertor implements TypeConvertor<Long, Object> {
    @Override
    public Object convert(Long value) {
        return value;
    }
}
