package org.openl.ruleservice.publish.server;

import org.openl.rules.ruleservice.management.ServiceManagerImpl;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class WebServicesExposingTest {
    public static void main(String[] args) throws Exception {
        @SuppressWarnings("resource")
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext(
            "classpath:openl-ruleservice-beans.xml");
        ServiceManagerImpl serviceManager = applicationContext.getBean("serviceManager", ServiceManagerImpl.class);
        serviceManager.start();
        System.out.print("Press enter for server stop:");
        System.in.read();
        System.out.println("Server has been stopped.");
    }
}
