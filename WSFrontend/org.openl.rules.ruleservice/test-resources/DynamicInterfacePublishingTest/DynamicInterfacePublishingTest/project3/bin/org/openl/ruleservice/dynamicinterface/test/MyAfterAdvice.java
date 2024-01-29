package org.openl.ruleservice.dynamicinterface.test;

import java.lang.reflect.Method;

public class MyAfterAdvice implements ServiceMethodAfterAdvice<MyClass> {

    public MyClass afterReturning(Method method, Object result, Object... args) throws Exception {
        return null;
    }

    public MyClass afterThrowing(Method method, Exception t, Object... args) throws Exception {
        return null;
    }
}
