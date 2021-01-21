package org.openl.itest.service;

import java.lang.reflect.Method;

import org.openl.rules.ruleservice.core.interceptors.AbstractServiceMethodAfterReturningAdvice;
import org.openl.rules.ruleservice.core.interceptors.annotations.UseOpenMethodReturnType;

@UseOpenMethodReturnType
class OpenLTypeServiceMethodAfterAdvice extends AbstractServiceMethodAfterReturningAdvice<Object> {
    @Override
    Object afterReturning(Method interfaceMethod, Object result, Object... args) throws Exception {
        return result;
    }
}
