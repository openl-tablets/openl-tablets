package org.openl.rules.ruleservice;

import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.ruleservice.loader.RulesLoader;
import org.openl.rules.ruleservice.publish.WebServicesDeployAdmin;
import org.openl.rules.ruleservice.publish.RulesPublisher;
import org.openl.rules.ruleservice.resolver.RulesProjectResolver;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Application main class. Sets up other classes.
 */
public class RuleServiceMain extends RuleServiceBase {

    public RuleServiceMain() {
        loader = new RulesLoader();
        deployAdmin = new WebServicesDeployAdmin();
        resolver = new RulesProjectResolver();
        publisher = new RulesPublisher();
    }

    public static void main(String[] args) throws RRepositoryException, InterruptedException, Exception {
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext("cxf.xml");

        WebServicesDeployAdmin admin;
        if (applicationContext.containsBean("deploymentAdmin")) {
            admin = (WebServicesDeployAdmin) applicationContext.getBean("deploymentAdmin");
        } else {
            admin = new WebServicesDeployAdmin();

        }
        RuleServiceMain ruleService = new RuleServiceMain();
        ruleService.setDeployAdmin(admin);
        ruleService.runFrontend();
    }
}
