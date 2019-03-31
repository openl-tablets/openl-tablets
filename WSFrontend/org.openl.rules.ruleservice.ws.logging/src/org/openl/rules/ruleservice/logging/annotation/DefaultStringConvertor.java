package org.openl.rules.ruleservice.logging.annotation;

import org.openl.rules.ruleservice.logging.TypeConvertor;

public final class DefaultStringConvertor implements TypeConvertor<String, Object> {
    @Override
    public Object convert(String value) {
        return value;
    }
}
