package org.openl.rules.ruleservice.logging.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.openl.rules.project.model.RulesDeploy.PublisherType;
import org.openl.rules.ruleservice.logging.Convertor;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = { ElementType.FIELD, ElementType.METHOD })
public @interface Url {
    Class<? extends Convertor<?, String>> convertor() default DefaultStringConvertor.class;

    PublisherType[] publisherTypes() default { PublisherType.WEBSERVICE,
            PublisherType.RESTFUL,
            PublisherType.KAFKA,
            PublisherType.RMI };
}
