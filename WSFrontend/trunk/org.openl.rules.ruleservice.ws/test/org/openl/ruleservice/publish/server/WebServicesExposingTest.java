package org.openl.ruleservice.publish.server;

import org.openl.rules.ruleservice.management.ServiceManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class WebServicesExposingTest {
    public static void main(String[] args) throws Exception {
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext(
                "classpath:test-ruleservice-beans.xml");
        ServiceManager serviceManager = applicationContext.getBean("serviceManager", ServiceManager.class);
        serviceManager.start();
        System.out.print("Press enter for server stop:");
        System.in.read();
        System.out.println("Server stoped");
        System.exit(0);
    }
}
