package org.openl.rules.ruleservice.loader;

/**
 * Loads deployments from some source.
 * 
 * @author PUdalau
 */
public interface RulesLoader {
    /**
     * Load all deployments from source.
     */
    void loadRules();

    /**
     * Load specified deployment.
     * 
     * @param di Deployment.
     * @throws Exception
     */
    void load(DeploymentInfo di) throws Exception;

    void removeLoadingListener(LoadingListener loadingListener);

    void addLoadingListener(LoadingListener loadingListener);
}
