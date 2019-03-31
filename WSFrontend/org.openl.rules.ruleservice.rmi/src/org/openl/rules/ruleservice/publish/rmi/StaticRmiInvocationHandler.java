package org.openl.rules.ruleservice.publish.rmi;

import java.lang.reflect.Method;
import java.util.Map;

import org.openl.runtime.IOpenLInvocationHandler;

class StaticRmiInvocationHandler implements IOpenLInvocationHandler {

    private Object target;
    private Map<Method, Method> methodMap;

    @Override
    public Object getTarget() {
        return target;
    }

    public StaticRmiInvocationHandler(Object target, Map<Method, Method> methodMap) {
        if (target == null) {
            throw new IllegalArgumentException("target argument must not be null!");
        }
        if (methodMap == null) {
            throw new IllegalArgumentException("methodMap argument must not be null!");
        }
        this.target = target;
        this.methodMap = methodMap;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Method m = methodMap.get(method);
        if (m == null) {
            throw new IllegalStateException("Method hasn't been found in methods map!");
        }
        return m.invoke(target, args);
    }
}