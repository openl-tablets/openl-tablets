package org.openl.rules.ruleservice.publish;

import org.apache.cxf.frontend.ServerFactoryBean;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.junit.Assert.*;

public class WebServicesDeployAdminTest {
    @Test
    public void testServerPrototypes() {
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext("openl-ws-test.xml");
        WebServicesDeployAdmin admin = (WebServicesDeployAdmin) applicationContext.getBean("deploymentAdmin");
        ServerFactoryBean firstServer = admin.getServerFactoryBean();
        ServerFactoryBean secondServer = admin.getServerFactoryBean();
        assertTrue(firstServer != secondServer);
        System.out.println(firstServer.getDataBinding());
    }
}
