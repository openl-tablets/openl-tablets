package org.openl.rules.ruleservice.publish;

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
import org.openl.rules.ruleservice.core.ModuleDescription;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.ServiceDescription;
import org.openl.rules.ruleservice.loader.RuleServiceLoader;
import org.openl.rules.ruleservice.management.ServiceConfigurer;
import org.openl.rules.ruleservice.management.ServiceManagerImpl;
import org.openl.rules.ruleservice.publish.RuleServicePublisher;
import org.openl.rules.ruleservice.publish.WebServicesRuleServicePublisher;
import org.openl.rules.workspace.WorkspaceUserImpl;
import org.openl.rules.workspace.deploy.impl.jcr.JcrProductionDeployer;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:openl-ruleservice-beans.xml" })
public class WebServicesExposingTest implements ApplicationContextAware {
    private static final String TUTORIAL4_SERVICE_URL = "org.openl.tablets.tutorial4";

    private static final String MULTIMODULE_SERVICE_URL = "multimodule";

    // private static final String TEST_REPOSITORY_PATH =
    // "./target/production-repository/";

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
        ServiceManagerImpl serviceManager = applicationContext.getBean("serviceManager", ServiceManagerImpl.class);
        assertNotNull(serviceManager);
        serviceManager.start();
        WebServicesRuleServicePublisher webServicesRuleServicePublisher = applicationContext.getBean(
                WebServicesRuleServicePublisher.class);
        ServerFactoryBean firstServer = webServicesRuleServicePublisher.getServerFactoryBean();
        ServerFactoryBean secondServer = webServicesRuleServicePublisher.getServerFactoryBean();
        assertTrue(firstServer != secondServer);
    }

    @Test
    public void testRedeployAfterChanges() throws Exception {
        assertNotNull(applicationContext);
        ServiceManagerImpl serviceManager = applicationContext.getBean("serviceManager", ServiceManagerImpl.class);
        assertNotNull(serviceManager);
        RuleServiceLoader rulesLoader = serviceManager.getRuleServiceLoader();
        serviceManager.start();
        OpenLService multimoduleService = serviceManager.getRuleService().getServiceByName("multimodule");
        // OpenLService tutorial4Service =
        // serviceManager.getRuleService().findServiceByName("tutorial4");
        Deployment domainDeployment = rulesLoader.getDeployment("domain",
                TestConfigurer.getLastVersion(rulesLoader, "domain"));

        ADeploymentProject testDeploymentProject = new ADeploymentProject(domainDeployment.getAPI(), null);
        new JcrProductionDeployer(new WorkspaceUserImpl("test")).deploy(testDeploymentProject,
                domainDeployment.getProjects());
        for (int i = 0; i < 12; i++) {// waiting for redeploying of services
                                      // during.
            Thread.sleep(5000); // notifications come asynchroniously
            if (multimoduleService != serviceManager.getRuleService().getServiceByName("multimodule")) {
                break;
            }
        }
        assertEquals(2, applicationContext.getBean(RuleServicePublisher.class).getServices()
                .size());
        assertNotSame(multimoduleService, serviceManager.getRuleService().getServiceByName("multimodule"));
        // uncomment after the smart redeployment will be implemented
        // assertSame(tutorial4Service,
        // serviceManager.getRuleService().findServiceByName("tutorial4"));
    }

    public static void main(String[] args) throws Exception {
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext(
                "classpath:openl-ruleservice-beans.xml");
        ServiceManagerImpl serviceManager = applicationContext.getBean("serviceManager", ServiceManagerImpl.class);
        serviceManager.start();
        System.out.print("Press enter for server stop:");
        System.in.read();
        System.out.println("Server is stoped");
        System.exit(0);
    }

    public static class TestConfigurer implements ServiceConfigurer {
        private static CommonVersion getLastVersion(RuleServiceLoader loader, String deploymentName) {
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

        private ServiceDescription resolveTutorial4Service(RuleServiceLoader loader) {
            final String deploymentName = "org.openl.tablets.tutorial4";
            ServiceDescription.ServiceDescriptionBuilder builder = new ServiceDescription.ServiceDescriptionBuilder();
            builder.setName("tutorial4").setProvideRuntimeContext(false).setUrl(TUTORIAL4_SERVICE_URL)
                    .setServiceClassName("org.openl.rules.tutorial4.Tutorial4Interface");

            ModuleDescription.ModuleDescriptionBuilder moduleBuilder = new ModuleDescription.ModuleDescriptionBuilder();
            moduleBuilder.setDeploymentName(deploymentName);
            moduleBuilder.setDeploymentVersion(getLastVersion(loader, deploymentName));
            moduleBuilder.setModuleName("Tutorial 4 - UServ Product Derby");
            moduleBuilder.setProjectName(deploymentName);

            builder.addModule(moduleBuilder.build());

            return builder.build();
        }

        private ServiceDescription resolveMultimoduleService(RuleServiceLoader loader) {
            final String multiModuleDeploymentName = "multimodule";
            final String domainDeploymentName = "domain";
            ServiceDescription.ServiceDescriptionBuilder builder = new ServiceDescription.ServiceDescriptionBuilder();
            builder.setUrl(MULTIMODULE_SERVICE_URL).setName("multimodule").setProvideRuntimeContext(false)
                    .setServiceClassName(null);

            ModuleDescription.ModuleDescriptionBuilder moduleBuilder = new ModuleDescription.ModuleDescriptionBuilder()
                    .setDeploymentName(domainDeploymentName)
                    .setDeploymentVersion(getLastVersion(loader, domainDeploymentName))
                    .setProjectName(domainDeploymentName).setModuleName("Domain");

            builder.addModule(moduleBuilder.build());

            moduleBuilder.setDeploymentName(multiModuleDeploymentName).setDeploymentVersion(
                    getLastVersion(loader, multiModuleDeploymentName));

            moduleBuilder.setProjectName("project1").setModuleName("Module1_1");
            builder.addModule(moduleBuilder.build());
            moduleBuilder.setProjectName("project2").setModuleName("Module2_1");
            builder.addModule(moduleBuilder.build());
            moduleBuilder.setProjectName("project3").setModuleName("Module3_1");
            builder.addModule(moduleBuilder.build());

            return builder.build();
        }

        public List<ServiceDescription> getServicesToBeDeployed(RuleServiceLoader loader) {
            List<ServiceDescription> services = new ArrayList<ServiceDescription>();
            services.add(resolveTutorial4Service(loader));
            services.add(resolveMultimoduleService(loader));
            return services;
        }
    }

}
