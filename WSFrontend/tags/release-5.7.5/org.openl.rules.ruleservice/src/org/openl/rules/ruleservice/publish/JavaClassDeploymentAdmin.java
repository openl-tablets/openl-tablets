package org.openl.rules.ruleservice.publish;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.dependency.IDependencyManager;
import org.openl.dependency.loader.IDependencyLoader;
import org.openl.main.OpenLWrapper;
import org.openl.rules.project.dependencies.RulesModuleDependencyLoader;
import org.openl.rules.project.dependencies.RulesProjectDependencyManager;
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
                    OpenLWrapper wrapper = deploy(deploymentName, rulesModule, infoList);
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

    private OpenLWrapper deploy(String serviceName, Module rulesModule, List<ProjectDescriptor> infoList)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        IDependencyManager dependencyManager = initDependencyManager(infoList);
        RulesInstantiationStrategy strategy = 
            RulesInstantiationStrategyFactory.getStrategy(rulesModule, true, dependencyManager);

        return (OpenLWrapper) strategy.instantiate(ReloadType.SINGLE);

    }

    private IDependencyManager initDependencyManager(List<ProjectDescriptor> infoList) {
        List<Module> modules = new ArrayList<Module>();
        for (ProjectDescriptor wsInfo : infoList) {
            modules.addAll(wsInfo.getModules());
        }
        RulesProjectDependencyManager dependencyManager = new RulesProjectDependencyManager();
        
        dependencyManager.setExecutionMode(true);
        IDependencyLoader loader1 = new RulesModuleDependencyLoader(modules);
        
        dependencyManager.setDependencyLoaders(Arrays.asList(loader1));
        return dependencyManager;
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