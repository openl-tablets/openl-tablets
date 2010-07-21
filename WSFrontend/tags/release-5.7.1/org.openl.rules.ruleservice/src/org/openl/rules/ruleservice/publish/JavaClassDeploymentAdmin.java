package org.openl.rules.ruleservice.publish;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.main.OpenLWrapper;
import org.openl.rules.project.instantiation.ReloadType;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.instantiation.RulesInstantiationStrategyFactory;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;

public class JavaClassDeploymentAdmin implements DeploymentAdmin {
    private static final Log log = LogFactory.getLog(JavaClassDeploymentAdmin.class);

    private Map<String, Map<String, OpenLWrapper>> runningServices = new HashMap<String, Map<String, OpenLWrapper>>();

    private Collection<DeploymentListener> deploymentListeners = new ArrayList<DeploymentListener>();

    public void addDeploymentListener(DeploymentListener deploymentListener) {
        if (deploymentListener != null) {
            deploymentListeners.add(deploymentListener);
        }
    }

    public synchronized void deploy(String deploymentName, List<ProjectDescriptor> infoList) {
        onBeforeDeployment(deploymentName);

        undeploy(deploymentName);

        Map<String, OpenLWrapper> projectWrappers = new HashMap<String, OpenLWrapper>();
        for (ProjectDescriptor wsInfo : infoList) {
            for (Module rulesModule : wsInfo.getModules()) {
                try {
                    OpenLWrapper wrapper = deploy(deploymentName, rulesModule);
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

    private OpenLWrapper deploy(String serviceName, Module rulesModule)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        RulesInstantiationStrategy strategy = RulesInstantiationStrategyFactory.getStrategy(rulesModule);

        return (OpenLWrapper) strategy.instantiate(ReloadType.RELOAD);

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