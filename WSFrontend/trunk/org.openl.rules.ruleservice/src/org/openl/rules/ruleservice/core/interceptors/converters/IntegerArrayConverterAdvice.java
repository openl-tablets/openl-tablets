package org.openl.rules.ruleservice.core.interceptors.converters;

import java.lang.reflect.Method;

import org.openl.rules.ruleservice.core.interceptors.AbstractServiceMethodAfterReturningAdvice;

public class IntegerArrayConverterAdvice extends AbstractServiceMethodAfterReturningAdvice<IntegerArray> {
    @Override
    public IntegerArray afterReturning(Method method, Object result, Object... args) throws Exception {
        return new IntegerArray((Integer[]) result);
    }
}
