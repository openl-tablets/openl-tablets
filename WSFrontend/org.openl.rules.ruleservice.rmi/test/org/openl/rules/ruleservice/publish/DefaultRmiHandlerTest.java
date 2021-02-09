package org.openl.rules.ruleservice.publish;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openl.rules.ruleservice.management.ServiceManager;
import org.openl.rules.ruleservice.rmi.DefaultRmiHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(properties = { "ruleservice.isProvideRuntimeContext=false",
        "ruleservice.rmiPort=61099",
        "ruleservice.instantiation.strategy.lazy = false",
        "production-repository.uri=test-resources/DefaultRmiHandlerTest",
        "production-repository.factory = repo-file"})
@ContextConfiguration(locations = { "classpath:openl-ruleservice-beans.xml" })
public class DefaultRmiHandlerTest {

    @Autowired
    private ApplicationContext applicationContext;

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
