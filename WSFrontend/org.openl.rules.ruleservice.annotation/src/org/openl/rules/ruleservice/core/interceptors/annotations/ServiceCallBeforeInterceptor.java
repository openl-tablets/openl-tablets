package org.openl.rules.ruleservice.core.interceptors.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.openl.rules.ruleservice.core.interceptors.ServiceMethodBeforeAdvice;

/**
 * Annotation for registering before method intercepters.
 *
 * @author Marat Kamalov
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ServiceCallBeforeInterceptor {
    Class<? extends ServiceMethodBeforeAdvice>[] value();

    ServiceCallInterceptorGroup group() default ServiceCallInterceptorGroup.ALL;
}
