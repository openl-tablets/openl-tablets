package org.openl.rules.ruleservice.core.interceptors;

import java.lang.reflect.Method;
import java.util.function.Consumer;

public interface ServiceInvocationAdviceListener {

    default void beforeMethodInvocation(Method interfaceMethod,
            Object[] args,
            Object result,
            Exception ex,
            Consumer<Object> postProcessAdvice) {
    }

    default void afterMethodInvocation(Method interfaceMethod,
            Object[] args,
            Object result,
            Exception ex,
            Consumer<Object> postProcessAdvice) {
    }

    default void beforeServiceMethodAdvice(ServiceMethodAdvice serviceMethodAdvice,
            Method interfaceMethod,
            Object[] args,
            Object result,
            Exception ex,
            Consumer<Object> postProcessAdvice) {
    }

    default void afterServiceMethodAdvice(ServiceMethodAdvice serviceMethodAdvice,
            Method interfaceMethod,
            Object[] args,
            Object result,
            Exception ex,
            Consumer<Object> postProcessAdvice) {
    }
}
