package org.openl.rules.ruleservice.storelogdata.annotation;

import java.util.Date;

import org.openl.rules.ruleservice.storelogdata.Convertor;

public final class DefaultDateConvertor implements Convertor<Object, Date> {
    @Override
    public Date convert(Object value) {
        throw new UnsupportedOperationException();
    }
}
