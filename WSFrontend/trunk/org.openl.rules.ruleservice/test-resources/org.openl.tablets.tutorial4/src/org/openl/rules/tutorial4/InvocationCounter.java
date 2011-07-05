package org.openl.rules.tutorial4;

import java.lang.reflect.Method;

import org.openl.rules.ruleservice.core.interceptors.ServiceMethodBeforeInterceptor;

public class InvocationCounter extends ServiceMethodBeforeInterceptor {
    
    private static int count = 0;

    public InvocationCounter(Method method) {
        super(method);
    }

    public void invoke(Object... args) {
        count++;
    }
    
    public static int getCount(){
        return count;
    }
}
