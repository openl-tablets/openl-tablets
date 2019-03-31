package org.openl.rules.ruleservice.conf;

import java.util.Collection;

import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class BeanCollectionFactoryBean<T> extends AbstractFactoryBean<Collection<T>> implements ApplicationContextAware{
    private Class<T> beanType;
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
    
    public void setBeanType(Class<T> beanType) {
        this.beanType = beanType;
    }

    @Override
    protected Collection<T> createInstance() throws Exception {
        return applicationContext.getBeansOfType(beanType).values();
    }

    @Override
    public Class<?> getObjectType() {
        return Collection.class;
    }    
}