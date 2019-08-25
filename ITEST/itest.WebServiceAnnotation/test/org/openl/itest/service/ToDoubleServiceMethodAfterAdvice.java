package org.openl.itest.service;

import java.lang.reflect.Method;

import org.openl.rules.ruleservice.core.interceptors.AbstractServiceMethodAfterReturningAdvice;

public class ToDoubleServiceMethodAfterAdvice extends AbstractServiceMethodAfterReturningAdvice<Double> {
    @Override
    public Double afterReturning(Method interfaceMethod, Object result, Object... args) throws Exception {
        return ((Number) result).doubleValue();
    }
}
