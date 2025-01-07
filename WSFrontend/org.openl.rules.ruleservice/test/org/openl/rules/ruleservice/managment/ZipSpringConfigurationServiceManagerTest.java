package org.openl.rules.ruleservice.managment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import org.openl.rules.ruleservice.simple.MethodInvocationException;
import org.openl.rules.ruleservice.simple.RulesFrontend;

@TestPropertySource(properties = {
        "production-repository.factory = repo-zip",
        "production-repository.uri = test-resources/openl-repository/deploy",
        "production-repository.archives = ErrorTest/ErrorTest, org.openl.tablets.tutorial4/org.openl.tablets.tutorial4"})
@SpringJUnitConfig(locations = {"classpath:openl-ruleservice-beans.xml"})
@DirtiesContext
public class ZipSpringConfigurationServiceManagerTest {

    @Autowired
    private RulesFrontend frontend;

    @Test
    public void testServiceManager() throws MethodInvocationException {
        Object object = frontend.execute("org.openl.tablets_org.openl.tablets",
                "vehicleEligibilityScore",
                "Provisional");
        assertTrue(object instanceof Double);
        assertEquals(50.0, (Double) object, 0.01);
    }

    @Test
    public void testExceptionFramework() throws Exception {
        assertThrows(MethodInvocationException.class, () -> {
            frontend.execute("ErrorTest_ErrorTest",
                    "vehicleEligibilityScore",
                    "test");
        });
    }

}
