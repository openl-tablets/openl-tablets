package org.openl.rules.tutorial4;

import java.lang.reflect.Method;

import org.openl.rules.ruleservice.core.interceptors.ServiceMethodBeforeAdvice;

public class InvocationCounter implements ServiceMethodBeforeAdvice {

    private static int count = 0;

    @Override
    public void before(Method method, Object proxy, Object... args) throws Throwable {
        count++;
    }
    
    public static int getCount() {
        return count;
    }
}
