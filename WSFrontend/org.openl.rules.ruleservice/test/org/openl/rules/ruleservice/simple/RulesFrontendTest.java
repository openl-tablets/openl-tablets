package org.openl.rules.ruleservice.simple;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.openl.rules.ruleservice.management.ServiceManager;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:RulesFrontendTest/openl-ruleservice-beans.xml" })
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

    public static interface ProxyInterface {
        String worldHello(int arg);

        String notExustedMethod(int arg);
    }
}
