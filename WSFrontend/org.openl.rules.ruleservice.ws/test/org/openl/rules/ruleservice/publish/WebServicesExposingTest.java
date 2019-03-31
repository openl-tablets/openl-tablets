package org.openl.rules.ruleservice.publish;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.cxf.frontend.ServerFactoryBean;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openl.rules.ruleservice.management.ServiceManagerImpl;
import org.openl.spring.env.PropertySourcesLoader;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration( initializers = PropertySourcesLoader.class, locations = { "classpath:openl-ruleservice-beans.xml", "classpath:openl-ruleservice-logging-beans.xml" })
public class WebServicesExposingTest implements ApplicationContextAware {
    private ApplicationContext applicationContext;

    @Override
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

    public static void main(String[] args) throws Exception {
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext(
                "classpath:openl-ruleservice-beans.xml");
        ServiceManagerImpl serviceManager = applicationContext.getBean("serviceManager", ServiceManagerImpl.class);
        serviceManager.start();
        System.out.print("Press enter for server stop:");
        System.in.read();
        System.out.println("Server has been stoped");
    }
}
