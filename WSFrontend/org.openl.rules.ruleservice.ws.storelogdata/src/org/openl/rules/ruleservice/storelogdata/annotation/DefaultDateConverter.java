package org.openl.rules.ruleservice.storelogdata.annotation;

import java.util.Date;

import org.openl.rules.ruleservice.storelogdata.Converter;

public final class DefaultDateConverter implements Converter<Object, Date> {
    @Override
    public Date apply(Object value) {
        throw new UnsupportedOperationException();
    }
}
