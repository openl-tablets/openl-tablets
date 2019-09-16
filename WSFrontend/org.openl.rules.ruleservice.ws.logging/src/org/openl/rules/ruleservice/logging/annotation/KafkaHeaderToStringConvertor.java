package org.openl.rules.ruleservice.logging.annotation;

import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.StringUtils;
import org.openl.rules.ruleservice.logging.Convertor;

public final class KafkaHeaderToStringConvertor implements Convertor<byte[], String> {

    @Override
    public String convert(byte[] value) {
        return StringUtils.toEncodedString(value, StandardCharsets.UTF_8);
    }
}
