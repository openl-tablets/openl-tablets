package org.openl.rules.ruleservice.core.interceptors.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.openl.rules.ruleservice.core.interceptors.ServiceMethodAroundAdvice;

/**
 * Annotation for registering after method interceptors.
 *
 * @author Marat Kamalov
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ServiceCallAroundInterceptor {
    Class<? extends ServiceMethodAroundAdvice<?>> value();
}
