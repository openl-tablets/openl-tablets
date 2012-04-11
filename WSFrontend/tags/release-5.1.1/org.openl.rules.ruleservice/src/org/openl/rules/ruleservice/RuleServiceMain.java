package org.openl.rules.ruleservice;

import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.repository.RDeploymentListener;
import org.openl.rules.ruleservice.publish.WebServicesDeployAdmin;
import org.openl.rules.ruleservice.publish.RulesWebServicesPublisher;
import org.openl.rules.workspace.production.client.JcrRulesClient;
import org.openl.SmartProps;

import java.io.File;

/**
 * Application main class. Sets up other classes. 
 */
public class RuleServiceMain {
    /**
     * Gets path to temporary directory. Extract the value with key <i>ruleservice.tmp.dir</i> from configuration file.
     * If such a key is missing returns default value <tt>/tmp/ws-deploy</tt>. 
     *
     * @return path to temporary directory
     */
    private static String getTempDirectory() {
        SmartProps props = new SmartProps("rules-production.properties");
        String value = props.getStr("ruleservice.tmp.dir");
        if (value == null || value.trim().length() == 0) {
            return "/tmp/ws-deploy";
        }
        
        return value;
    }

    public static void main(String[] args) throws RRepositoryException, InterruptedException {
        final JcrRulesClient rulesClient = new JcrRulesClient(); 

        RulesWebServicesPublisher app = new RulesWebServicesPublisher();
        app.setRulesClient(rulesClient);
        app.setTempFolder(new File(getTempDirectory()));
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
