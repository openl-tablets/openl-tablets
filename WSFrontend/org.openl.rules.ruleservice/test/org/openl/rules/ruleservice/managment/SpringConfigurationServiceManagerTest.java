package org.openl.rules.ruleservice.managment;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.openl.rules.context.RulesRuntimeContextFactory;
import org.openl.rules.ruleservice.management.ServiceManagerImpl;
import org.openl.rules.ruleservice.simple.MethodInvocationException;
import org.openl.rules.ruleservice.simple.RulesFrontend;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(properties = {
        "production-repository.factory = org.openl.rules.repository.file.FileSystemRepository",
        "production-repository.uri = test-resources/openl-repository",
        "version-in-deployment-name = true" })
@ContextConfiguration(locations = { "classpath:openl-ruleservice-beans.xml" })
@DirtiesContext
public class SpringConfigurationServiceManagerTest implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Test
    public void testServiceManager() throws MethodInvocationException {
        assertNotNull(applicationContext);
        ServiceManagerImpl serviceManager = applicationContext.getBean("serviceManager", ServiceManagerImpl.class);
        assertNotNull(serviceManager);
        RulesFrontend frontend = applicationContext.getBean(RulesFrontend.class);
        assertNotNull(frontend);
        Object object = frontend.execute("org.openl.tablets.tutorial4_org.openl.tablets.tutorial4",
            "vehicleEligibilityScore",
            new Object[] { RulesRuntimeContextFactory.buildRulesRuntimeContext(), "Provisional" });
        assertTrue(object instanceof org.openl.meta.DoubleValue);
        org.openl.meta.DoubleValue value = (org.openl.meta.DoubleValue) object;
        assertEquals(50.0, value.getValue(), 0.01);
    }

    @Test(expected = MethodInvocationException.class)
    public void testExceptionFramework() throws Exception {
        assertNotNull(applicationContext);
        ServiceManagerImpl serviceManager = applicationContext.getBean(ServiceManagerImpl.class);
        assertNotNull(serviceManager);
        RulesFrontend frontend = applicationContext.getBean(RulesFrontend.class);
        assertNotNull(frontend);
        frontend.execute("ErrorTest_ErrorTest",
            "vehicleEligibilityScore",
            new Object[] { RulesRuntimeContextFactory.buildRulesRuntimeContext(), "test" });
    }
}
