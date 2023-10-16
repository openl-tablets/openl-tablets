package org.openl.itest.service.internal;

import java.lang.reflect.Method;

import org.openl.rules.ruleservice.core.interceptors.ServiceMethodBeforeAdvice;

class PrepareInterceptor implements ServiceMethodBeforeAdvice {

    void before(Method interfaceMethod, Object proxy, Object[] args) throws Throwable {
        def type = args[0]
        type.name = type.extra
    }

}
