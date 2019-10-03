package org.openl.rules.ruleservice.storelogdata.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.openl.rules.ruleservice.storelogdata.Converter;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = { ElementType.FIELD, ElementType.METHOD })
public @interface KafkaMessageHeader {
    String value();

    Type type() default Type.CONSUMER_RECORD;

    Class<? extends Converter<byte[], ?>> converter() default ByteArrayToStringConverter.class;

    public enum Type {
        PRODUCER_RECORD,
        CONSUMER_RECORD
    }
}
