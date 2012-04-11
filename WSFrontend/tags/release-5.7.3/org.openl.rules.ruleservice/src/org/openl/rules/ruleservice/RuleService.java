package org.openl.rules.ruleservice;

import org.openl.rules.ruleservice.loader.LoadingEventObject;
import org.openl.rules.ruleservice.loader.LoadingListener;
import org.openl.rules.ruleservice.loader.RulesLoader;
import org.openl.rules.ruleservice.publish.RulesPublisher;

public class RuleService implements Runnable {
    protected  RulesLoader loader;

    protected  RulesPublisher publisher;
    /**
     * Continuously exposes web service frontend with newest rules projects. Be
     * aware to create a new thread to run this method.
     *
     * @throws RRepositoryException
     * @throws InterruptedException
     */
    public  void run() {
        loader.addLoadingListener(new LoadingListener() {

            public void onAfterLoading(LoadingEventObject loadedDeployment) {
                publisher.deploy(loadedDeployment.getDeploymentInfo(), loadedDeployment.getDeploymentLocalFolder());
            }

            public void onBeforeLoading(LoadingEventObject loadingDeployment) {
                publisher.undeploy(loadingDeployment.getDeploymentInfo());
            }

        });

        loader.loadRules();
    }

    public RulesLoader getLoader() {
        return loader;
    }

    public void setLoader(RulesLoader loader) {
        this.loader = loader;
    }

    public RulesPublisher getPublisher() {
        return publisher;
    }

    public void setPublisher(RulesPublisher publisher) {
        this.publisher = publisher;
    }
}
