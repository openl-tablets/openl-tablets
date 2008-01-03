package org.openl.rules.ruleservice;

import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.repository.RDeploymentListener;
import org.openl.rules.ruleservice.publish.WebServicesDeployAdmin;
import org.openl.rules.ruleservice.publish.RulesWebServicesPublisher;
import org.openl.rules.workspace.production.client.JcrRulesClient;

import java.io.File;

public class RuleServiceMain {
    public static void main(String[] args) throws RRepositoryException, InterruptedException {
        final JcrRulesClient rulesClient = new JcrRulesClient(); 

        RulesWebServicesPublisher app = new RulesWebServicesPublisher();
        app.setRulesClient(rulesClient);
        app.setTempFolder(new File("/tmp/ws-deploy"));
        app.setDeployAdmin(new WebServicesDeployAdmin());

        final PeriodicalExecutor executor = new PeriodicalExecutor(app);

        final RDeploymentListener listener = new RDeploymentListener() {
            public void projectsAdded() {
                executor.signal();
            }
        };
        rulesClient.addListener(listener);

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                try {
                    rulesClient.removeListener(listener);
                } catch (RRepositoryException e) {}

                try {
                    rulesClient.release();
                } catch (RRepositoryException e) {}
            }
        }));


        executor.execute();
    }
}
