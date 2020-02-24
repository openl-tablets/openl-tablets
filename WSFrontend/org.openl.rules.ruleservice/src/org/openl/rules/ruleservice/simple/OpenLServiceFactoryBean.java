package org.openl.rules.ruleservice.simple;

import org.openl.runtime.ASMProxyFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Creates a proxy object for defined service.
 * 
 * @param <T> The facade interface type
 */
public class OpenLServiceFactoryBean<T> implements FactoryBean<T> {
    private Class<T> proxyInterface;
    private String serviceName;
    private ClassLoader classLoader;
    private RulesFrontend rulesFrontend;

    /**
     * @param proxyInterface a facade interface for work with OpenL rules.
     * @param serviceName a name of OpenL rules saved in Frontend.
     */
    public OpenLServiceFactoryBean(Class<T> proxyInterface, String serviceName) {
        this.proxyInterface = proxyInterface;
        this.serviceName = serviceName;
    }

    @Override
    public T getObject() {
        ClassLoader cl = classLoader != null ? classLoader : Thread.currentThread().getContextClassLoader();
        return ASMProxyFactory.newProxyInstance(cl, (proxy, method, args) -> {
            try {
                return rulesFrontend.execute(serviceName, method.getName(), method.getParameterTypes(), args);
            } catch (MethodInvocationException e) {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                } else {
                    throw new MethodInvocationRuntimeException(e);
                }
            }
        }, proxyInterface);

    }

    @Override
    public Class<?> getObjectType() {
        return proxyInterface;
    }

    @Override
    public boolean isSingleton() {
        return Boolean.TRUE;
    }

    @Autowired
    @Qualifier("frontend")
    public void setRulesFrontend(RulesFrontend rulesFrontend) {
        this.rulesFrontend = rulesFrontend;
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Kept for backward compatibility.
     */
    @Deprecated
    public OpenLServiceFactoryBean() {
    }

    /**
     * @deprecated Use constructor-arg instead
     */
    @Deprecated
    public void setProxyInterface(Class<T> proxyInterface) {
        this.proxyInterface = proxyInterface;
    }

    /**
     * @deprecated Use constructor-arg instead
     */
    @Deprecated
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

}
