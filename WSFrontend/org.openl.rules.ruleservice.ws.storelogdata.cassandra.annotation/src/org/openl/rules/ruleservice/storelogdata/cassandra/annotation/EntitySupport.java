package org.openl.rules.ruleservice.storelogdata.cassandra.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = { ElementType.TYPE })
public @interface EntitySupport {
    Class<? extends EntityOperations<?, ?>> value();
}
