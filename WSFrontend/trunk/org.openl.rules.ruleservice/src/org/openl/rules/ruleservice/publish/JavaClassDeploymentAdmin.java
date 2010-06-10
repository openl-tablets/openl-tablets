package org.openl.rules.ruleservice.publish;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.main.OpenLWrapper;
import org.openl.rules.ruleservice.instantiation.RulesInstantiationStrategy;
import org.openl.rules.ruleservice.instantiation.RulesInstantiationStrategyFactory;
import org.openl.rules.ruleservice.resolver.RulesModuleInfo;
import org.openl.rules.ruleservice.resolver.RulesProjectInfo;

public class JavaClassDeploymentAdmin implements DeploymentAdmin {
    private static final Log log = LogFactory.getLog(JavaClassDeploymentAdmin.class);

    private Map<String, Map<String, OpenLWrapper>> runningServices = new HashMap<String, Map<String, OpenLWrapper>>();

    private Collection<DeploymentListener> deploymentListeners = new ArrayList<DeploymentListener>();

    public void addDeploymentListener(DeploymentListener deploymentListener) {
        if (deploymentListener != null) {
            deploymentListeners.add(deploymentListener);
        }
    }

    public synchronized void deploy(String deploymentName, ClassLoader loader, List<RulesProjectInfo> infoList) {
        onBeforeDeployment(deploymentName);

        undeploy(deploymentName);

        Map<String, OpenLWrapper> projectWrappers = new HashMap<String, OpenLWrapper>();
        for (RulesProjectInfo wsInfo : infoList) {
            for (RulesModuleInfo rulesModule : wsInfo.getRulesModules()) {
                try {
                    OpenLWrapper wrapper = deploy(deploymentName, loader, rulesModule);
                    projectWrappers.put(rulesModule.getName(), wrapper);
                } catch (Exception e) {
                    log.error("failed to create service", e);
                }
            }
        }

        runningServices.put(deploymentName, projectWrappers);
        log.info(String.format("Deployed \"%s\" ", deploymentName));

        onAfterDeployment(deploymentName, projectWrappers);
    }

    private OpenLWrapper deploy(String serviceName, ClassLoader loader, RulesModuleInfo rulesModule)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        RulesInstantiationStrategy strategy = RulesInstantiationStrategyFactory.getStrategy(rulesModule, loader);

        return (OpenLWrapper) strategy.instantiate();

    }

    private void onAfterDeployment(String deploymentName, Map<String, OpenLWrapper> projectWrappers) {
        for (DeploymentListener deploymentListener : deploymentListeners) {
            deploymentListener.afterDeployment(deploymentName, projectWrappers);
        }
    }

    private void onAfterUndeployment(String deploymentName) {
        for (DeploymentListener deploymentListener : deploymentListeners) {
            deploymentListener.afterUndeployment(deploymentName);
        }
    }

    private void onBeforeDeployment(String deploymentName) {
        for (DeploymentListener deploymentListener : deploymentListeners) {
            deploymentListener.beforeDeployment(deploymentName);
        }
    }

    private void onBeforeUndeployment(String deploymentName) {
        for (DeploymentListener deploymentListener : deploymentListeners) {
            deploymentListener.beforeUndeployment(deploymentName);
        }
    }

    public void removeDeploymentListener(DeploymentListener deploymentListener) {
        deploymentListeners.remove(deploymentListener);
    }

    public synchronized void undeploy(String deploymentName) {
        onBeforeUndeployment(deploymentName);

        runningServices.remove(deploymentName);

        onAfterUndeployment(deploymentName);
    }

}