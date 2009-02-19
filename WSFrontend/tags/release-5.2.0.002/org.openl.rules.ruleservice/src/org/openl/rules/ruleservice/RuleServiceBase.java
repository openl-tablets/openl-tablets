package org.openl.rules.ruleservice;

import java.io.File;

import org.openl.SmartProps;
import org.openl.rules.repository.RDeploymentListener;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.ruleservice.loader.LoadingEventObject;
import org.openl.rules.ruleservice.loader.LoadingListener;
import org.openl.rules.ruleservice.loader.RulesLoader;
import org.openl.rules.ruleservice.publish.DeploymentAdmin;
import org.openl.rules.ruleservice.publish.RulesPublisher;
import org.openl.rules.ruleservice.resolver.RulesProjectResolver;
import org.openl.rules.workspace.production.client.JcrRulesClient;

public class RuleServiceBase {
    /**
     * Gets path to temporary directory. Extract the value with key <i>ruleservice.tmp.dir</i> from configuration file.
     * If such a key is missing returns default value <tt>/tmp/rules-deploy</tt>. 
     *
     * @return path to temporary directory
     */
    protected static String getTempDirectory() {
        SmartProps props = new SmartProps("rules-production.properties");
        String value = props.getStr("ruleservice.tmp.dir");
        if (value == null || value.trim().length() == 0) {
            return "/tmp/rules-deploy";
        }
        
        return value;
    }
    
    protected static RulesLoader loader;
    protected static DeploymentAdmin deployAdmin;
    protected static RulesProjectResolver resolver;
    protected static RulesPublisher publisher;
    
    /**
     * Continuously exposes web service frontend with newest rules projects.
     * Be aware to create a new thread to run this method.
     * 
     * @throws RRepositoryException
     * @throws InterruptedException
     */
    public static void runFrontend() throws RRepositoryException, InterruptedException {
        final JcrRulesClient rulesClient = new JcrRulesClient(); 

        loader.setRulesClient(rulesClient);;
        loader.setTempFolder(new File(getTempDirectory()));
        
        publisher.setDeployAdmin(deployAdmin);
        
        loader.addLoadingListener(new LoadingListener (){

            public void onAfterLoading(LoadingEventObject loadedDeployment) {
                publisher.deploy(loadedDeployment.getDeploymentInfo(), loadedDeployment.getDeploymentLocalFolder());
            }

            public void onBeforeLoading(LoadingEventObject loadingDeployment) {
                publisher.undeploy(loadingDeployment.getDeploymentInfo());
            }
            
        });
        
        publisher.setRulesProjectResolver(resolver);

        final PeriodicalExecutor executor = new PeriodicalExecutor(loader);

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
