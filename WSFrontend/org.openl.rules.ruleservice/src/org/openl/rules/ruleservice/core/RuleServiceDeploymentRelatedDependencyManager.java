package org.openl.rules.ruleservice.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import org.openl.dependency.CompiledDependency;
import org.openl.dependency.loader.IDependencyLoader;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.ProjectException;
import org.openl.rules.project.IRulesDeploySerializer;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.dependencies.ProjectExternalDependenciesHelper;
import org.openl.rules.project.instantiation.AbstractDependencyManager;
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

    private DeploymentDescription deploymentDescription;

    private Collection<ProjectDescriptor> projectDescriptors = null;
    List<IDependencyLoader> dependencyLoaders = null;
    Collection<String> dependencyNames = null;

    private IRulesDeploySerializer rulesDeploySerializer = new XmlRulesDeploySerializer();

    private boolean lazy;

    private PathMatcher wildcardPatternMatcher = new AntPathMatcher();

    public boolean isLazy() {
        return lazy;
    }

    public RuleServiceLoader getRuleServiceLoader() {
        return ruleServiceLoader;
    }

    @Override
    public Collection<String> getAllDependencies() {
        if (dependencyLoaders == null) {
            initDependencyLoaders();
        }
        return dependencyNames;
    }

    private ThreadLocal<Deque<CompilationInfo>> compliationInfoThreadLocal = ThreadLocal.withInitial(ArrayDeque::new);

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
                throw new IllegalStateException("Illegal State!");
            }
            Collection<Module> modules = compilationInfo.modules;

            long t = System.currentTimeMillis() - compilationInfo.time;

            if (modules.size() == 1 && successed && (!(dependencyLoader instanceof LazyRuleServiceDependencyLoader))) {
                Module module = modules.iterator().next();
                if (log.isInfoEnabled()) {
                    log.info(String.format("Module '%s' in project '%s' has been compiled in %s ms.",
                        module.getName(),
                        module.getProject().getName(),
                        String.valueOf((t - compilationInfo.embeddedTime))));
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
            throw new OpenLCompilationException("Something wrong!", e);
        }
    }

    public RuleServiceDeploymentRelatedDependencyManager(DeploymentDescription deploymentDescription,
            RuleServiceLoader ruleServiceLoader,
            ClassLoader rootClassLoader,
            boolean lazy) {
        super(rootClassLoader);
        this.deploymentDescription = Objects.requireNonNull(deploymentDescription, "deploymentDescription cannot be null");
        this.ruleServiceLoader = Objects.requireNonNull(ruleServiceLoader, "ruleService cannot be null");
        this.lazy = lazy;
        super.setExecutionMode(true);
    }

    @Override
    public void setExecutionMode(boolean executionMode) {
        throw new UnsupportedOperationException("This dependency manager does not support executionMode=false!");
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
    public Collection<ProjectDescriptor> getProjectDescriptors() {
        if (dependencyLoaders == null) {
            initDependencyLoaders();
        }
        return projectDescriptors;
    }

    @Override
    public List<IDependencyLoader> getDependencyLoaders() {
        if (dependencyLoaders == null) {
            initDependencyLoaders();
        }
        return dependencyLoaders;
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

    private synchronized void initDependencyLoaders() {
        if (projectDescriptors == null && dependencyLoaders == null) {
            dependencyLoaders = new ArrayList<>();
            projectDescriptors = new ArrayList<>();
            dependencyNames = new HashSet<>();
            Collection<Deployment> deployments = ruleServiceLoader.getDeployments();
            for (Deployment deployment : deployments) {
                String deploymentName = deployment.getDeploymentName();
                CommonVersion deploymentVersion = deployment.getCommonVersion();
                if (deploymentDescription.getName().equals(deploymentName) && deploymentDescription.getVersion()
                    .equals(deploymentVersion)) {
                    for (AProject project : deployment.getProjects()) {
                        String projectName = project.getName();
                        try {
                            Collection<Module> modulesOfProject = ruleServiceLoader
                                .resolveModulesForProject(deploymentName, deploymentVersion, projectName);
                            ProjectDescriptor projectDescriptor = null;
                            Set<String> wildcardPatterns = new HashSet<>();
                            if (!modulesOfProject.isEmpty()) {
                                Module firstModule = modulesOfProject.iterator().next();
                                projectDescriptor = firstModule.getProject();

                                InputStream content = null;
                                RulesDeploy rulesDeploy = null;
                                try {
                                    AProjectArtefact artifact = project
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
                                } finally {
                                    if (content != null) {
                                        try {
                                            content.close();
                                        } catch (IOException e) {
                                            log.error(e.getMessage(), e);
                                        }
                                    }
                                }

                                for (Module m : modulesOfProject) {
                                    IDependencyLoader moduleLoader;
                                    String moduleName = m.getName();
                                    List<Module> module = Arrays.asList(m);
                                    if (isLazy()) {
                                        boolean compileAfterLazyCompilation = compilationAfterLazyCompilationRequred(
                                            wildcardPatterns,
                                            moduleName);
                                        moduleLoader = new LazyRuleServiceDependencyLoader(deploymentDescription,
                                            moduleName,
                                            module,
                                            compileAfterLazyCompilation,
                                            false);
                                    } else {
                                        moduleLoader = new RuleServiceDependencyLoader(moduleName, module, false);
                                    }
                                    dependencyLoaders.add(moduleLoader);
                                    dependencyNames.add(moduleName);
                                }
                            }
                            if (projectDescriptor != null) {
                                IDependencyLoader projectLoader;
                                if (isLazy()) {
                                    projectLoader = new LazyRuleServiceDependencyLoader(deploymentDescription,
                                        ProjectExternalDependenciesHelper
                                            .buildDependencyNameForProjectName(projectDescriptor.getName()),
                                        projectDescriptor.getModules(),
                                        false,
                                        true);
                                } else {
                                    projectLoader = new RuleServiceDependencyLoader(
                                        ProjectExternalDependenciesHelper
                                            .buildDependencyNameForProjectName(projectDescriptor.getName()),
                                        projectDescriptor.getModules(),
                                        true);
                                }
                                projectDescriptors.add(projectDescriptor);
                                dependencyLoaders.add(projectLoader);
                            }
                        } catch (Exception e) {
                            log.error("Failed to build dependency manager loaders for project '{}' in deployment '{}'!",
                                projectName,
                                deploymentName,
                                e);
                        }
                    }
                }
            }
        }
    }
}
