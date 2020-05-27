package org.openl.rules.ruleservice.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.openl.dependency.CompiledDependency;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.ProjectException;
import org.openl.rules.project.IRulesDeploySerializer;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.instantiation.AbstractDependencyManager;
import org.openl.rules.project.instantiation.DependencyLoaderInitializationException;
import org.openl.rules.project.instantiation.IDependencyLoader;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.project.xml.XmlRulesDeploySerializer;
import org.openl.rules.ruleservice.conf.LastVersionProjectsServiceConfigurer;
import org.openl.rules.ruleservice.loader.RuleServiceLoader;
import org.openl.rules.ruleservice.publish.lazy.LazyRuleServiceDependencyLoader;
import org.openl.syntax.code.IDependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

public class RuleServiceDependencyManager extends AbstractDependencyManager {

    private final Logger log = LoggerFactory.getLogger(RuleServiceDependencyManager.class);

    private final RuleServiceLoader ruleServiceLoader;
    private final DeploymentDescription deployment;
    private final IRulesDeploySerializer rulesDeploySerializer = new XmlRulesDeploySerializer();
    private final boolean lazyCompilation;
    private final PathMatcher wildcardPatternMatcher = new AntPathMatcher();
    private final ThreadLocal<Deque<CompilationInfo>> compilationInfoThreadLocal = ThreadLocal
        .withInitial(ArrayDeque::new);

    public boolean isLazyCompilation() {
        return lazyCompilation;
    }

    public RuleServiceLoader getRuleServiceLoader() {
        return ruleServiceLoader;
    }

    private static class CompilationInfo {
        long time;
        long embeddedTime;
    }

    public void compilationBegin(IDependencyLoader dependencyLoader) {
        CompilationInfo compilationInfo = new CompilationInfo();
        compilationInfo.time = System.currentTimeMillis();
        Deque<CompilationInfo> compilationInfoStack = compilationInfoThreadLocal.get();
        compilationInfoStack.push(compilationInfo);
    }

    public enum DependencyCompilationType {
        NONLAZY,
        LAZY,
        UNLOADABLE;
    }

    public void compilationCompleted(IDependencyLoader dependencyLoader,
            DependencyCompilationType compilationType,
            boolean writeToLog) {
        Deque<CompilationInfo> compilationInfoStack = compilationInfoThreadLocal.get();
        try {
            CompilationInfo compilationInfo = compilationInfoStack.pop();
            long t = System.currentTimeMillis() - compilationInfo.time;

            if (log.isInfoEnabled() && !dependencyLoader.isProject() && writeToLog) {
                log.info("SUCCESS COMPILATION - {} - Module '{}',  project '{}', deployment '{}' in [{}] ms.",
                    compilationType,
                    dependencyLoader.getDependencyName(),
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
    public CompiledDependency loadDependency(final IDependency dependency) throws OpenLCompilationException {
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
            boolean lazyCompilation,
            Map<String, Object> externalParameters) {
        super(rootClassLoader, true, externalParameters);
        this.deployment = Objects.requireNonNull(deploymentDescription, "deploymentDescription cannot be null");
        this.ruleServiceLoader = Objects.requireNonNull(ruleServiceLoader, "ruleService cannot be null");
        this.lazyCompilation = lazyCompilation;
    }

    @Override
    public synchronized void reset(IDependency dependency) {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized void resetAll() {
        throw new UnsupportedOperationException();
    }

    private boolean compilationAfterLazyCompilationRequred(Set<String> wildcardPatterns, String moduleName) {
        for (String pattern : wildcardPatterns) {
            if (wildcardPatternMatcher.match(pattern, moduleName)) {
                return true;
            }
        }
        return false;
    }

    public final IRulesDeploySerializer getRulesDeploySerializer() {
        return rulesDeploySerializer;
    }

    @Override
    protected Map<String, Collection<IDependencyLoader>> initDependencyLoaders() {
        Map<String, Collection<IDependencyLoader>> dependencyLoaders = new HashMap<>();
        Deployment rslDeployment = ruleServiceLoader.getDeployment(deployment.getName(), deployment.getVersion());
        String deploymentName = rslDeployment.getDeploymentName();
        CommonVersion deploymentVersion = rslDeployment.getCommonVersion();
        for (AProject aProject : rslDeployment.getProjects()) {
            String projectName = aProject.getName();
            try {
                Collection<Module> modules = ruleServiceLoader
                    .resolveModulesForProject(deploymentName, deploymentVersion, projectName);
                ProjectDescriptor project = null;
                Set<String> wildcardPatterns = new HashSet<>();
                if (!modules.isEmpty()) {
                    project = modules.iterator().next().getProject();

                    InputStream content = null;
                    RulesDeploy rulesDeploy;
                    try {
                        AProjectArtefact artifact = aProject
                            .getArtefact(LastVersionProjectsServiceConfigurer.RULES_DEPLOY_XML);
                        if (artifact instanceof AProjectResource) {
                            AProjectResource resource = (AProjectResource) artifact;
                            content = resource.getContent();
                            rulesDeploy = getRulesDeploySerializer().deserialize(content);
                            RulesDeploy.WildcardPattern[] compilationPatterns = rulesDeploy
                                .getLazyModulesForCompilationPatterns();
                            if (compilationPatterns != null) {
                                for (RulesDeploy.WildcardPattern wp : compilationPatterns) {
                                    wildcardPatterns.add(wp.getValue());
                                }
                            }
                        }
                    } catch (ProjectException e) {
                        // Occurs if rules-deploy.xml file is not present in the project.
                    } finally {
                        closeRuleDeployContent(content);
                    }

                    for (Module m : modules) {
                        IDependencyLoader moduleLoader;
                        if (isLazyCompilation()) {
                            boolean compileAfterLazyCompilation = compilationAfterLazyCompilationRequred(
                                wildcardPatterns,
                                m.getName());
                            moduleLoader = new LazyRuleServiceDependencyLoader(deployment,
                                project,
                                m,
                                compileAfterLazyCompilation,
                                this);
                        } else {
                            moduleLoader = new RuleServiceDependencyLoader(project, m, this);
                        }
                        Collection<IDependencyLoader> dependencyLoadersByName = dependencyLoaders
                            .computeIfAbsent(moduleLoader.getDependencyName(), e -> new ArrayList<>());
                        dependencyLoadersByName.add(moduleLoader);
                    }
                }
                if (project != null) {
                    IDependencyLoader projectLoader;
                    if (isLazyCompilation()) {
                        projectLoader = new LazyRuleServiceDependencyLoader(deployment, project, null, false, this);
                    } else {
                        projectLoader = new RuleServiceDependencyLoader(project, null, this);
                    }
                    Collection<IDependencyLoader> dependencyLoadersByName = dependencyLoaders
                        .computeIfAbsent(projectLoader.getDependencyName(), e -> new ArrayList<>());
                    dependencyLoadersByName.add(projectLoader);
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

    private void closeRuleDeployContent(InputStream content) {
        if (content != null) {
            try {
                content.close();
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }
}
