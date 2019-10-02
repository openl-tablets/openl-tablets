package org.openl.rules.ruleservice.storelogdata.elasticsearch.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.openl.rules.ruleservice.storelogdata.elasticsearch.IndexBuilder;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = { ElementType.METHOD, ElementType.TYPE })
public @interface StoreLogDataToElasticsearch {
    Class<? extends IndexBuilder>[] value() default DEFAULT.class;

    public static interface DEFAULT extends IndexBuilder {
    }
}
