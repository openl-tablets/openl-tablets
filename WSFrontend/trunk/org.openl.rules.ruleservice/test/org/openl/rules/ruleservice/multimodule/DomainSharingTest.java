package org.openl.rules.ruleservice.multimodule;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.openl.rules.common.CommonVersion;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.resolving.InitializingModuleListener;
import org.openl.rules.ruleservice.conf.ServiceConfigurer;
import org.openl.rules.ruleservice.core.ModuleDescription;
import org.openl.rules.ruleservice.core.ModuleDescription.ModuleDescriptionBuilder;
import org.openl.rules.ruleservice.core.ServiceDescription;
import org.openl.rules.ruleservice.core.ServiceDescription.ServiceDescriptionBuilder;
import org.openl.rules.ruleservice.loader.RuleServiceLoader;
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
@ContextConfiguration(locations = { "classpath:domain-sharing/openl-ruleservice-beans.xml" })
public class DomainSharingTest implements ApplicationContextAware {
    // public static interface ServiceClass {
    // String printJavaBean();
    //
    // String printDatatype();
    // }

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

    public static class TestServiceConfigurer implements ServiceConfigurer {

        @Override
        public Collection<ServiceDescription> getServicesToBeDeployed(RuleServiceLoader ruleServiceLoader) {
            Collection<ServiceDescription> serviceDescriptions = new ArrayList<ServiceDescription>();
            Deployment deployment = ruleServiceLoader.getDeployments().iterator().next();
            String deploymentName = deployment.getDeploymentName();
            CommonVersion deploymentVersion = deployment.getCommonVersion();
            ModuleDescription domainModuleDescription = new ModuleDescriptionBuilder()
                    .setDeploymentName(deploymentName).setDeploymentVersion(deploymentVersion).setProjectName("domain")
                    .setModuleName("domain").build();
            ModuleDescription project1ModuleDescription = new ModuleDescriptionBuilder()
                    .setDeploymentName(deploymentName).setDeploymentVersion(deploymentVersion)
                    .setProjectName("project1").setModuleName("Module1_1").build();
            ModuleDescription project2ModuleDescription = new ModuleDescriptionBuilder()
                    .setDeploymentName(deploymentName).setDeploymentVersion(deploymentVersion)
                    .setProjectName("project2").setModuleName("Module2_1").build();
            Set<ModuleDescription> modules = new HashSet<ModuleDescription>();
            modules.add(domainModuleDescription);
            modules.add(project1ModuleDescription);
            modules.add(project2ModuleDescription);
            serviceDescriptions.add(new ServiceDescriptionBuilder().addModuleInService(domainModuleDescription)
                    .addModuleInService(project1ModuleDescription).setModules(modules).setName("project1")
                    .setProvideRuntimeContext(false)
                    // .setServiceClassName(ServiceClass.class.getName())
                    .setUrl("project1").build());
            serviceDescriptions.add(new ServiceDescriptionBuilder().addModuleInService(domainModuleDescription)
                    .addModuleInService(project2ModuleDescription).setModules(modules).setName("project2")
                    .setProvideRuntimeContext(false)
                    // .setServiceClassName(ServiceClass.class.getName())
                    .setUrl("project2").build());
            serviceDescriptions.add(new ServiceDescriptionBuilder().addModuleInService(domainModuleDescription)
                    .addModuleInService(project1ModuleDescription).addModuleInService(project2ModuleDescription)
                    .setModules(modules).setName("multimodule").setProvideRuntimeContext(false)
                    // .setServiceClassName(ServiceClass.class.getName())
                    .setUrl("multimodule").build());
            return serviceDescriptions;
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
        serviceManager.start();
        RulesFrontend frontend = applicationContext.getBean("frontend", RulesFrontend.class);
        assertTrue(((String) frontend.execute("project1", "printJavaBean")).contains("project1"));
        assertTrue(((String) frontend.execute("project1", "printJavaBean")).contains("javabean"));
        assertEquals("project1javabean1", frontend.execute("project1", "printJavaBean"));
        assertTrue(((String) frontend.execute("project1", "printDatatype")).contains("project1"));
        assertTrue(((String) frontend.execute("project1", "printDatatype")).contains("datatype"));
        assertEquals("project1datatypefalse", frontend.execute("project1", "printDatatype"));
        assertTrue(((String) frontend.execute("project2", "printJavaBean")).contains("project2"));
        assertTrue(((String) frontend.execute("project2", "printJavaBean")).contains("javabean"));
        assertEquals("project2javabean1.0", frontend.execute("project2", "printJavaBean"));
        assertTrue(((String) frontend.execute("project2", "printDatatype")).contains("project2"));
        assertTrue(((String) frontend.execute("project2", "printDatatype")).contains("datatype"));
        assertEquals("project2datatypefalse", frontend.execute("project2", "printDatatype"));
        assertEquals("project2javabean1.0", frontend.execute("multimodule", "printJavaBeanSecond"));
    }
}
