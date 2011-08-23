package org.openl.ruleservice.publish;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.cxf.frontend.ServerFactoryBean;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.repository.ProductionRepositoryFactoryProxy;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.ServiceDescription;
import org.openl.rules.ruleservice.core.ServiceDescription.ModuleConfiguration;
import org.openl.rules.ruleservice.loader.IRulesLoader;
import org.openl.rules.ruleservice.management.IServiceConfigurer;
import org.openl.rules.ruleservice.management.ServiceManager;
import org.openl.rules.ruleservice.publish.RulesPublisher;
import org.openl.rules.workspace.WorkspaceUserImpl;
import org.openl.rules.workspace.deploy.impl.jcr.JcrProductionDeployer;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-ruleservice-beans.xml" })
public class WebServicesExposingTest implements ApplicationContextAware{
    private static final String TUTORIAL4_SERVICE_URL = "org.openl.tablets.tutorial4";
    
    private static final String MULTIMODULE_SERVICE_URL = "multimodule";
        
    //private static final String TEST_REPOSITORY_PATH = "./target/production-repository/";
    
    private ApplicationContext applicationContext;

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @AfterClass
    public static void clearRepository() throws Exception {
        ProductionRepositoryFactoryProxy.release();
    }
    
    @Test
    public void testServerPrototypes() {
        assertNotNull(applicationContext);
        ServiceManager serviceManager = applicationContext.getBean("serviceManager", ServiceManager.class);
        assertNotNull(serviceManager);
        serviceManager.start();
        WebServicesDeploymentAdmin deploymentAdmin = applicationContext.getBean("deploymentAdmin",
                WebServicesDeploymentAdmin.class);
        ServerFactoryBean firstServer = deploymentAdmin.getServerFactoryBean();
        ServerFactoryBean secondServer = deploymentAdmin.getServerFactoryBean();
        assertTrue(firstServer != secondServer);
    }

    @Test
    public void testRedeployAfterChanges() throws Exception {
        assertNotNull(applicationContext);
        ServiceManager serviceManager = applicationContext.getBean("serviceManager", ServiceManager.class);
        assertNotNull(serviceManager);
        IRulesLoader rulesLoader = serviceManager.getRulesLoader();
        serviceManager.start();
        OpenLService multimoduleService = serviceManager.getRuleService().findServiceByName("multimodule");
        //OpenLService tutorial4Service = serviceManager.getRuleService().findServiceByName("tutorial4");
        Deployment domainDeployment = rulesLoader.getDeployment("domain",
                TestConfigurer.getLastVersion(rulesLoader, "domain"));

        ADeploymentProject testDeploymentProject = new ADeploymentProject(domainDeployment.getAPI(), null);
        new JcrProductionDeployer(new WorkspaceUserImpl("test")).deploy(testDeploymentProject,
                domainDeployment.getProjects());
        for (int i = 0; i < 12; i++) {//waiting for redeploying of services during.
            Thread.sleep(5000); // notifications come asynchroniously
            if (multimoduleService != serviceManager.getRuleService().findServiceByName("multimodule")) {
                break;
            }
        }
        assertEquals(2, applicationContext.getBean("rulesPublisher", RulesPublisher.class).getRunningServices().size());
        assertNotSame(multimoduleService, serviceManager.getRuleService().findServiceByName("multimodule"));
        //uncomment after the smart redeployment will be implemented
        //assertSame(tutorial4Service, serviceManager.getRuleService().findServiceByName("tutorial4"));
    }
    
    public static void main(String[] args) throws Exception {
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext(
                "classpath:test-ruleservice-beans.xml");
        ServiceManager serviceManager = applicationContext.getBean("serviceManager", ServiceManager.class);
        serviceManager.start();
        System.out.print("Press enter for server stop:");
        System.in.read();
        System.out.println("Server is stoped");
        System.exit(0);
    }

    public static class TestConfigurer implements IServiceConfigurer {
        private static CommonVersion getLastVersion(IRulesLoader loader, String deploymentName) {
            CommonVersion lastVersion = new CommonVersionImpl(0, 0, 0);
            for (Deployment deployment : loader.getDeployments()) {
                if (deployment.getDeploymentName().equals(deploymentName)) {
                    if (lastVersion.compareTo(deployment.getCommonVersion()) < 0) {
                        lastVersion = deployment.getCommonVersion();
                    }
                }
            }
            return lastVersion;
        }

        private ServiceDescription resolveTutorial4Service(IRulesLoader loader) {
            final String deploymentName = "org.openl.tablets.tutorial4";
            List<ModuleConfiguration> modules = new ArrayList<ServiceDescription.ModuleConfiguration>(1);
            modules.add(new ModuleConfiguration(deploymentName, getLastVersion(loader, deploymentName), deploymentName,
                    "Tutorial 4 - UServ Product Derby"));
            return new ServiceDescription("tutorial4", TUTORIAL4_SERVICE_URL, "org.openl.rules.tutorial4.Tutorial4Interface" , false, modules);
        }

        private ServiceDescription resolveMultimoduleService(IRulesLoader loader) {
            final String multiModuleDeploymentName = "multimodule";
            final String domainDeploymentName = "domain";
            List<ModuleConfiguration> modules = new ArrayList<ServiceDescription.ModuleConfiguration>(1);
            modules.add(new ModuleConfiguration(domainDeploymentName, getLastVersion(loader, domainDeploymentName),
                    domainDeploymentName, "Domain"));
            modules.add(new ModuleConfiguration(multiModuleDeploymentName, getLastVersion(loader,
                    multiModuleDeploymentName), "project1", "Module1_1"));
            modules.add(new ModuleConfiguration(multiModuleDeploymentName, getLastVersion(loader,
                    multiModuleDeploymentName), "project2", "Module2_1"));
            modules.add(new ModuleConfiguration(multiModuleDeploymentName, getLastVersion(loader,
                    multiModuleDeploymentName), "project3", "Module3_1"));
            return new ServiceDescription("multimodule", MULTIMODULE_SERVICE_URL, null, false, modules);
        }

        public List<ServiceDescription> getServicesToBeDeployed(IRulesLoader loader) {
            List<ServiceDescription> services = new ArrayList<ServiceDescription>();
            services.add(resolveTutorial4Service(loader));
            services.add(resolveMultimoduleService(loader));
            return services;
        }
    }

}
