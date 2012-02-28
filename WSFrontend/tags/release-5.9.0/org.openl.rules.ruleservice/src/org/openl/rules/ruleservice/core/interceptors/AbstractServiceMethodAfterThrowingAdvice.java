package org.openl.rules.ruleservice.core.interceptors;

import java.lang.reflect.Method;

public abstract class AbstractServiceMethodAfterThrowingAdvice<T> implements ServiceMethodAfterAdvice<T> {
    @Override
    @SuppressWarnings("unchecked")
    public T afterReturning(Method method, Object result, Object... args) throws Throwable {
        return (T) result;
    }
}
