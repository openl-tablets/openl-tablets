package org.openl.rules.ruleservice.multimodule;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import org.openl.rules.ruleservice.simple.RulesFrontend;

@TestPropertySource(properties = {"production-repository.uri=test-resources/MultipleProjectsInDeploymentTest",
		"production-repository.factory = repo-file"})
@SpringJUnitConfig(locations = {"classpath:openl-ruleservice-beans.xml"})
public class MultipleProjectsInDeploymentTest {

    @Autowired
    private RulesFrontend frontend;

    @Test
    public void testInvoke() throws Exception {
        assertEquals("Hello First world", frontend.execute("first-hello", "sayHello"));
        assertEquals("Hello Second world", frontend.execute("second-hello", "sayHello"));
        assertEquals("Hello First world", frontend.execute("third-hello", "sayHello"));
    }

}
