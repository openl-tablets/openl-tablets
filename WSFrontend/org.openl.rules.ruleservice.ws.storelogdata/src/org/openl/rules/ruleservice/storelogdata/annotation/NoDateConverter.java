package org.openl.rules.ruleservice.storelogdata.annotation;

import java.time.ZonedDateTime;

import org.openl.rules.ruleservice.storelogdata.Converter;

public final class NoDateConverter implements Converter<ZonedDateTime, Object> {
    @Override
    public Object apply(ZonedDateTime value) {
        throw new UnsupportedOperationException();
    }
}
