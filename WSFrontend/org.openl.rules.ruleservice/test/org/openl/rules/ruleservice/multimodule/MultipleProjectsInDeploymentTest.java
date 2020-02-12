package org.openl.rules.ruleservice.multimodule;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.openl.rules.ruleservice.simple.RulesFrontend;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(properties = {"ruleservice.datasource.dir=test-resources/MultipleProjectsInDeploymentTest",
        "ruleservice.datasource.deploy.clean.datasource=false"})
@ContextConfiguration({"classpath:openl-ruleservice-beans.xml"})
public class MultipleProjectsInDeploymentTest {

    @Autowired
    RulesFrontend frontend;

    @Test
    public void testInvoke() throws Exception {
        assertEquals("Hello First world", frontend.execute("first-hello", "sayHello"));
        assertEquals("Hello Second world", frontend.execute("second-hello", "sayHello"));
        assertEquals("Hello First world", frontend.execute("third-hello", "sayHello"));
    }

}
