package org.openl.itest.serviceclass;

import java.lang.reflect.Method;

import org.openl.rules.ruleservice.core.interceptors.ServiceMethodBeforeAdvice;

public class Simple4ServiceMethodBeforeAdvice implements ServiceMethodBeforeAdvice {
    @Override
    public void before(Method interfaceMethod, Object serviceTarget, Object... args) throws Throwable {
        args[1] = 22;
    }
}
