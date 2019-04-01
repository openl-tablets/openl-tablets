package org.openl.rules.ruleservice.test;

import java.util.Collection;

import org.junit.Before;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleServiceInstantiationException;
import org.openl.rules.ruleservice.management.ServiceManagerImpl;
import org.openl.rules.ruleservice.simple.JavaClassRuleServicePublisher;
import org.openl.rules.ruleservice.simple.MethodInvocationException;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Class designed for testing rules. This test requires JavaClassRuleServicePublisher publisher in spring context.
 *
 * @author Marat Kamalov
 *
 */
// @RunWith(SpringJUnit4ClassRunner.class)
// @ContextConfiguration
public abstract class AbstractJavaClassRuleServiceTest implements ApplicationContextAware {
    private ApplicationContext applicationContext;

    private static volatile boolean initialized = false;

    @Before
    public void before() {
        if (!initialized) {
            applicationContext.getBean("serviceManager", ServiceManagerImpl.class);
            initialized = true;
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * Returns JavaClassRuleServicePublisher from context.
     *
     * @return java class rule service publisher
     */
    protected JavaClassRuleServicePublisher getJavaClassRuleServicePublisher() {
        return applicationContext.getBean(JavaClassRuleServicePublisher.class);
    }

    /**
     * Returns all deployed services
     *
     * @return all deployed services
     */
    protected Collection<OpenLService> getServices() {
        return getJavaClassRuleServicePublisher().getServices();
    }

    /**
     * Executes rule from published service.
     *
     * @param serviceName service name
     * @param ruleName rule name
     * @param params patameters
     * @return result of invocation
     * @throws MethodInvocationException exception on rule execution fail.
     */
    protected Object execute(String serviceName, String ruleName, Object... params) throws MethodInvocationException {
        if (serviceName == null) {
            throw new IllegalArgumentException("serviceName argument can't be null");
        }

        if (ruleName == null) {
            throw new IllegalArgumentException("ruleName argument can't be null");
        }
        return getJavaClassRuleServicePublisher().getFrontend().execute(serviceName, ruleName, params);
    }

    /**
     * Executes rule from published service.
     *
     * @param serviceName service name
     * @param ruleName rule name
     * @param inputParamsTypes the type of parameters
     * @param params parameters
     * @return result of invocation
     * @throws MethodInvocationException exception on rule execution fail.
     */
    protected Object execute(String serviceName,
            String ruleName,
            Class<?>[] inputParamsTypes,
            Object[] params) throws MethodInvocationException {
        if (serviceName == null) {
            throw new IllegalArgumentException("serviceName argument can't be null");
        }
        if (ruleName == null) {
            throw new IllegalArgumentException("ruleName argument can't be null");
        }
        return getJavaClassRuleServicePublisher().getFrontend()
            .execute(serviceName, ruleName, inputParamsTypes, params);
    }

    /**
     * Returns field value from published service.
     *
     * @param serviceName service name
     * @param fieldName field name
     * @return field value
     * @throws MethodInvocationException exception on rule execution fail.
     */
    protected Object getValue(String serviceName, String fieldName) throws MethodInvocationException {
        if (serviceName == null) {
            throw new IllegalArgumentException("serviceName argument can't be null");
        }
        if (fieldName == null) {
            throw new IllegalArgumentException("fieldName argument can't be null");
        }

        return getJavaClassRuleServicePublisher().getFrontend().getValue(serviceName, fieldName);
    }

    /**
     * Returns published service by service name.
     *
     * @param serviceName service name
     * @return service
     */
    protected Object getService(String serviceName) throws RuleServiceInstantiationException {
        OpenLService service = getJavaClassRuleServicePublisher().getServiceByName(serviceName);
        if (service == null) {
            return null;
        }
        return service.getServiceBean();
    }

    /**
     * Returns published service by service name.
     *
     * @param serviceName service name
     * @param serviceClass service type
     * @return service
     */
    @SuppressWarnings("unchecked")
    protected <T> T getService(String serviceName, Class<T> serviceClass) throws RuleServiceInstantiationException {
        OpenLService service = getJavaClassRuleServicePublisher().getServiceByName(serviceName);
        if (service == null) {
            return null;
        }
        Object result = service.getServiceBean();
        return (T) result;
    }

    /**
     * Returns published service type by service name.
     *
     * @param serviceName service namee
     * @return service type
     */
    protected Class<?> getServiceClassByServiceName(String serviceName) throws RuleServiceInstantiationException {
        OpenLService service = getJavaClassRuleServicePublisher().getServiceByName(serviceName);
        if (service == null) {
            return null;
        }
        return service.getServiceClass();
    }
}
