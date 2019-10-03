package org.openl.rules.ruleservice.storelogdata.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Date;

import org.openl.rules.ruleservice.storelogdata.Converter;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = { ElementType.FIELD, ElementType.METHOD })
public @interface IncomingTime {
    Class<? extends Converter<?, Date>> converter() default DefaultDateConverter.class;
}
