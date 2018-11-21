package org.openl.itest.service.internal;

import java.lang.reflect.Method;

import org.openl.rules.ruleservice.core.interceptors.ServiceMethodAfterAdvice;

public class OutputInterceptor implements ServiceMethodAfterAdvice<MyType> {

    public MyType afterReturning(Method interfaceMethod, Object result, Object... args) throws Exception {
        return new MyType("PARSED", (Integer) result);
    }

    public MyType afterThrowing(Method interfaceMethod, Exception t, Object... args) throws Exception {
        return new MyType("ERROR", -1);
    }

}
