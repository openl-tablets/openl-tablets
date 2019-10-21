package org.openl.rules.ruleservice.publish.rmi;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;

import org.openl.runtime.IOpenLInvocationHandler;

class StaticRmiInvocationHandler implements IOpenLInvocationHandler<Method, Method> {

    private Object target;
    private Map<Method, Method> methodMap;

    @Override
    public Object getTarget() {
        return target;
    }

    @Override
    public Method getTargetMember(Method key) {
        return methodMap.get(key);
    }

    public StaticRmiInvocationHandler(Object target, Map<Method, Method> methodMap) {
        this.target = Objects.requireNonNull(target, "target cannot be null");
        this.methodMap = Objects.requireNonNull(methodMap, "methodMap cannot be null");
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Method m = methodMap.get(method);
        if (m == null) {
            throw new IllegalStateException("Method has not been found in methods map!");
        }
        return m.invoke(target, args);
    }
}