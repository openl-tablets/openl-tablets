package org.openl.rules.ruleservice.core.interceptors;

import java.lang.reflect.Method;

public abstract class AbstractServiceMethodAfterReturningAdvice<T> implements ServiceMethodAfterAdvice<T> {

    @Override
    public T afterThrowing(Method method, Throwable t, Object args) throws Throwable {
        throw t;
    }
}
