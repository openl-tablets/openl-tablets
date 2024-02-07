package org.openl.rules.ruleservice.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import org.openl.rules.ruleservice.management.ServiceManager;
import org.openl.rules.ruleservice.simple.RulesFrontend;

@TestPropertySource(properties = {
        "production-repository.uri=test-resources/RuleServiceDeploymentRelatedDependencyManagerTest",
        "ruleservice.isProvideRuntimeContext=false",
        "production-repository.factory = repo-file"})
@SpringJUnitConfig(locations = {"classpath:openl-ruleservice-beans.xml"})
public class RuleServiceDeploymentRelatedDependencyManagerTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    public void testInstantiation() throws Exception {
        assertNotNull(applicationContext);
        ServiceManager serviceManager = applicationContext.getBean("serviceManager", ServiceManager.class);
        assertNotNull(serviceManager);
        RulesFrontend frontend = applicationContext.getBean("frontend", RulesFrontend.class);
        assertTrue(
                ((String) frontend.execute("RuleServiceDeploymentRelatedDependencyManagerTest_project1", "printJavaBean"))
                        .contains("project1"));
        assertTrue(
                ((String) frontend.execute("RuleServiceDeploymentRelatedDependencyManagerTest_project1", "printJavaBean"))
                        .contains("javabean"));
        assertEquals("project1javabean1",
                frontend.execute("RuleServiceDeploymentRelatedDependencyManagerTest_project1", "printJavaBean"));
        assertTrue(
                ((String) frontend.execute("RuleServiceDeploymentRelatedDependencyManagerTest_project1", "printDatatype"))
                        .contains("project1"));
        assertTrue(
                ((String) frontend.execute("RuleServiceDeploymentRelatedDependencyManagerTest_project1", "printDatatype"))
                        .contains("datatype"));
        assertEquals("project1datatypefalse",
                frontend.execute("RuleServiceDeploymentRelatedDependencyManagerTest_project1", "printDatatype"));
        assertTrue(
                ((String) frontend.execute("RuleServiceDeploymentRelatedDependencyManagerTest_project2", "printJavaBean"))
                        .contains("project2"));
        assertTrue(
                ((String) frontend.execute("RuleServiceDeploymentRelatedDependencyManagerTest_project2", "printJavaBean"))
                        .contains("javabean"));
        assertEquals("project2javabean1.0",
                frontend.execute("RuleServiceDeploymentRelatedDependencyManagerTest_project2", "printJavaBean"));
        assertTrue(
                ((String) frontend.execute("RuleServiceDeploymentRelatedDependencyManagerTest_project2", "printDatatype"))
                        .contains("project2"));
        assertTrue(
                ((String) frontend.execute("RuleServiceDeploymentRelatedDependencyManagerTest_project2", "printDatatype"))
                        .contains("datatype"));
        assertEquals("project2datatypefalse",
                frontend.execute("RuleServiceDeploymentRelatedDependencyManagerTest_project2", "printDatatype"));
        assertEquals("project2javabean1.0",
                frontend.execute("RuleServiceDeploymentRelatedDependencyManagerTest_multimodule", "printJavaBeanSecond"));
    }
}
