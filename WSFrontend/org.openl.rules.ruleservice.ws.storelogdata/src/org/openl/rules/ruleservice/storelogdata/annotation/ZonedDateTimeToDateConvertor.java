package org.openl.rules.ruleservice.storelogdata.annotation;

import java.time.ZonedDateTime;
import java.util.Date;

import org.openl.rules.ruleservice.storelogdata.Converter;

public final class ZonedDateTimeToDateConvertor implements Converter<ZonedDateTime, Date> {
    @Override
    public Date apply(ZonedDateTime value) {
        return value != null ? Date.from(value.toInstant()) : null;
    }
}
