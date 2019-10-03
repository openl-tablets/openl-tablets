package org.openl.itest.cassandra;

import org.openl.rules.ruleservice.storelogdata.Converter;

public class IntToStringConvertor implements Converter<Integer, String> {
    @Override
    public String apply(Integer value) {
        return value != null ? String.valueOf(value) : null;
    }
}
