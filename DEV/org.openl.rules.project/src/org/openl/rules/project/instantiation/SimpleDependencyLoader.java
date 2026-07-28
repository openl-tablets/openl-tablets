package org.openl.rules.project.instantiation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

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

@Slf4j
public class SimpleDependencyLoader implements IDependencyLoader {


    @Getter
    private final AbstractDependencyManager dependencyManager;
    @Getter
    private final ResolvedDependency dependency;
    private volatile CompiledDependency compiledDependency;
    private final boolean executionMode;
    @Getter
    private final ProjectDescriptor project;
    @Getter
    private final Module module;

    @Override
    public CompiledDependency getRefToCompiledDependency() {
        return compiledDependency;
    }

    @Override
    public boolean isProjectLoader() {
        return module == null;
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

    @Override
    public final CompiledDependency getCompiledDependency() throws OpenLCompilationException {
        var cachedDependency = compiledDependency;
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

        var oldValidationState = ValidationManager.isValidationEnabled();
        try {
            ValidationManager.turnOffValidation();
            var compiledOpenClass = compile(source, classLoader);
            var compiledDependency = new CompiledDependency(dependency,
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
        var oldClassLoader = Thread.currentThread().getContextClassLoader();
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
        throw new OpenLCompilationException("Failed to load dependency '%s'.".formatted(dependency), ex);
    }

    @Override
    public void reset() {
        var compiledDependency1 = compiledDependency;
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
        if (!(o instanceof SimpleDependencyLoader that))
            return false;
        return dependency.equals(that.dependency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dependency);
    }
}
