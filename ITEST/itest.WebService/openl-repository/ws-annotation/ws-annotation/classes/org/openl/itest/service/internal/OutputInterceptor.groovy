package org.openl.itest.service.internal

import java.lang.reflect.Method

import org.openl.rules.ruleservice.core.interceptors.ServiceMethodAfterAdvice

class OutputInterceptor implements ServiceMethodAfterAdvice<MyType> {

    MyType afterReturning(Method interfaceMethod, Object result, Object... args) throws Exception {
        return new MyType("PARSED", (Integer) result);
    }

    MyType afterThrowing(Method interfaceMethod, Exception t, Object... args) throws Exception {
        return new MyType("ERROR", -1);
    }

}
