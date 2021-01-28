package org.openl.ruleservice.dynamicinterface.test;

import java.lang.reflect.Method;

import org.openl.rules.ruleservice.core.interceptors.ServiceMethodAfterAdvice;

public class MyAfterAdvice implements ServiceMethodAfterAdvice<MyClass> {

    public MyClass afterReturning(Method method, Object result, Object... args) throws Exception {
        return null;
    }

    public MyClass afterThrowing(Method method, Exception t, Object... args) throws Exception {
        return null;
    }
}
