package org.openl.rules.ruleservice.simple;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class RulesFrontendProxyInvocationHandler implements InvocationHandler {

    private RulesFrontend rulesFrontend;
    private String serviceName;

    public RulesFrontendProxyInvocationHandler(String serviceName, RulesFrontend rulesFrontend) {
        this.rulesFrontend = rulesFrontend;
        this.serviceName = serviceName;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            return rulesFrontend.execute(serviceName, method.getName(), args);
        } catch (MethodInvocationException e) {
            if (e.getCause() instanceof RuntimeException) {
                throw e.getCause();
            } else {
                throw new MethodInvocationRuntimeException(e);
            }
        }
    }
}
