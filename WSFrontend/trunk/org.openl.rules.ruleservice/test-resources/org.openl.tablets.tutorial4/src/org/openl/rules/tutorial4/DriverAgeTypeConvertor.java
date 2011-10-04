package org.openl.rules.tutorial4;

import java.lang.reflect.Method;

import org.openl.rules.ruleservice.core.interceptors.ServiceMethodAfterReturningAdvice;

public class DriverAgeTypeConvertor implements ServiceMethodAfterReturningAdvice<DriverAgeType> {

    @Override
    public DriverAgeType afterReturning(Method method, Object result, Object... args) throws Throwable {
        return DriverAgeType.parse((String) result);
    }
}
