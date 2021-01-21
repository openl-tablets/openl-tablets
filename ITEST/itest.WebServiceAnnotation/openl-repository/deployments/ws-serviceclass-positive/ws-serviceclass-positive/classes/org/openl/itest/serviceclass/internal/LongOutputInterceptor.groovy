package org.openl.itest.serviceclass.internal

import org.openl.rules.ruleservice.core.interceptors.ServiceMethodAfterAdvice

import java.lang.reflect.Method

class LongOutputInterceptor implements ServiceMethodAfterAdvice<Response> {

    @Override
    Response afterReturning(Method interfaceMethod, Object result, Object... args) throws Exception {
        return new Response("SUCCESS", ((Long) result).intValue());
    }

    @Override
    Response afterThrowing(Method interfaceMethod, Exception t, Object... args) throws Exception {
        return new Response("ERROR", -1);
    }
}
