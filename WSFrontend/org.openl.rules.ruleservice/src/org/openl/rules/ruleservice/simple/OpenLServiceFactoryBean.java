package org.openl.rules.ruleservice.simple;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Required;

public class OpenLServiceFactoryBean implements FactoryBean<Object> {
    private Class<?> proxyInterface;
    private String serviceName;
    private RulesFrontend rulesFrontend;

    @Override
    public Object getObject() throws Exception {
        return rulesFrontend.buildServiceProxy(serviceName, proxyInterface);
    }

    @Override
    public Class<?> getObjectType() {
        return proxyInterface;
    }

    @Override
    public boolean isSingleton() {
        return Boolean.TRUE;
    }

    @Required
    public void setRulesFrontend(RulesFrontend rulesFrontend) {
        this.rulesFrontend = rulesFrontend;
    }

    @Required
    public void setProxyInterface(Class<?> proxyInterface) {
        this.proxyInterface = proxyInterface;
    }

    @Required
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
}
