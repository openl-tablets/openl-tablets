package org.openl.rules.ruleservice.storelogdata.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.openl.rules.ruleservice.storelogdata.StoreLogDataConverter;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = { ElementType.FIELD, ElementType.METHOD })
public @interface WithStoreLogDataConverter {
    Class<? extends StoreLogDataConverter<?>> converter();
}
