package org.openl.rules.ruleservice.logging.annotation;

import java.util.Date;

import org.openl.rules.ruleservice.logging.TypeConvertor;

public final class DefaultDateConvertor implements TypeConvertor<Date, Object> {
    @Override
    public Object convert(Date value) {
        return value;
    }
}
