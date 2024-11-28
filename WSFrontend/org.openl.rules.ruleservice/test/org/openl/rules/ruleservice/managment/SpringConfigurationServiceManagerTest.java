package org.openl.rules.ruleservice.managment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import org.openl.rules.context.RulesRuntimeContextFactory;
import org.openl.rules.ruleservice.management.ServiceManagerImpl;
import org.openl.rules.ruleservice.simple.MethodInvocationException;
import org.openl.rules.ruleservice.simple.RulesFrontend;

@TestPropertySource(properties = {
        "production-repository.factory = repo-zip",
        "production-repository.uri = test-resources/openl-repository/deploy"})
@SpringJUnitConfig(locations = {"classpath:openl-ruleservice-beans.xml"})
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
                RulesRuntimeContextFactory.buildRulesRuntimeContext(), "Provisional");
        assertTrue(object instanceof Double);
        assertEquals(50.0, (Double) object, 0.01);
    }

    @Test
    public void testExceptionFramework() throws Exception {
        assertThrows(MethodInvocationException.class, () -> {
            assertNotNull(applicationContext);
            ServiceManagerImpl serviceManager = applicationContext.getBean(ServiceManagerImpl.class);
            assertNotNull(serviceManager);
            RulesFrontend frontend = applicationContext.getBean(RulesFrontend.class);
            assertNotNull(frontend);
            frontend.execute("ErrorTest_ErrorTest",
                    "vehicleEligibilityScore",
                    RulesRuntimeContextFactory.buildRulesRuntimeContext(), "test");
        });
    }
}
