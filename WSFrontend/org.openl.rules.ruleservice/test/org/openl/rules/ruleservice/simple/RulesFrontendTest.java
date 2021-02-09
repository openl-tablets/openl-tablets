package org.openl.rules.ruleservice.simple;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.openl.rules.ruleservice.core.RuleServiceUndeployException;
import org.openl.rules.ruleservice.management.ServiceManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(properties = { "production-repository.uri=test-resources/RulesFrontendTest",
        "ruleservice.isProvideRuntimeContext=false",
        "production-repository.factory = repo-file"})
@ContextConfiguration({ "classpath:openl-ruleservice-beans.xml" })
public class RulesFrontendTest {

    @Autowired
    private RulesFrontend frontend;

    @Autowired
    private ServiceManager serviceManager;

    @Test
    public void testGetServices() {
        assertNotNull(serviceManager);
        assertNotNull(frontend);
        Collection<String> services = frontend.getServiceNames();
        assertNotNull(services);
        assertEquals(
            new HashSet<>(
                Arrays.asList("org.openl.rules.tutorial4.Tutorial4Interface", "simple/name", "RulesFrontendTest_multimodule")),
            new HashSet<>(services));
        assertEquals(3, services.size());
    }

    @Test
    public void testProxyServices() throws MethodInvocationException {
        Object result = frontend.execute("RulesFrontendTest_multimodule", "worldHello", 10);
        assertEquals("World, Good Morning!", result);
    }

    @Test(expected = org.openl.rules.ruleservice.simple.MethodInvocationException.class)
    public void testProxyServicesNotExistedMethod() throws MethodInvocationException {
        frontend.execute("RulesFrontendTest_multimodule", "notExistedMethod", 10);
    }

    @Test
    public void testProxyServicesNotExistedService() throws RuleServiceUndeployException, MethodInvocationException {
        assertEquals(3, frontend.getServiceNames().size());
        Object result = frontend.execute("RulesFrontendTest_multimodule", "worldHello", 10);
        assertEquals("World, Good Morning!", result);
        serviceManager.getServiceByDeploy("RulesFrontendTest/multimodule");

        serviceManager.undeploy("RulesFrontendTest/multimodule");
        assertEquals(Arrays.asList("org.openl.rules.tutorial4.Tutorial4Interface", "simple/name"),
            frontend.getServiceNames());

        try {
            frontend.execute("RulesFrontendTest_multimodule", "notExistedMethod", 10);
            fail();
        } catch (MethodInvocationException e) {
            assertTrue(e.getMessage().contains("Service 'RulesFrontendTest_multimodule' is not found."));
        }
    }
}
