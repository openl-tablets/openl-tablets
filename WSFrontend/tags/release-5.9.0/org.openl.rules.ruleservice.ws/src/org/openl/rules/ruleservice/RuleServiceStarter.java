package org.openl.rules.ruleservice;

import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.ruleservice.management.ServiceManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Application main class. Sets up other classes.
 */
public class RuleServiceStarter {
    private RuleServiceStarter() {
    }

    public static void startServicesFromClasspath() {
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext("openl-ruleservice-beans.xml");
        final ServiceManager serviceManager = (ServiceManager) applicationContext.getBean("serviceManager");
        serviceManager.start();
        System.out.println("Type \"exit\" to stop services.");
        while ("exit".equals(System.console().readLine())) {
        }
    }

    public static void main(String[] args) throws RRepositoryException {
        startServicesFromClasspath();
    }
}
