package org.openl.rules.ruleservice.publish;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.openl.engine.OpenLSystemProperties;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.context.RulesRuntimeContextFactory;
import org.openl.rules.ruleservice.management.ServiceManager;
import org.openl.rules.ruleservice.simple.RulesFrontend;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(properties = { "production-repository.uri=test-resources/MultiModuleDispatchingTest",
        "production-repository.factory = repo-file"})
@ContextConfiguration({ "classpath:openl-ruleservice-beans.xml" })
public class MultiModuleDispatchingTest {
    private static final String SERVICE_NAME = "MultiModuleDispatchingTest_multimodule";

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    public void testMultiModuleService2() throws Exception {
        assertNotNull(applicationContext);
        ServiceManager serviceManager = applicationContext.getBean("serviceManager", ServiceManager.class);
        assertNotNull(serviceManager);
        RulesFrontend frontend = applicationContext.getBean("frontend", RulesFrontend.class);
        IRulesRuntimeContext cxt = RulesRuntimeContextFactory.buildRulesRuntimeContext();

        // dispatcher table
        System.setProperty(OpenLSystemProperties.DISPATCHING_MODE_PROPERTY, OpenLSystemProperties.DISPATCHING_MODE_DT);
        cxt.setLob("lob1_1");
        assertEquals("Hello1", frontend.execute(SERVICE_NAME, "hello", cxt));
        cxt.setLob("lob2_1");
        assertEquals("Hello2", frontend.execute(SERVICE_NAME, "hello", cxt));
        cxt.setLob("lob3_1");
        assertEquals("Hello3", frontend.execute(SERVICE_NAME, "hello", cxt));

        // dispatching by java code
        System.setProperty(OpenLSystemProperties.DISPATCHING_MODE_PROPERTY,
            OpenLSystemProperties.DISPATCHING_MODE_JAVA);
        cxt.setLob("lob1_1");
        assertEquals("Hello1", frontend.execute(SERVICE_NAME, "hello", cxt));
        cxt.setLob("lob2_1");
        assertEquals("Hello2", frontend.execute(SERVICE_NAME, "hello", cxt));
        cxt.setLob("lob3_1");
        assertEquals("Hello3", frontend.execute(SERVICE_NAME, "hello", cxt));

    }

}
