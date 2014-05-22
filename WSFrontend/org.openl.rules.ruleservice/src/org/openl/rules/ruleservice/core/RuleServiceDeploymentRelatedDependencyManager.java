package org.openl.rules.ruleservice.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Semaphore;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.LogFactoryImpl;
import org.openl.dependency.CompiledDependency;
import org.openl.dependency.loader.IDependencyLoader;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.dependencies.ProjectExternalDependenciesHelper;
import org.openl.rules.project.instantiation.AbstractProjectDependencyManager;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.ruleservice.loader.RuleServiceLoader;
import org.openl.syntax.code.IDependency;

public class RuleServiceDeploymentRelatedDependencyManager extends AbstractProjectDependencyManager {

    private final Log log = LogFactoryImpl.getLog(RuleServiceDeploymentRelatedDependencyManager.class);

    private RuleServiceLoader ruleServiceLoader;

    private DeploymentDescription deploymentDescription;

    private boolean lazy;

    public boolean isLazy() {
        return lazy;
    }

    public RuleServiceLoader getRuleServiceLoader() {
        return ruleServiceLoader;
    }

    private static class SemaphoreHolder {
        private static Semaphore limitCompilationThreadsSemaphore = new Semaphore(RuleServiceStaticConfigurationUtil.getMaxThreadsForCompile());
    }

    @Override
    public synchronized CompiledDependency loadDependency(IDependency dependency) throws OpenLCompilationException {
        try {
            SemaphoreHolder.limitCompilationThreadsSemaphore.acquire();
            return super.loadDependency(dependency);
        } catch (InterruptedException e) {
            throw new OpenLCompilationException("Interrupter exception!", e);
        } finally {
            SemaphoreHolder.limitCompilationThreadsSemaphore.release();
        }
    }

    public RuleServiceDeploymentRelatedDependencyManager(DeploymentDescription deploymentDescription,
            RuleServiceLoader ruleServiceLoader) {
        this(deploymentDescription, ruleServiceLoader, false);
    }

    public RuleServiceDeploymentRelatedDependencyManager(DeploymentDescription deploymentDescription,
            RuleServiceLoader ruleServiceLoader,
            boolean lazy) {
        if (deploymentDescription == null) {
            throw new IllegalArgumentException("deploymentDescription can't be null!");
        }
        if (ruleServiceLoader == null) {
            throw new IllegalArgumentException("ruleService can't be null!");
        }
        this.deploymentDescription = deploymentDescription;
        this.ruleServiceLoader = ruleServiceLoader;
        this.lazy = lazy;
        super.setExecutionMode(true);
    }

    @Override
    public void setExecutionMode(boolean executionMode) {
        throw new UnsupportedOperationException("This dependency manager supports only executionMode=true");
    }

    @Override
    public void reset(IDependency dependency) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void resetAll() {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized List<IDependencyLoader> getDependencyLoaders() {
        if (dependencyLoaders != null) {
            return dependencyLoaders;
        }
        dependencyLoaders = new ArrayList<IDependencyLoader>();
        Collection<Deployment> deployments = ruleServiceLoader.getDeployments();
        for (Deployment deployment : deployments) {
            String deploymentName = deployment.getDeploymentName();
            if (deploymentDescription.getName().equals(deploymentName) && deploymentDescription.getVersion()
                .equals(deployment.getCommonVersion())) {
                for (AProject project : deployment.getProjects()) {
                    try {
                        Collection<Module> modulesOfProject = ruleServiceLoader.resolveModulesForProject(deployment.getDeploymentName(),
                            deployment.getCommonVersion(),
                            project.getName());
                        ProjectDescriptor projectDescriptor = null;
                        if (!modulesOfProject.isEmpty()) {
                            Module module = modulesOfProject.iterator().next();
                            projectDescriptor = module.getProject();
                            for (final Module m : modulesOfProject) {
                                IDependencyLoader moduleLoader;
                                if (isLazy()) {
                                    moduleLoader = new LazyRuleServiceDependencyLoader(deploymentDescription,
                                        m.getName(),
                                        new ArrayList<Module>() {
                                            private static final long serialVersionUID = 9044645178042342374L;
                                            {
                                                add(m);
                                            }
                                        });
                                } else {
                                    moduleLoader = new RuleServiceDependencyLoader(m.getName(),
                                        new ArrayList<Module>() {
                                            private static final long serialVersionUID = 9044645178042342374L;
                                            {
                                                add(m);
                                            }
                                        });
                                }
                                dependencyLoaders.add(moduleLoader);
                            }
                        }
                        if (projectDescriptor != null) {
                            IDependencyLoader projectLoader;
                            if (isLazy()) {
                                projectLoader = new LazyRuleServiceDependencyLoader(deploymentDescription,
                                    ProjectExternalDependenciesHelper.buildDependencyNameForProjectName(project.getName()),
                                    projectDescriptor.getModules());
                            } else {
                                projectLoader = new RuleServiceDependencyLoader(ProjectExternalDependenciesHelper.buildDependencyNameForProjectName(project.getName()),
                                    projectDescriptor.getModules());
                            }
                            dependencyLoaders.add(projectLoader);
                        }
                    } catch (Exception e) {
                        if (log.isErrorEnabled()) {
                            log.error("Build dependency manager loaders for project \"" + project.getName() + "\" from deployment \"" + deploymentName + "\" was failed!",
                                e);
                        }
                    }
                }
            }
        }
        return dependencyLoaders;
    }
}
