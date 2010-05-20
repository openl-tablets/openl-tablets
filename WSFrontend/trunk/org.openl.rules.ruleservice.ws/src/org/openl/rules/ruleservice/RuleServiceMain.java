package org.openl.rules.ruleservice;

import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.ruleservice.loader.RulesLoaderJcr;
import org.openl.rules.ruleservice.publish.DeploymentAdmin;
import org.openl.rules.ruleservice.publish.WebServicesDeployAdmin;
import org.openl.rules.ruleservice.publish.RulesPublisher;
import org.openl.rules.ruleservice.resolver.RulesProjectResolver;
import org.openl.rules.workspace.production.client.JcrRulesClient;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Application main class. Sets up other classes.
 */
public class RuleServiceMain extends RuleServiceBase {

    public RuleServiceMain() throws RRepositoryException {
        loader = new RulesLoaderJcr(new JcrRulesClient());
        publisher = new RulesPublisher();
        publisher.setRulesProjectResolver(new RulesProjectResolver());
        publisher.setDeployAdmin(getDeploymentAdmin());
    }
    
    private DeploymentAdmin getDeploymentAdmin(){
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext("cxf.xml");
        WebServicesDeployAdmin admin;
        if (applicationContext.containsBean("deploymentAdmin")) {
            admin = (WebServicesDeployAdmin) applicationContext.getBean("deploymentAdmin");
        } else {
            admin = new WebServicesDeployAdmin();

        }
        return admin;
    }

    public static void main(String[] args) throws RRepositoryException{
        RuleServiceMain ruleService = new RuleServiceMain();
        ruleService.run();
    }
}
