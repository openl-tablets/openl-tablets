package org.openl.rules.ruleservice.core;

import java.util.Collection;

import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.model.Module;

public class DeploymentRelatedInfo {
    private static ThreadLocal<DeploymentRelatedInfo> currentHolder = new ThreadLocal<DeploymentRelatedInfo>();

    public static DeploymentRelatedInfo getCurrent() {
        return currentHolder.get();
    }

    public static void setCurrent(DeploymentRelatedInfo modules) {
        currentHolder.set(modules);
    }

    public static void removeCurrent() {
        currentHolder.remove();
    }

    private final DeploymentDescription deploymentDescription;
    private final IDependencyManager dependencyManager;
    private final Collection<Module> modulesInDeployment;

    public DeploymentRelatedInfo(DeploymentDescription deploymentDescription, IDependencyManager dependencyManager,
            Collection<Module> modulesInDeployment) {
        this.deploymentDescription = deploymentDescription;
        this.dependencyManager = dependencyManager;
        this.modulesInDeployment = modulesInDeployment;
    }

    public DeploymentDescription getDeploymentDescription() {
        return deploymentDescription;
    }

    public IDependencyManager getDependencyManager() {
        return dependencyManager;
    }

    public Collection<Module> getModulesInDeployment() {
        return modulesInDeployment;
    }

}
