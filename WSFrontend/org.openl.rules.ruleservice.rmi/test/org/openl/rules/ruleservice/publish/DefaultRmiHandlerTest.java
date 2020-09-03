package org.openl.rules.ruleservice.publish;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openl.rules.ruleservice.management.ServiceManager;
import org.openl.rules.ruleservice.rmi.DefaultRmiHandler;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(properties = { "ruleservice.datasource.deploy.clean.datasource=false",
        "ruleservice.isProvideRuntimeContext=false",
        "ruleservice.rmiPort=61099",
        "ruleservice.instantiation.strategy.lazy = false",
        "ruleservice.datasource.dir=test-resources/DefaultRmiHandlerTest" })
@ContextConfiguration(locations = { "classpath:openl-ruleservice-beans.xml" })
public class DefaultRmiHandlerTest implements ApplicationContextAware {
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Test
    public void test() throws Exception {
        Assert.assertNotNull(applicationContext);
        ServiceManager serviceManager = applicationContext.getBean("serviceManager", ServiceManager.class);
        Assert.assertNotNull(serviceManager);

        Registry registry = LocateRegistry.getRegistry(61099);
        DefaultRmiHandler defaultRmiHandler = (DefaultRmiHandler) registry
            .lookup("DefaultRmiHandlerTest/simpleProject");

        Assert.assertNotNull(defaultRmiHandler);

        String result = (String) defaultRmiHandler
            .execute("baseHello", new Class<?>[] { int.class }, new Object[] { 10 });

        Assert.assertEquals("Good Morning", result);

    }
}
