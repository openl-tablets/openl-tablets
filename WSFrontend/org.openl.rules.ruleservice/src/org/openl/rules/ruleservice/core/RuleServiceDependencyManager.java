package org.openl.rules.ruleservice.core;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.openl.dependency.CompiledDependency;
import org.openl.dependency.ResolvedDependency;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.common.CommonVersion;
import org.openl.rules.project.abstraction.IDeployment;
import org.openl.rules.project.abstraction.IProject;
import org.openl.rules.project.instantiation.AbstractDependencyManager;
import org.openl.rules.project.instantiation.DependencyLoaderInitializationException;
import org.openl.rules.project.instantiation.IDependencyLoader;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.ruleservice.loader.RuleServiceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RuleServiceDependencyManager extends AbstractDependencyManager {

    private final Logger log = LoggerFactory.getLogger(RuleServiceDependencyManager.class);

    private final RuleServiceLoader ruleServiceLoader;
    private final DeploymentDescription deployment;
    private final ThreadLocal<Deque<CompilationInfo>> compilationInfoThreadLocal = ThreadLocal
        .withInitial(ArrayDeque::new);

    private static class CompilationInfo {
        long time;
        long embeddedTime;
    }

    public void compilationBegin() {
        CompilationInfo compilationInfo = new CompilationInfo();
        compilationInfo.time = System.currentTimeMillis();
        Deque<CompilationInfo> compilationInfoStack = compilationInfoThreadLocal.get();
        compilationInfoStack.push(compilationInfo);
    }

    public void compilationCompleted(IDependencyLoader dependencyLoader,
                                     boolean writeToLog) {
        Deque<CompilationInfo> compilationInfoStack = compilationInfoThreadLocal.get();
        try {
            CompilationInfo compilationInfo = compilationInfoStack.pop();
            long t = System.currentTimeMillis() - compilationInfo.time;

            if (log.isInfoEnabled() && !dependencyLoader.isProjectLoader() && writeToLog) {
                log.info("SUCCESS COMPILATION - Module '{}',  project '{}', deployment '{}' in [{}] ms.",
                    dependencyLoader.getModule().getName(),
                    dependencyLoader.getProject().getName(),
                    deployment.getName(),
                    t - compilationInfo.embeddedTime);
            }

            if (!compilationInfoStack.isEmpty()) {
                CompilationInfo compilationInfoParent = compilationInfoStack.peek();
                compilationInfoParent.embeddedTime = compilationInfoParent.embeddedTime + t;
            }
        } catch (Exception e) {
            log.error("Unexpected exception.", e);
        } finally {
            if (compilationInfoStack.isEmpty()) {
                compilationInfoThreadLocal.remove(); // Clean up the thread
            }
        }
    }

    @Override
    public CompiledDependency loadDependency(final ResolvedDependency dependency) throws OpenLCompilationException {
        try {
            return MaxThreadsForCompileSemaphore.getInstance()
                .run(() -> RuleServiceDependencyManager.super.loadDependency(dependency));
        } catch (OpenLCompilationException e) {
            throw e;
        } catch (Exception e) {
            throw new OpenLCompilationException("Failed to compile dependency.", e);
        }
    }

    public RuleServiceDependencyManager(DeploymentDescription deploymentDescription,
            RuleServiceLoader ruleServiceLoader,
            ClassLoader rootClassLoader,
                                        Map<String, Object> externalParameters) {
        super(rootClassLoader, true, externalParameters);
        this.deployment = Objects.requireNonNull(deploymentDescription, "deploymentDescription cannot be null");
        this.ruleServiceLoader = Objects.requireNonNull(ruleServiceLoader, "ruleService cannot be null");
    }

    @Override
    public synchronized void reset(ResolvedDependency dependency) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Set<IDependencyLoader> initDependencyLoaders() {
        Set<IDependencyLoader> dependencyLoaders = new HashSet<>();
        IDeployment rslDeployment = ruleServiceLoader.getDeployment(deployment.getName(), deployment.getVersion());
        String deploymentName = rslDeployment.getDeploymentName();
        CommonVersion deploymentVersion = rslDeployment.getCommonVersion();
        for (IProject aProject : rslDeployment.getProjects()) {
            String projectName = aProject.getName();
            try {
                Collection<Module> modules = ruleServiceLoader
                    .resolveModulesForProject(deploymentName, deploymentVersion, projectName);
                ProjectDescriptor project = null;
                if (!modules.isEmpty()) {
                    project = modules.iterator().next().getProject();

                    for (Module m : modules) {
                        dependencyLoaders.add(new RuleServiceDependencyLoader(project, m, this));
                    }
                }
                if (project != null) {
                    dependencyLoaders.add(new RuleServiceDependencyLoader(project, null, this));
                }
            } catch (Exception e) {
                throw new DependencyLoaderInitializationException(
                    String.format("Failed to initialize dependency loaders for project '%s' in deployment '%s'.",
                        projectName,
                        deploymentName),
                    e);
            }
        }
        return dependencyLoaders;
    }
}
