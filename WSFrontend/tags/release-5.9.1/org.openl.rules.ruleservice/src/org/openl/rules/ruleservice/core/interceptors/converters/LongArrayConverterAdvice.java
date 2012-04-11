package org.openl.rules.ruleservice.core.interceptors.converters;

import java.lang.reflect.Method;

import org.openl.rules.ruleservice.core.interceptors.AbstractServiceMethodAfterReturningAdvice;

public class LongArrayConverterAdvice extends AbstractServiceMethodAfterReturningAdvice<LongArray> {
    @Override
    public LongArray afterReturning(Method method, Object result, Object... args) throws Exception {
        return new LongArray((Long[]) result);
    }
}
