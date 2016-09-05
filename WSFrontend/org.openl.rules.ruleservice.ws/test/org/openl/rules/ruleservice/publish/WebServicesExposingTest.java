package org.openl.rules.ruleservice.publish;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import org.apache.cxf.frontend.ServerFactoryBean;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.repository.ProductionRepositoryFactoryProxy;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.management.ServiceManagerImpl;
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
    private ApplicationContext applicationContext;

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
    
    @Test
    public void testServerPrototypes() {
        assertNotNull(applicationContext);
        ServiceManagerImpl serviceManager = applicationContext.getBean("serviceManager", ServiceManagerImpl.class);
        assertNotNull(serviceManager);
        serviceManager.start();
        JAXWSRuleServicePublisher webServicesRuleServicePublisher = applicationContext.getBean(
                JAXWSRuleServicePublisher.class);
        ServerFactoryBean firstServer = webServicesRuleServicePublisher.getServerFactoryBean();
        ServerFactoryBean secondServer = webServicesRuleServicePublisher.getServerFactoryBean();
        assertTrue(firstServer != secondServer);
    }

    @Test
    @Ignore
    public void testRedeployAfterChanges() throws Exception {
        assertNotNull(applicationContext);
        ServiceManagerImpl serviceManager = applicationContext.getBean("serviceManager", ServiceManagerImpl.class);
        assertNotNull(serviceManager);
        serviceManager.start();
        OpenLService multimoduleService = serviceManager.getRuleService().getServiceByName("multimodule");
        // RuleServiceLoader rulesLoader = serviceManager.getRuleServiceLoader();
        Deployment domainDeployment = null; //rulesLoader.getDeployment("multimodule", new CommonVersionImpl(1));
        ADeploymentProject testDeploymentProject = new ADeploymentProject(domainDeployment.getAPI(), null);
        ProductionRepositoryFactoryProxy proxy = applicationContext.getBean("productionRepositoryFactoryProxy", ProductionRepositoryFactoryProxy.class);
        new JcrProductionDeployer(proxy, ProductionRepositoryFactoryProxy.DEFAULT_REPOSITORY_PROPERTIES_FILE).deploy(testDeploymentProject,
                domainDeployment.getProjects(), new WorkspaceUserImpl("test"));
        for (int i = 0; i < 12; i++) {// waiting for redeploying of services
                                      // during.
            Thread.sleep(5000); // notifications come asynchroniously
            if (multimoduleService != serviceManager.getRuleService().getServiceByName("multimodule")) {
                break;
            }
        }
        assertEquals(2, applicationContext.getBean(JAXWSRuleServicePublisher.class).getServices()
                .size());
        assertNotSame(multimoduleService, serviceManager.getRuleService().getServiceByName("multimodule"));
    }

    public static void main(String[] args) throws Exception {
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext(
                "classpath:openl-ruleservice-beans.xml");
        ServiceManagerImpl serviceManager = applicationContext.getBean("serviceManager", ServiceManagerImpl.class);
        serviceManager.start();
        System.out.print("Press enter for server stop:");
        System.in.read();
        System.out.println("Server is stoped");
    }
}
