package org.openl.rules.tutorial4;

import java.lang.reflect.Method;

import org.openl.rules.ruleservice.core.interceptors.ServiceMethodAfterInterceptor;

public class DriverAgeTypeConvertor extends ServiceMethodAfterInterceptor<DriverAgeType>{

    public DriverAgeTypeConvertor(Method method) {
        super(method);
    }

    @Override
    public DriverAgeType invoke(Object result, Object... args) {
        return DriverAgeType.parse((String)result);
    }

}
