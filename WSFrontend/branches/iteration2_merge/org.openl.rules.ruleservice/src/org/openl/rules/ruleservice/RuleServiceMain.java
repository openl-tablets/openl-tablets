package org.openl.rules.ruleservice;

import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.repository.RDeploymentListener;
import org.openl.rules.ruleservice.loader.DeploymentInfo;
import org.openl.rules.ruleservice.loader.LoadingListener;
import org.openl.rules.ruleservice.loader.RulesLoader;
import org.openl.rules.ruleservice.publish.DeploymentAdmin;
import org.openl.rules.ruleservice.publish.WebServicesDeployAdmin;
import org.openl.rules.ruleservice.publish.RulesPublisher;
import org.openl.rules.ruleservice.resolver.RulesProjectResolver;
import org.openl.rules.workspace.production.client.JcrRulesClient;
import org.openl.SmartProps;

import java.io.File;

/**
 * Application main class. Sets up other classes. 
 */
public class RuleServiceMain extends RuleServiceBase{
    

    public static void main(String[] args) throws RRepositoryException, InterruptedException {
        init();
        runFrontend();
    }

    private static void init() {
        loader = new RulesLoader();
        deployAdmin = new WebServicesDeployAdmin();
        resolver = new RulesProjectResolver();
        publisher = new RulesPublisher();
    }


}
