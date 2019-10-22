package org.openl.rules.activiti.spring;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.persistence.deploy.DefaultDeploymentCache;
import org.activiti.engine.impl.persistence.deploy.DeploymentCache;
import org.activiti.engine.impl.persistence.entity.DeploymentEntity;
import org.activiti.engine.impl.persistence.entity.DeploymentEntityManager;
import org.openl.rules.activiti.ResourceCompileException;
import org.openl.rules.activiti.ResourcePrepareException;
import org.openl.rules.activiti.util.ResourceUtils;
import org.openl.rules.project.instantiation.ProjectEngineFactory;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder;
import org.openl.rules.project.model.RulesDeploy;

public final class OpenLRulesHelper {

    private static class OpenLRulesHelperHolder {
        private static final OpenLRulesHelper INSTANCE = new OpenLRulesHelper();
    }

    public static OpenLRulesHelper getInstance() {
        return OpenLRulesHelperHolder.INSTANCE;
    }

    private OpenLRulesHelper() {
    }

    @SuppressWarnings("rawtypes")
    private Map<String, DeploymentCache<ProjectEngineFactory>> cache = new HashMap<>();

    private Map<String, DeploymentCache<Object>> cacheInstance = new HashMap<>();

    public Object getInstance(String deploymentId, String resource) {
        // First find in cache
        DeploymentCache<Object> deploymentCache = cacheInstance.get(deploymentId);
        if (deploymentCache == null) {
            deploymentCache = new DefaultDeploymentCache<>();
            cacheInstance.put(deploymentId, deploymentCache);
        }
        Object instance = deploymentCache.get(resource);
        if (instance == null) {
            @SuppressWarnings("rawtypes")
            ProjectEngineFactory projectEngineFactory = get(deploymentId, resource);
            try {
                instance = projectEngineFactory.newInstance();
            } catch (Exception e) {
                throw new ResourceCompileException(String.format("Resource with name '%s' in deployment with id '%s' compilation has been failed", resource, deploymentId));
            }
            deploymentCache.add(resource, instance);
        }
        return instance;
    }

    public void clear(String deploymentId) {
        cache.remove(deploymentId);
        cacheInstance.remove(deploymentId);
    }

    @SuppressWarnings("rawtypes")
    public void clear(String deploymentId, String resource) {
        DeploymentCache<ProjectEngineFactory> deploymentCache = cache.get(deploymentId);
        if (deploymentCache != null) {
            deploymentCache.remove(resource);
        }

        DeploymentCache<Object> dCache = cacheInstance.get(deploymentId);
        if (dCache != null) {
            dCache.remove(resource);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ProjectEngineFactory get(String deploymentId, String resource) {
        DeploymentEntityManager deploymentEntityManager = Context.getCommandContext().getDeploymentEntityManager();

        DeploymentEntity deployment = deploymentEntityManager.findDeploymentById(deploymentId);
        if (deployment == null) {
            throw new ActivitiObjectNotFoundException("Deployment with id '" + deploymentId + "' is not found.",
                DeploymentEntity.class);
        }

        // First find in cache
        DeploymentCache<ProjectEngineFactory> deploymentCache = cache.computeIfAbsent(deploymentId,
            e -> new DefaultDeploymentCache<>());

        ProjectEngineFactory<Object> projectEngineFactory = deploymentCache.get(resource);
        if (projectEngineFactory != null) {
            return projectEngineFactory;
        } else {
            // Not found in cache. Compile resource and put it to cache.
            try {
                File openlProjectFolder = ResourceUtils.prepareDeploymentOpenLResource(deploymentId, resource);

                RulesDeploy rulesDeploy = ResourceUtils.readRulesDeploy(openlProjectFolder);

                SimpleProjectEngineFactoryBuilder simpleProjectEngineFactoryBuilder = new SimpleProjectEngineFactoryBuilder()
                    .setExecutionMode(true)
                    .setProject(openlProjectFolder.getCanonicalPath())
                    .setWorkspace(openlProjectFolder.getCanonicalPath());
                if (rulesDeploy != null) {
                    simpleProjectEngineFactoryBuilder.setProvideRuntimeContext(rulesDeploy.isProvideRuntimeContext());
                    if (rulesDeploy.getServiceClass() != null) {
                        Class<?> interfaceClass = Thread.currentThread()
                            .getContextClassLoader()
                            .loadClass(rulesDeploy.getServiceClass());
                        simpleProjectEngineFactoryBuilder.setInterfaceClass(interfaceClass);
                    }
                }

                SimpleProjectEngineFactory<Object> simpleProjectEngineFactory = simpleProjectEngineFactoryBuilder
                    .build();

                deploymentCache.add(resource, simpleProjectEngineFactory);

                return simpleProjectEngineFactory;
            } catch (IOException | ClassNotFoundException e) {
                throw new ResourcePrepareException(
                    "Preparing resource with name '" + resource + "' in deployment with id '" + deploymentId + "' has been failed.",
                    e);
            }
        }
    }
}
