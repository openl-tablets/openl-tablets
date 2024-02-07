package org.openl.rules.ruleservice.storelogdata.annotation;

import java.nio.charset.StandardCharsets;

import org.openl.rules.ruleservice.storelogdata.Converter;

public final class ByteArrayToStringConverter implements Converter<byte[], String> {

    @Override
    public String apply(byte[] value) {
        return new String(value, StandardCharsets.UTF_8);
    }
}
