package org.openl.rules.ruleservice;

import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.ruleservice.loader.RulesLoader;
import org.openl.rules.ruleservice.publish.WebServicesDeployAdmin;
import org.openl.rules.ruleservice.publish.RulesPublisher;
import org.openl.rules.ruleservice.resolver.RulesProjectResolver;

/**
 * Application main class. Sets up other classes.
 */
public class RuleServiceMain extends RuleServiceBase {

    private static void init() throws Exception {
        loader = new RulesLoader();
        deployAdmin = new WebServicesDeployAdmin();
        resolver = new RulesProjectResolver();
        publisher = new RulesPublisher();
    }

    public static void main(String[] args) throws RRepositoryException, InterruptedException, Exception {
        init();
        runFrontend();
    }

}
