package org.openl.rules.ruleservice.core.interceptors;

import java.lang.reflect.Method;

public interface ServiceInvocationAdviceListener {

    default void beforeMethodInvocation(Method interfaceMethod,
                                        Object[] args,
                                        Object result,
                                        Exception ex,
                                        Instantiator postProcessAdvice) {
    }

    default void afterMethodInvocation(Method interfaceMethod,
                                       Object[] args,
                                       Object result,
                                       Exception ex,
                                       Instantiator postProcessAdvice) {
    }

    default void beforeServiceMethodAdvice(ServiceMethodAdvice serviceMethodAdvice,
                                           Method interfaceMethod,
                                           Object[] args,
                                           Object result,
                                           Exception ex,
                                           Instantiator postProcessAdvice) {
    }

    default void afterServiceMethodAdvice(ServiceMethodAdvice serviceMethodAdvice,
                                          Method interfaceMethod,
                                          Object[] args,
                                          Object result,
                                          Exception ex,
                                          Instantiator postProcessAdvice) {
    }

    @FunctionalInterface
    static interface Instantiator {
        <T> T instantiate(Class<T> clazz);

    }
}
