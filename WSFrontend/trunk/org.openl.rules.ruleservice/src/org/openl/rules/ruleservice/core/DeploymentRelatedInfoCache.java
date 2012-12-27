package org.openl.rules.ruleservice.core;

import java.util.HashMap;
import java.util.Map;

public class DeploymentRelatedInfoCache {
    private static ThreadLocal<DeploymentRelatedInfoCache> instanceHolder = new ThreadLocal<DeploymentRelatedInfoCache>();
    private Map<DeploymentDescription, DeploymentRelatedInfo> cache = new HashMap<DeploymentDescription, DeploymentRelatedInfo>();

    public static DeploymentRelatedInfoCache getInstance() {
        DeploymentRelatedInfoCache instance = instanceHolder.get();
        if (instance == null) {
            instance = new DeploymentRelatedInfoCache();
            setInstance(instance);
        }
        return instance;
    }

    public static void setInstance(DeploymentRelatedInfoCache instance) {
        instanceHolder.set(instance);
    }

    public static void removeInstance() {
        instanceHolder.remove();
    }

    public DeploymentRelatedInfo getDeploymentRelatedInfo(DeploymentDescription deploymentDescription) {
        return cache.get(deploymentDescription);
    }

    public void putDeploymentRelatedInfo(DeploymentDescription deploymentDescription,
            DeploymentRelatedInfo deploymentRelatedInfo) {
        cache.put(deploymentDescription, deploymentRelatedInfo);
    }
}
