package org.openl.rules.ruleservice.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.resolving.InitializingModuleListener;
import org.openl.rules.ruleservice.management.ServiceManager;
import org.openl.rules.ruleservice.simple.RulesFrontend;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.PropertiesLoader;
import org.openl.rules.table.properties.TableProperties;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:RuleServiceDeploymentRelatedDependencyManagerTest/openl-ruleservice-beans.xml" })
@Ignore
public class RuleServiceDeploymentRelatedDependencyManagerTest implements ApplicationContextAware {

    public static class TestInitializingListener implements InitializingModuleListener {
        @Override
        public void afterModuleLoad(Module module) {
            ITableProperties props2 = new TableProperties();
            props2.setLob(module.getProject().getName());
            Map<String, Object> params = new HashMap<String, Object>();
            params.put(PropertiesLoader.EXTERNAL_MODULE_PROPERTIES_KEY, props2);
            module.setProperties(params);
        }
    }

    private ApplicationContext applicationContext;

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Test
    public void testInstantiation() throws Exception {
        assertNotNull(applicationContext);
        ServiceManager serviceManager = applicationContext.getBean("serviceManager", ServiceManager.class);
        assertNotNull(serviceManager);
        RulesFrontend frontend = applicationContext.getBean("frontend", RulesFrontend.class);
        assertTrue(((String) frontend.execute("RuleServiceDeploymentRelatedDependencyManagerTest_project1", "printJavaBean")).contains("project1"));
        assertTrue(((String) frontend.execute("RuleServiceDeploymentRelatedDependencyManagerTest_project1", "printJavaBean")).contains("javabean"));
        assertEquals("project1javabean1", frontend.execute("RuleServiceDeploymentRelatedDependencyManagerTest_project1", "printJavaBean"));
        assertTrue(((String) frontend.execute("RuleServiceDeploymentRelatedDependencyManagerTest_project1", "printDatatype")).contains("project1"));
        assertTrue(((String) frontend.execute("RuleServiceDeploymentRelatedDependencyManagerTest_project1", "printDatatype")).contains("datatype"));
        assertEquals("project1datatypefalse", frontend.execute("RuleServiceDeploymentRelatedDependencyManagerTest_project1", "printDatatype"));
        assertTrue(((String) frontend.execute("RuleServiceDeploymentRelatedDependencyManagerTest_project2", "printJavaBean")).contains("project2"));
        assertTrue(((String) frontend.execute("RuleServiceDeploymentRelatedDependencyManagerTest_project2", "printJavaBean")).contains("javabean"));
        assertEquals("project2javabean1.0", frontend.execute("RuleServiceDeploymentRelatedDependencyManagerTest_project2", "printJavaBean"));
        assertTrue(((String) frontend.execute("RuleServiceDeploymentRelatedDependencyManagerTest_project2", "printDatatype")).contains("project2"));
        assertTrue(((String) frontend.execute("RuleServiceDeploymentRelatedDependencyManagerTest_project2", "printDatatype")).contains("datatype"));
        assertEquals("project2datatypefalse", frontend.execute("RuleServiceDeploymentRelatedDependencyManagerTest_project2", "printDatatype"));
        assertEquals("project2javabean1.0", frontend.execute("RuleServiceDeploymentRelatedDependencyManagerTest_multimodule", "printJavaBeanSecond"));
    }
}
