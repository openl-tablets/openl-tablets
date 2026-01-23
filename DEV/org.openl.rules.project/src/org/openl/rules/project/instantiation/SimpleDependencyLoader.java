package org.openl.rules.project.instantiation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openl.CompiledOpenClass;
import org.openl.dependency.CompiledDependency;
import org.openl.dependency.DependencyType;
import org.openl.dependency.ResolvedDependency;
import org.openl.engine.OpenLCompileManager;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.project.dependencies.ProjectExternalDependenciesHelper;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.source.impl.VirtualSourceCodeModule;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.code.IDependency;
import org.openl.validation.ValidationManager;

public class SimpleDependencyLoader implements IDependencyLoader {

    private final Logger log = LoggerFactory.getLogger(SimpleDependencyLoader.class);

    private final AbstractDependencyManager dependencyManager;
    private final ResolvedDependency dependency;
    private volatile CompiledDependency compiledDependency;
    private final boolean executionMode;
    private final ProjectDescriptor project;
    private final Module module;

    @Override
    public CompiledDependency getRefToCompiledDependency() {
        return compiledDependency;
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

    public SimpleDependencyLoader(ProjectDescriptor project,
                                  Module module,
                                  boolean executionMode,
                                  AbstractDependencyManager dependencyManager) {
        this.project = Objects.requireNonNull(project, "project cannot be null");
        this.module = module;
        this.executionMode = executionMode;
        this.dependencyManager = Objects.requireNonNull(dependencyManager, "dependencyManager cannot be null");
        this.dependency = buildDependency(project, module);
    }

    private static ResolvedDependency buildDependency(ProjectDescriptor project, Module module) {
        if (module != null) {
            return AbstractDependencyManager.buildResolvedDependency(module);
        }
        return AbstractDependencyManager.buildResolvedDependency(project);
    }

    public AbstractDependencyManager getDependencyManager() {
        return dependencyManager;
    }

    @Override
    public final CompiledDependency getCompiledDependency() throws OpenLCompilationException {
        CompiledDependency cachedDependency = compiledDependency;
        if (cachedDependency != null) {
            log.debug("Compiled dependency '{}' is used from cache.", dependency);
            return cachedDependency;
        }
        log.debug("Dependency '{}' is not found in cache.", dependency);
        synchronized (dependencyManager) {
            cachedDependency = compiledDependency;
            if (cachedDependency != null) {
                log.debug("Compiled dependency '{}' is used from cache.", dependency);
                return cachedDependency;
            }
            return compileDependency();
        }
    }

    protected boolean isActualDependency() {
        return true;
    }

    protected CompiledDependency compileDependency() throws OpenLCompilationException {
        var classLoader = dependencyManager.getExternalJarsClassLoader(getProject());

        var parameters = ProjectExternalDependenciesHelper
                .buildExternalParamsWithProjectDependencies(dependencyManager.getExternalParameters(), project);

        IOpenSourceCodeModule source;
        if (isProjectLoader()) {
            source = new VirtualSourceCodeModule();
            var dependencies = new ArrayList<IDependency>();
            getProject().getModules().stream()
                    .map(AbstractDependencyManager::buildResolvedDependency)
                    .distinct()
                    .forEach(dependencies::add);
            if (parameters.get(OpenLCompileManager.EXTERNAL_DEPENDENCIES_KEY) != null) {
                @SuppressWarnings("unchecked")
                var externalDependencies = (Collection<? extends IDependency>) parameters.get(OpenLCompileManager.EXTERNAL_DEPENDENCIES_KEY);
                dependencies.addAll(externalDependencies);
            }
            parameters.put(OpenLCompileManager.EXTERNAL_DEPENDENCIES_KEY, dependencies);
        } else {
            source = new ModulePathSourceCodeModule(module);
            if (module.getProperties() != null) {
                parameters.putAll(module.getProperties());
            }
        }
        source.setParams(parameters);

        boolean oldValidationState = ValidationManager.isValidationEnabled();
        try {
            ValidationManager.turnOffValidation();
            CompiledOpenClass compiledOpenClass = compile(source, classLoader);
            CompiledDependency compiledDependency = new CompiledDependency(dependency,
                    compiledOpenClass,
                    isProjectLoader() ? DependencyType.PROJECT : DependencyType.MODULE);
            if (isActualDependency()) {
                onCompilationComplete(this, compiledDependency);
                this.compiledDependency = compiledDependency;
                log.debug("Dependency '{}' is saved in cache.", dependency);
            }
            return compiledDependency;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            return onCompilationFailure(ex, dependencyManager);
        } finally {
            if (oldValidationState) {
                ValidationManager.turnOnValidation();
            }
        }
    }

    private CompiledOpenClass compile(IOpenSourceCodeModule source, ClassLoader classLoader) {
        var engineFactory = new RulesEngineFactory<>(source);
        engineFactory.setExecutionMode(executionMode);
        engineFactory.setDependencyManager(dependencyManager);
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);
        try {
            return engineFactory.getCompiledOpenClass();
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    protected void onCompilationComplete(IDependencyLoader dependencyLoader, CompiledDependency compiledDependency) {
    }

    protected CompiledDependency onCompilationFailure(Exception ex,
                                                      AbstractDependencyManager dependencyManager) throws OpenLCompilationException {
        throw new OpenLCompilationException(String.format("Failed to load dependency '%s'.", dependency), ex);
    }

    public ResolvedDependency getDependency() {
        return dependency;
    }

    @Override
    public void reset() {
        CompiledDependency compiledDependency1 = compiledDependency;
        if (compiledDependency1 != null) {
            onResetComplete(this, compiledDependency1);
        }
        compiledDependency = null;
    }

    protected void onResetComplete(IDependencyLoader dependencyLoader, CompiledDependency compiledDependency) {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        SimpleDependencyLoader that = (SimpleDependencyLoader) o;
        return dependency.equals(that.dependency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dependency);
    }
}
