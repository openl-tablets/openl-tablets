package org.openl.rules.ruleservice.core.interceptors.converters;

import java.lang.reflect.Method;

import org.openl.rules.ruleservice.core.interceptors.AbstractServiceMethodAfterReturningAdvice;

public class StringArrayConverterAdvice extends AbstractServiceMethodAfterReturningAdvice<StringArray> {
    @Override
    public StringArray afterReturning(Method method, Object result, Object... args) throws Exception {
        return new StringArray((String[]) result);
    }
}
