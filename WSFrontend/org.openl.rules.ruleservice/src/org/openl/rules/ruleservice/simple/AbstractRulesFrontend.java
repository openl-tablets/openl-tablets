package org.openl.rules.ruleservice.simple;

import java.lang.reflect.Proxy;
import java.util.Objects;

public abstract class AbstractRulesFrontend implements RulesFrontend {
    @Override
    public <T> T buildServiceProxy(String serviceName, Class<T> proxyInterface) {
        return buildServiceProxy(serviceName, proxyInterface, Thread.currentThread().getContextClassLoader());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T buildServiceProxy(String serviceName, Class<T> proxyInterface, ClassLoader classLoader) {
        Objects.requireNonNull(serviceName, "serviceName cannot be null");
        Objects.requireNonNull(proxyInterface, "proxyInterface cannot be null");
        return (T) Proxy.newProxyInstance(classLoader,
            new Class[] { proxyInterface },
            new RulesFrontendProxyInvocationHandler(serviceName, this));
    }
}
