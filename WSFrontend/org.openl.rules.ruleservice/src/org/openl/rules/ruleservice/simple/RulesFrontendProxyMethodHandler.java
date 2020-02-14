package org.openl.rules.ruleservice.simple;

import java.lang.reflect.Method;

import javassist.util.proxy.MethodHandler;

public class RulesFrontendProxyMethodHandler implements MethodHandler {

    private RulesFrontend rulesFrontend;
    private String serviceName;

    public RulesFrontendProxyMethodHandler(String serviceName, RulesFrontend rulesFrontend) {
        this.rulesFrontend = rulesFrontend;
        this.serviceName = serviceName;
    }

    @Override
    public Object invoke(Object proxy, Method method, Method proceed, Object[] args) throws Throwable {
        try {
            return rulesFrontend.execute(serviceName, method.getName(), method.getParameterTypes(), args);
        } catch (MethodInvocationException e) {
            if (e.getCause() instanceof RuntimeException) {
                throw e.getCause();
            } else {
                throw new MethodInvocationRuntimeException(e);
            }
        }
    }
}
