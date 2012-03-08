package org.openl.rules.ruleservice.test;

import org.openl.rules.ruleservice.simple.JavaClassRuleServicePublisher;
import org.openl.rules.ruleservice.simple.MethodInvocationException;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Class designed for testing rules.
 * 
 * @author Marat Kamalov
 * 
 */
//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration
public abstract class AbstractJavaClassRuleServiceTest implements ApplicationContextAware {
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    protected abstract JavaClassRuleServicePublisher getJavaClassRuleServicePublisher();

    protected Object execute(String serviceName, String ruleName, Object... params) throws MethodInvocationException {
        if (serviceName == null) {
            throw new IllegalArgumentException("serviceName argument can't be null");
        }

        if (ruleName == null) {
            throw new IllegalArgumentException("ruleName argument can't be null");
        }
        return getJavaClassRuleServicePublisher().getFrontend().execute(serviceName, ruleName, params);
    }

    protected Object execute(String serviceName, String ruleName, Class<?>[] inputParamsTypes, Object[] params)
            throws MethodInvocationException {
        if (serviceName == null) {
            throw new IllegalArgumentException("serviceName argument can't be null");
        }
        if (ruleName == null) {
            throw new IllegalArgumentException("ruleName argument can't be null");
        }
        return getJavaClassRuleServicePublisher().getFrontend()
                .execute(serviceName, ruleName, inputParamsTypes, params);
    }

    protected Object getValue(String serviceName, String fieldName) throws MethodInvocationException {
        if (serviceName == null) {
            throw new IllegalArgumentException("serviceName argument can't be null");
        }
        if (fieldName == null) {
            throw new IllegalArgumentException("fieldName argument can't be null");
        }

        return getJavaClassRuleServicePublisher().getFrontend().getValue(serviceName, fieldName);
    }

    @SuppressWarnings("unchecked")
    protected <T> T getService(String serviceName, Class<T> serviceClass) {
        Object result = getJavaClassRuleServicePublisher().getFrontend().findServiceByName(serviceName).getServiceBean();
        return (T) result;
    }
}
