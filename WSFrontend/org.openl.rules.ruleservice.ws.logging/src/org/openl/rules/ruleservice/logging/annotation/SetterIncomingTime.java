package org.openl.rules.ruleservice.logging.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Date;

import org.openl.rules.project.model.RulesDeploy.PublisherType;
import org.openl.rules.ruleservice.logging.TypeConvertor;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SetterIncomingTime {
    Class<? extends TypeConvertor<Date, ?>> convertor() default DefaultDateConvertor.class;

    PublisherType[] publisherTypes() default { PublisherType.WEBSERVICE, PublisherType.RESTFUL };
}
