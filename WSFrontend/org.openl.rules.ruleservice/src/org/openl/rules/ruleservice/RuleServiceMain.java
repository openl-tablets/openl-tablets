package org.openl.rules.ruleservice;

import org.openl.rules.repository.exceptions.RRepositoryException;

import java.io.File;

public class RuleServiceMain {
    public static void main(String[] args) throws RRepositoryException, InterruptedException {
        RulesWebServicesPublisher app = new RulesWebServicesPublisher();
        app.setTempFolder(new File("/tmp/ws-deploy"));
        try {
            app.init();
        } finally {
            app.destroy();
        }

        Thread.sleep(Long.MAX_VALUE);
    }
}
