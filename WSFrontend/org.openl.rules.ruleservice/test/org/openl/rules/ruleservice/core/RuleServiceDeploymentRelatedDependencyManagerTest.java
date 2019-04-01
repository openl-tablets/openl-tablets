package org.openl.rules.ruleservice.core;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.openl.rules.ruleservice.management.ServiceManager;
import org.openl.rules.ruleservice.simple.RulesFrontend;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(properties = {
        "ruleservice.datasource.dir=test-resources/RuleServiceDeploymentRelatedDependencyManagerTest",
        "ruleservice.datasource.deploy.clean.datasource=false",
        "ruleservice.isProvideRuntimeContext=false" })
@ContextConfiguration({ "classpath:openl-ruleservice-beans.xml" })
public class RuleServiceDeploymentRelatedDependencyManagerTest implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

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
