package org.openl.itest.serviceclass;

import java.lang.reflect.Method;

import org.openl.rules.ruleservice.core.interceptors.AbstractServiceMethodAfterReturningAdvice;

public class Simple4ServiceMethodAfterAdvice extends AbstractServiceMethodAfterReturningAdvice<String> {
    @Override
    public String afterReturning(Method interfaceMethod, Object result, Object... args) throws Exception {
        return "I don't know";
    }
}
