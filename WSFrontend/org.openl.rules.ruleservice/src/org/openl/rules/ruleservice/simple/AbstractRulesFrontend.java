package org.openl.rules.ruleservice.simple;

import java.lang.reflect.Proxy;

public abstract class AbstractRulesFrontend implements RulesFrontend{
    @Override
    public <T> T buildServiceProxy(String serviceName, Class<T> proxyInterface) {
        return buildServiceProxy(serviceName, proxyInterface, Thread.currentThread().getContextClassLoader());
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public <T> T buildServiceProxy(String serviceName, Class<T> proxyInterface, ClassLoader classLoader) {
        if (serviceName == null) {
            throw new IllegalArgumentException("serviceName argument can't be null");
        }
        if (proxyInterface == null){
            throw new IllegalArgumentException("proxyInterface argument can't be null");
        }
        
        return (T) Proxy.newProxyInstance(classLoader, new Class[]{proxyInterface}, new RulesFrontendProxyInvocationHandler(serviceName, this));
    }
}
