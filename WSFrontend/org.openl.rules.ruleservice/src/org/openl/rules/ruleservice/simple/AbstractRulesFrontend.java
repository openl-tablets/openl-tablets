package org.openl.rules.ruleservice.simple;

@Deprecated
public abstract class AbstractRulesFrontend implements RulesFrontend {

    @Deprecated
    @Override
    public <T> T buildServiceProxy(String serviceName, Class<T> proxyInterface) {
        OpenLServiceFactoryBean<T> factory = new OpenLServiceFactoryBean<>(proxyInterface, serviceName);
        factory.setRulesFrontend(this);
        return factory.getObject();
    }

    @Deprecated
    @Override
    public <T> T buildServiceProxy(String serviceName, Class<T> proxyInterface, ClassLoader classLoader) {
        OpenLServiceFactoryBean<T> factory = new OpenLServiceFactoryBean<>(proxyInterface, serviceName);
        factory.setRulesFrontend(this);
        factory.setClassLoader(classLoader);
        return factory.getObject();
    }
}
