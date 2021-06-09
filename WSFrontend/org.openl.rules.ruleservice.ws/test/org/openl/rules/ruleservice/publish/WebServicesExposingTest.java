package org.openl.rules.ruleservice.publish;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;

import java.lang.reflect.Field;
import java.util.ArrayList;

import org.apache.cxf.frontend.ServerFactoryBean;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.ruleservice.core.DeploymentDescription;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.OpenLService.OpenLServiceBuilder;
import org.openl.rules.ruleservice.core.ServiceDescription;
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
        Field serviceDescriptionInProcessField = ServiceManagerImpl.class
            .getDeclaredField("serviceDescriptionInProcess");
        serviceDescriptionInProcessField.setAccessible(true);

        Field openLServiceInProcessField = ServiceManagerImpl.class.getDeclaredField("openLServiceInProcess");
        openLServiceInProcessField.setAccessible(true);

        try {
            ServiceDescription serviceDescription = new ServiceDescription.ServiceDescriptionBuilder().setName("mock")
                .setResourceLoader(location -> null)
                .setModules(new ArrayList<>())
                .setDeployment(new DeploymentDescription("mock", new CommonVersionImpl(1)))
                .build();

            serviceDescriptionInProcessField.set(serviceManager, serviceDescription);

            OpenLServiceBuilder openLServiceBuilder = new OpenLService.OpenLServiceBuilder();
            openLServiceBuilder.setName("mock")
                    .setDeployPath("mock")
                    .setDeployment(new DeploymentDescription("mock", new CommonVersionImpl("0")));
            OpenLService openLService = openLServiceBuilder.build(openLService1 -> {
            });
            openLServiceInProcessField.set(serviceManager, openLService);

            ServerFactoryBean firstServer = webServicesRuleServicePublisher.getServerFactoryBeanObjectFactory()
                .getObject();
            ServerFactoryBean secondServer = webServicesRuleServicePublisher.getServerFactoryBeanObjectFactory()
                .getObject();
            assertNotSame(firstServer, secondServer);
        } finally {
            serviceDescriptionInProcessField.set(serviceManager, null);
            openLServiceInProcessField.set(serviceManager, null);
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
