package org.openl.rules.ruleservice;

import org.openl.rules.repository.exceptions.RRepositoryException;

import java.io.File;

public class RuleServiceMain {
    public static void main(String[] args) throws RRepositoryException, InterruptedException {
        final RulesWebServicesPublisher app = new RulesWebServicesPublisher();
        app.setTempFolder(new File("/tmp/ws-deploy"));
        app.setDeployAdmin(new WebServicesDeployAdmin());

        app.init();

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                try {
                    app.destroy();
                } catch (RRepositoryException e) {}
            }
        }));

        Thread.sleep(Long.MAX_VALUE);
    }
}
