package org.openl.rules.ruleservice.storelogdata.cassandra.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = {ElementType.METHOD, ElementType.TYPE})
public @interface StoreLogDataToCassandra {
    Class<?>[] value() default DEFAULT.class;

    boolean sync() default false;

    interface DEFAULT {
    }
}