package org.openl.rules.ruleservice;

import java.io.File;

import org.openl.config.ConfigPropertyString;
import org.openl.config.ConfigSet;
import org.openl.config.SysConfigManager;
import org.openl.rules.repository.RDeploymentListener;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.ruleservice.publish.RulesWebServicesPublisher;
import org.openl.rules.ruleservice.publish.WebServicesDeployAdmin;
import org.openl.rules.workspace.production.client.JcrRulesClient;

/**
 * Application main class. Sets up other classes.
 */
public class RuleServiceMain {
    /**
     * Gets path to temporary directory. Extract the value with key
     * <i>ruleservice.tmp.dir</i> from configuration file. If such a key is
     * missing returns default value <tt>/tmp/ws-deploy</tt>.
     * 
     * @return path to temporary directory
     */
    private static String getTempDirectory() {
        ConfigPropertyString confTempDirectory = new ConfigPropertyString("ruleservice.tmp.dir", "/tmp/ws-deploy");

        ConfigSet confSet = SysConfigManager.getConfigManager().locate("rules-production.properties");
        if (confSet != null) {
            confSet.updateProperty(confTempDirectory);
        }

        return confTempDirectory.getValue();
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
                } catch (RRepositoryException e) {
                }

                try {
                    rulesClient.release();
                } catch (RRepositoryException e) {
                }
            }
        }));

        executor.execute();
    }
}
