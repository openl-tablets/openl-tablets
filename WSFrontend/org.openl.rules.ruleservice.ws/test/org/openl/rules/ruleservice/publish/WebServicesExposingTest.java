package org.openl.rules.ruleservice.publish;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.apache.cxf.frontend.ServerFactoryBean;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.ruleservice.core.DeploymentDescription;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.OpenLService.OpenLServiceBuilder;
import org.openl.rules.ruleservice.core.OpenLServiceHolder;
import org.openl.rules.ruleservice.core.OpenLServiceInitializer;
import org.openl.rules.ruleservice.core.Resource;
import org.openl.rules.ruleservice.core.ResourceLoader;
import org.openl.rules.ruleservice.core.RuleServiceInstantiationException;
import org.openl.rules.ruleservice.core.ServiceDescription;
import org.openl.rules.ruleservice.management.ServiceDescriptionHolder;
import org.openl.rules.ruleservice.management.ServiceManagerImpl;
import org.openl.spring.env.PropertySourcesLoader;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(initializers = PropertySourcesLoader.class, locations = { "classpath:openl-ruleservice-beans.xml",
        "classpath:openl-ruleservice-store-log-data-beans.xml" })
public class WebServicesExposingTest implements ApplicationContextAware {
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Test
    public void testServerPrototypes() throws Exception {
        assertNotNull(applicationContext);
        ServiceManagerImpl serviceManager = applicationContext.getBean("serviceManager", ServiceManagerImpl.class);
        assertNotNull(serviceManager);
        serviceManager.start();
        JAXWSRuleServicePublisher webServicesRuleServicePublisher = applicationContext
            .getBean(JAXWSRuleServicePublisher.class);
        assertNotNull(webServicesRuleServicePublisher);
        try {
            ServiceDescriptionHolder.getInstance()
                .setServiceDescription(new ServiceDescription.ServiceDescriptionBuilder().setName("mock")
                    .setResourceLoader(new ResourceLoader() {
                        @Override
                        public Resource getResource(String location) {
                            return null;
                        }
                    })
                    .setModules(new ArrayList<>())
                    .setDeployment(new DeploymentDescription("mock", new CommonVersionImpl(1)))
                    .build());

            OpenLServiceBuilder openLServiceBuilder = new OpenLService.OpenLServiceBuilder();
            openLServiceBuilder.setName("mock");
            OpenLService openLService = openLServiceBuilder.build(new OpenLServiceInitializer() {
                @Override
                public void ensureInitialization(OpenLService openLService) throws RuleServiceInstantiationException {
                }
            });

            OpenLServiceHolder.getInstance().setOpenLService(openLService);

            ServerFactoryBean firstServer = webServicesRuleServicePublisher.getServerFactoryBeanObjectFactory()
                .getObject();
            ServerFactoryBean secondServer = webServicesRuleServicePublisher.getServerFactoryBeanObjectFactory()
                .getObject();
            assertTrue(firstServer != secondServer);
        } finally {
            ServiceDescriptionHolder.getInstance().remove();
            OpenLServiceHolder.getInstance().remove();
        }
    }

    public static void main(String[] args) throws Exception {
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext(
            "classpath:openl-ruleservice-beans.xml",
            "classpath:openl-ruleservice-logging-beans.xml");
        ServiceManagerImpl serviceManager = applicationContext.getBean("serviceManager", ServiceManagerImpl.class);
        serviceManager.start();
        System.out.print("Press enter for server stop:");
        System.in.read();
        System.out.println("Server has been stoped");
    }
}
