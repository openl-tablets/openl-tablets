package org.openl.rules.ruleservice.storelogdata.annotation;

import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.StringUtils;
import org.openl.rules.ruleservice.storelogdata.Converter;

public final class ByteArrayToStringConverter implements Converter<byte[], String> {

    @Override
    public String apply(byte[] value) {
        return StringUtils.toEncodedString(value, StandardCharsets.UTF_8);
    }
}
