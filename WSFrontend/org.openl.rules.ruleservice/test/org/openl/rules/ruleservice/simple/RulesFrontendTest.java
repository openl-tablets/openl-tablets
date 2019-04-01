package org.openl.rules.ruleservice.simple;

import static org.junit.Assert.*;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleServiceDeployException;
import org.openl.rules.ruleservice.core.RuleServiceUndeployException;
import org.openl.rules.ruleservice.management.ServiceManager;
import org.openl.rules.ruleservice.publish.RuleServicePublisher;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(properties = { "ruleservice.datasource.dir=test-resources/RulesFrontendTest",
        "ruleservice.datasource.deploy.clean.datasource=false",
        "ruleservice.isProvideRuntimeContext=false" })
@ContextConfiguration({ "classpath:openl-ruleservice-beans.xml" })
public class RulesFrontendTest implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext arg0) throws BeansException {
        this.applicationContext = arg0;
    }

    @Test
    public void testGetServices() {
        assertNotNull(applicationContext);
        ServiceManager serviceManager = applicationContext.getBean("serviceManager", ServiceManager.class);
        assertNotNull(serviceManager);
        RulesFrontend frontend = applicationContext.getBean("frontend", RulesFrontend.class);
        assertNotNull(frontend);
        Collection<String> services = frontend.getServiceNames();
        assertNotNull(services);
        assertEquals(2, services.size());
    }

    @Test
    public void testProxyServices() {
        assertNotNull(applicationContext);
        ServiceManager serviceManager = applicationContext.getBean("serviceManager", ServiceManager.class);
        assertNotNull(serviceManager);
        RulesFrontend frontend = applicationContext.getBean("frontend", RulesFrontend.class);
        assertNotNull(frontend);
        Collection<String> services = frontend.getServiceNames();
        assertNotNull(services);
        assertEquals(2, services.size());
        ProxyInterface proxy = frontend.buildServiceProxy("RulesFrontendTest_multimodule", ProxyInterface.class);
        assertEquals("World, Good Morning!", proxy.worldHello(10));

    }

    @Test(expected = org.openl.rules.ruleservice.simple.MethodInvocationRuntimeException.class)
    public void testProxyServicesNotExistedMethod() {
        assertNotNull(applicationContext);
        ServiceManager serviceManager = applicationContext.getBean("serviceManager", ServiceManager.class);
        assertNotNull(serviceManager);
        RulesFrontend frontend = applicationContext.getBean("frontend", RulesFrontend.class);
        assertNotNull(frontend);
        Collection<String> services = frontend.getServiceNames();
        assertNotNull(services);
        assertEquals(2, services.size());
        ProxyInterface proxy = frontend.buildServiceProxy("RulesFrontendTest_multimodule", ProxyInterface.class);
        proxy.notExustedMethod(10);
    }

    @Test(expected = org.openl.rules.ruleservice.simple.MethodInvocationRuntimeException.class)
    public void testProxyServicesNotExistedService() throws RuleServiceUndeployException, RuleServiceDeployException {
        assertNotNull(applicationContext);
        ServiceManager serviceManager = applicationContext.getBean("serviceManager", ServiceManager.class);
        assertNotNull(serviceManager);
        RuleServicePublisher ruleServicePublisher = applicationContext.getBean("ruleServicePublisher",
            RuleServicePublisher.class);
        assertNotNull(serviceManager);
        RulesFrontend frontend = applicationContext.getBean("frontend", RulesFrontend.class);
        assertNotNull(frontend);
        Collection<String> services = frontend.getServiceNames();
        assertNotNull(services);
        assertEquals(2, services.size());
        ProxyInterface proxy = frontend.buildServiceProxy("RulesFrontendTest_multimodule", ProxyInterface.class);
        assertEquals("World, Good Morning!", proxy.worldHello(10));
        OpenLService openLService = ruleServicePublisher.getServiceByName("RulesFrontendTest_multimodule");
        try {
            ruleServicePublisher.undeploy("RulesFrontendTest_multimodule");
            try {
                proxy.notExustedMethod(10);
            } catch (MethodInvocationRuntimeException e) {
                assertTrue(e.getMessage().contains("Service 'RulesFrontendTest_multimodule' hasn't been found"));
                throw e;
            }
        } finally {
            ruleServicePublisher.deploy(openLService);
        }
    }

    public interface ProxyInterface {
        String worldHello(int arg);

        String notExustedMethod(int arg);
    }
}
