package org.openl.rules.ruleservice.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

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
import org.openl.rules.ruleservice.core.MaxThreadsForCompileSemaphore.Callable;
import org.openl.rules.ruleservice.loader.RuleServiceLoader;
import org.openl.syntax.code.IDependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

public class RuleServiceDeploymentRelatedDependencyManager extends AbstractDependencyManager implements CompilationTimeLoggingDependencyManager {

    private final Logger log = LoggerFactory.getLogger(RuleServiceDeploymentRelatedDependencyManager.class);

    private RuleServiceLoader ruleServiceLoader;
    private DeploymentDescription deployment;
    private IRulesDeploySerializer rulesDeploySerializer = new XmlRulesDeploySerializer();
    private boolean lazyCompilation;
    private PathMatcher wildcardPatternMatcher = new AntPathMatcher();
    private ThreadLocal<Deque<CompilationInfo>> compliationInfoThreadLocal = ThreadLocal.withInitial(ArrayDeque::new);

    public boolean isLazyCompilation() {
        return lazyCompilation;
    }

    public RuleServiceLoader getRuleServiceLoader() {
        return ruleServiceLoader;
    }

    private static class CompilationInfo {
        long time;
        long embeddedTime;
        IDependencyLoader dependencyLoader;
        Collection<Module> modules;
    }

    @Override
    public void compilationBegin(IDependencyLoader dependencyLoader, Collection<Module> modules) {
        CompilationInfo compilationInfo = new CompilationInfo();
        compilationInfo.time = System.currentTimeMillis();
        compilationInfo.dependencyLoader = dependencyLoader;
        compilationInfo.modules = Collections.unmodifiableCollection(modules);
        Deque<CompilationInfo> compilationInfoStack = compliationInfoThreadLocal.get();
        compilationInfoStack.push(compilationInfo);
    }

    @Override
    public void compilationCompleted(IDependencyLoader dependencyLoader, boolean successed) {
        Deque<CompilationInfo> compilationInfoStack = compliationInfoThreadLocal.get();
        try {
            CompilationInfo compilationInfo = compilationInfoStack.pop();
            if (compilationInfo.dependencyLoader != dependencyLoader) {
                throw new IllegalStateException("This should not happen.");
            }
            Collection<Module> modules = compilationInfo.modules;

            long t = System.currentTimeMillis() - compilationInfo.time;

            if (modules.size() == 1 && successed && !(dependencyLoader instanceof LazyRuleServiceDependencyLoader)) {
                Module module = modules.iterator().next();
                if (log.isInfoEnabled()) {
                    log.info(String.format("Module '%s' in project '%s' has been compiled in %s ms.",
                        module.getName(),
                        module.getProject().getName(),
                        String.valueOf(t - compilationInfo.embeddedTime)));
                }
            }

            if (!compilationInfoStack.isEmpty()) {
                CompilationInfo compilationInfoParent = compilationInfoStack.peek();
                compilationInfoParent.embeddedTime = compilationInfoParent.embeddedTime + t;
            }
        } catch (Exception e) {
            log.error("Unexpected exception.", e);
        } finally {
            if (compilationInfoStack.isEmpty()) {
                compliationInfoThreadLocal.remove(); // Clean a thread
            }
        }
    }

    @Override
    public CompiledDependency loadDependency(final IDependency dependency) throws OpenLCompilationException {
        try {
            return MaxThreadsForCompileSemaphore.getInstance().run(new Callable<CompiledDependency>() {
                @Override
                public CompiledDependency call() throws Exception {
                    return RuleServiceDeploymentRelatedDependencyManager.super.loadDependency(dependency);
                }
            });
        } catch (OpenLCompilationException e) {
            throw e;
        } catch (Exception e) {
            throw new OpenLCompilationException("Failed to compile.", e);
        }
    }

    public RuleServiceDeploymentRelatedDependencyManager(DeploymentDescription deploymentDescription,
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
    protected Map<String, IDependencyLoader> initDependencyLoaders() {
        Map<String, IDependencyLoader> dependencyLoaders = new HashMap<>();
        Collection<Deployment> deployments = ruleServiceLoader.getDeployments();
        for (Deployment rslDeployment : deployments) {
            String deploymentName = rslDeployment.getDeploymentName();
            CommonVersion deploymentVersion = rslDeployment.getCommonVersion();
            if (deployment.getName().equals(deploymentName) && deployment.getVersion().equals(deploymentVersion)) {
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
                            RulesDeploy rulesDeploy = null;
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
                                //Occurs if rules-deploy.xml file is not present in the project.
                            } finally {
                                closeRuleDeployContent(content);
                            }

                            for (Module m : modules) {
                                IDependencyLoader moduleLoader;
                                if (isLazyCompilation()) {
                                    boolean compileAfterLazyCompilation = compilationAfterLazyCompilationRequred(
                                        wildcardPatterns,
                                        m.getName());
                                    moduleLoader = LazyRuleServiceDependencyLoader
                                        .forModule(deployment, m, compileAfterLazyCompilation, this);
                                } else {
                                    moduleLoader = RuleServiceDependencyLoader.forModule(m, this);
                                }
                                dependencyLoaders.put(moduleLoader.getDependencyName(), moduleLoader);
                            }
                        }
                        if (project != null) {
                            IDependencyLoader projectLoader;
                            if (isLazyCompilation()) {
                                projectLoader = LazyRuleServiceDependencyLoader.forProject(deployment, project, this);
                            } else {
                                projectLoader = RuleServiceDependencyLoader.forProject(project, this);
                            }
                            dependencyLoaders.put(projectLoader.getDependencyName(), projectLoader);
                        }
                    } catch (Exception e) {
                        throw new DependencyLoaderInitializationException(String.format(
                            "Failed to initialize dependency loaders for project '%s' in deployment '%s'.",
                            projectName,
                            deploymentName), e);
                    }
                }
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
