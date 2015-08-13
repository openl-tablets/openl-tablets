package org.openl.rules.ruleservice.publish.rmi;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;

class StaticRmiInvocationHandler implements InvocationHandler {

    private Object target;
    private Map<Method, Method> methodMap;

    public StaticRmiInvocationHandler(Object target, Map<Method, Method> methodMap) {
        if (target == null) {
            throw new IllegalArgumentException("target argument can't be null!");
        }
        if (methodMap == null) {
            throw new IllegalArgumentException("methodMap argument can't be null!");
        }
        this.target = target;
        this.methodMap = methodMap;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Method m = methodMap.get(method);
        if (m == null) {
            throw new IllegalStateException("Method not found in methods map!");
        }
        return m.invoke(target, args);
    }
}