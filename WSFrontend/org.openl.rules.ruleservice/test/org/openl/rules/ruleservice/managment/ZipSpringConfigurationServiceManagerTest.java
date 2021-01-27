package org.openl.rules.ruleservice.managment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.openl.rules.context.RulesRuntimeContextFactory;
import org.openl.rules.ruleservice.simple.MethodInvocationException;
import org.openl.rules.ruleservice.simple.RulesFrontend;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(properties = {
        "production-repository.factory = repo-zip",
        "production-repository.uri = test-resources/openl-repository/deploy",
        "production-repository.archives = ErrorTest/ErrorTest, org.openl.tablets.tutorial4/org.openl.tablets.tutorial4"})
@ContextConfiguration(locations = { "classpath:openl-ruleservice-beans.xml" })
@DirtiesContext
public class ZipSpringConfigurationServiceManagerTest {

    @Autowired
    private RulesFrontend frontend;

    @Test
    public void testServiceManager() throws MethodInvocationException {
        Object object = frontend.execute("org.openl.tablets_org.openl.tablets",
                "vehicleEligibilityScore",
                RulesRuntimeContextFactory.buildRulesRuntimeContext(), "Provisional");
        assertTrue(object instanceof org.openl.meta.DoubleValue);
        org.openl.meta.DoubleValue value = (org.openl.meta.DoubleValue) object;
        assertEquals(50.0, value.getValue(), 0.01);
    }

    @Test(expected = MethodInvocationException.class)
    public void testExceptionFramework() throws Exception {
        frontend.execute("ErrorTest_ErrorTest",
                "vehicleEligibilityScore",
                RulesRuntimeContextFactory.buildRulesRuntimeContext(), "test");
    }

}
