package org.openl.rules.ruleservice.publish.lazy;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import org.openl.CompiledOpenClass;
import org.openl.dependency.CompiledDependency;
import org.openl.dependency.ResolvedDependency;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.lang.xls.prebind.IPrebindHandler;
import org.openl.rules.project.dependencies.ProjectExternalDependenciesHelper;
import org.openl.rules.project.instantiation.AbstractDependencyManager;
import org.openl.rules.project.instantiation.ApiBasedInstantiationStrategy;
import org.openl.rules.project.instantiation.IDependencyLoader;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.instantiation.SimpleDependencyLoader;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.ruleservice.core.DeploymentDescription;
import org.openl.rules.ruleservice.core.RuleServiceDependencyManager;
import org.openl.rules.ruleservice.core.RuleServiceDependencyManager.DependencyCompilationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LazyRuleServiceDependencyLoader implements IDependencyLoader {

    private final Logger log = LoggerFactory.getLogger(LazyRuleServiceDependencyLoader.class);

    private final RuleServiceDependencyManager dependencyManager;
    private final ResolvedDependency dependency;
    private final boolean realCompileRequired;
    private CompiledOpenClass lazyCompiledOpenClass;
    private final DeploymentDescription deployment;
    private final ProjectDescriptor project;
    private final Module module;

    public LazyRuleServiceDependencyLoader(DeploymentDescription deployment,
            ProjectDescriptor project,
            Module module,
            boolean realCompileRequired,
            RuleServiceDependencyManager dependencyManager) {
        this.deployment = Objects.requireNonNull(deployment, "deployment cannot null.");
        this.project = Objects.requireNonNull(project, "project cannot be null");
        this.module = module;
        this.realCompileRequired = realCompileRequired;
        this.dependencyManager = Objects.requireNonNull(dependencyManager, "dependencyManager cannot be null");
        this.dependency = SimpleDependencyLoader.buildDependency(project, module);
    }

    @Override
    public CompiledDependency getRefToCompiledDependency() {
        return lazyCompiledDependency;
    }

    private Collection<Module> getModules() {
        return module != null ? Collections.singleton(module) : project.getModules();
    }

    @Override
    public boolean isProjectLoader() {
        return module == null;
    }

    @Override
    public ProjectDescriptor getProject() {
        return project;
    }

    @Override
    public Module getModule() {
        return module;
    }

    @Override
    public ResolvedDependency getDependency() {
        return dependency;
    }

    private ClassLoader buildClassLoader(AbstractDependencyManager dependencyManager) {
        return dependencyManager.getExternalJarsClassLoader(getProject());
    }

    public CompiledOpenClass compile(final ResolvedDependency dependency,
            final RuleServiceDependencyManager dependencyManager) throws OpenLCompilationException {
        if (lazyCompiledOpenClass != null) {
            return lazyCompiledOpenClass;
        }

        log.debug("Compiling lazy dependency: deployment='{}', version='{}', name='{}'.",
            deployment.getName(),
            deployment.getVersion().getVersionName(),
            dependency);

        final ClassLoader classLoader = buildClassLoader(dependencyManager);
        RulesInstantiationStrategy rulesInstantiationStrategy;
        Collection<Module> modules = getModules();
        if (isProjectLoader()) {
            if (modules.isEmpty()) {
                throw new IllegalStateException("Expected at least one module in the project.");
            }
            rulesInstantiationStrategy = new LazyInstantiationStrategy(deployment,
                modules,
                dependencyManager,
                classLoader);
        } else {
            rulesInstantiationStrategy = new ApiBasedInstantiationStrategy(module,
                dependencyManager,
                classLoader,
                true);
        }
        rulesInstantiationStrategy.setServiceClass(LazyRuleServiceDependencyLoaderInterface.class);// Prevent
        // generation interface and Virtual module duplicate (instantiate method). Improve performance.
        final Map<String, Object> parameters = ProjectExternalDependenciesHelper
            .buildExternalParamsWithProjectDependencies(dependencyManager.getExternalParameters(), modules);
        rulesInstantiationStrategy.setExternalParameters(parameters);
        IPrebindHandler prebindHandler = LazyBinderMethodHandler.getPrebindHandler();
        try {
            LazyBinderMethodHandler
                .setPrebindHandler(new LazyPrebindHandler(modules, dependencyManager, classLoader, deployment));
            try {
                dependencyManager.compilationBegin();
                lazyCompiledOpenClass = rulesInstantiationStrategy.compile();
                if (!isProjectLoader() && realCompileRequired) {
                    synchronized (lazyCompiledOpenClass) {
                        CompiledOpenClass compiledOpenClass = CompiledOpenClassCache.getInstance()
                            .get(deployment, dependency);
                        if (compiledOpenClass == null) {
                            CompiledOpenClassCache.compileToCache(dependencyManager,
                                dependency,
                                deployment,
                                modules.iterator().next(),
                                classLoader);
                        }
                    }
                }
                dependencyManager.compilationCompleted(this,
                    realCompileRequired ? DependencyCompilationType.UNLOADABLE : DependencyCompilationType.LAZY,
                    !lazyCompiledOpenClass.hasErrors());
            } finally {
                if (lazyCompiledOpenClass == null) {
                    dependencyManager.compilationCompleted(this,
                        realCompileRequired ? DependencyCompilationType.UNLOADABLE : DependencyCompilationType.LAZY,
                        false);
                }
            }
            return lazyCompiledOpenClass;
        } catch (Exception ex) {
            throw new OpenLCompilationException(String.format("Failed to load dependency '%s'.", dependency), ex);
        } finally {
            LazyBinderMethodHandler.setPrebindHandler(prebindHandler);
        }
    }

    private boolean isCompiledBefore = false;
    private CompiledDependency lazyCompiledDependency = null;

    @Override
    public final CompiledDependency getCompiledDependency() throws OpenLCompilationException {
        if (!isCompiledBefore) {
            compile(dependency, dependencyManager);
            isCompiledBefore = true;
        }
        if (lazyCompiledDependency == null) {
            CompiledOpenClass compiledOpenClass = new LazyCompiledOpenClass(dependencyManager, this, dependency);
            lazyCompiledDependency = new CompiledDependency(dependency, compiledOpenClass);
        }
        return lazyCompiledDependency;
    }

    @Override
    public void reset() {
        CompiledOpenClassCache.getInstance().removeAll(deployment);
    }

    interface LazyRuleServiceDependencyLoaderInterface {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        LazyRuleServiceDependencyLoader that = (LazyRuleServiceDependencyLoader) o;
        return dependency.equals(that.dependency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dependency);
    }
}