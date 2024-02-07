package org.openl.rules.ruleservice.publish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import org.openl.rules.ruleservice.management.ServiceManager;
import org.openl.rules.ruleservice.rmi.DefaultRmiHandler;

@TestPropertySource(properties = {"ruleservice.isProvideRuntimeContext=false",
        "ruleservice.rmiPort=31099",
        "ruleservice.publishers=RMI",
        "production-repository.uri=test-resources/DefaultRmiHandlerTest",
        "production-repository.factory = repo-file"})
@SpringJUnitConfig(locations = {"classpath:openl-ruleservice-beans.xml"})
public class DefaultRmiHandlerTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    public void test() throws Exception {
        assertNotNull(applicationContext);
        ServiceManager serviceManager = applicationContext.getBean("serviceManager", ServiceManager.class);
        assertNotNull(serviceManager);

        Registry registry = LocateRegistry.getRegistry(31099);
        DefaultRmiHandler defaultRmiHandler = (DefaultRmiHandler) registry
                .lookup("DefaultRmiHandlerTest/simpleProject");

        assertNotNull(defaultRmiHandler);

        String result = (String) defaultRmiHandler
                .execute("baseHello", new Class<?>[]{int.class}, new Object[]{10});

        assertEquals("Good Morning", result);

    }
}
