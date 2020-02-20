package org.openl.rules.ruleservice.simple;

import java.lang.reflect.Method;

import org.openl.runtime.ASMProxyHandler;

public class RulesFrontendProxyMethodHandler implements ASMProxyHandler {

    private RulesFrontend rulesFrontend;
    private String serviceName;

    public RulesFrontendProxyMethodHandler(String serviceName, RulesFrontend rulesFrontend) {
        this.rulesFrontend = rulesFrontend;
        this.serviceName = serviceName;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        try {
            return rulesFrontend.execute(serviceName, method.getName(), method.getParameterTypes(), args);
        } catch (MethodInvocationException e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            } else {
                throw new MethodInvocationRuntimeException(e);
            }
        }
    }
}
