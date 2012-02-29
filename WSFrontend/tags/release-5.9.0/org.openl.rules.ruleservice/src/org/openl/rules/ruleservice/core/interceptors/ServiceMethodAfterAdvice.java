package org.openl.rules.ruleservice.core.interceptors;

import java.lang.reflect.Method;

public interface ServiceMethodAfterAdvice<T> {

    T afterReturning(Method method, Object result, Object... args) throws Throwable;

    T afterThrowing(Method method, Throwable t, Object... args) throws Throwable;

}
