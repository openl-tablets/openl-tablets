package org.openl.rules.ruleservice.core.interceptors;

import java.lang.reflect.Method;

public interface ServiceInvocationAdviceListener {

    default void beforeMethodInvocation(Method interfaceMethod,
            Object[] args,
            Object result,
            Exception lastOccuredException) {
    }

    default void afterMethodInvocation(Method interfaceMethod,
            Object[] args,
            Object result,
            Exception lastOccuredException) {
    }

    default void beforeServiceMethodAdvice(ServiceMethodAdvice serviceMethodAdvice,
            Method interfaceMethod,
            Object[] args,
            Object result,
            Exception lastOccuredException) {
    }

    default void afterServiceMethodAdvice(ServiceMethodAdvice serviceMethodAdvice,
            Method interfaceMethod,
            Object[] args,
            Object result,
            Exception lastOccuredException) {
    }
}
