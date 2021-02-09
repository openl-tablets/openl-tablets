package org.openl.rules.project.instantiation;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.openl.CompiledOpenClass;
import org.openl.OpenClassUtil;
import org.openl.classloader.OpenLClassLoader;
import org.openl.dependency.CompiledDependency;
import org.openl.dependency.IDependencyManager;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.project.dependencies.ProjectExternalDependenciesHelper;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.validation.ValidationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleDependencyLoader implements IDependencyLoader {

    private final Logger log = LoggerFactory.getLogger(SimpleDependencyLoader.class);

    private final AbstractDependencyManager dependencyManager;
    private final String dependencyName;
    private CompiledDependency compiledDependency;
    private final boolean executionMode;
    private final boolean singleModuleMode;
    private final ProjectDescriptor project;
    private final Module module;

    protected Map<String, Object> configureExternalParameters(IDependencyManager dependencyManager) {
        Map<String, Object> params;
        if (!singleModuleMode) {
            params = ProjectExternalDependenciesHelper
                .buildExternalParamsWithProjectDependencies(dependencyManager.getExternalParameters(), getModules());
        } else {
            params = new HashMap<>(dependencyManager.getExternalParameters());
        }
        return params;
    }

    private Collection<Module> getModules() {
        return module != null ? Collections.singleton(module) : project.getModules();
    }

    @Override
    public boolean isCompiled() {
        return compiledDependency != null;
    }

    @Override
    public boolean isProject() {
        return module == null;
    }

    @Override
    public ProjectDescriptor getProject() {
        return project;
    }

    public SimpleDependencyLoader(ProjectDescriptor project,
            Module module,
            boolean singleModuleMode,
            boolean executionMode,
            AbstractDependencyManager dependencyManager) {
        this.project = Objects.requireNonNull(project, "project cannot be null");
        this.module = module;
        this.executionMode = executionMode;
        this.singleModuleMode = singleModuleMode;
        this.dependencyManager = Objects.requireNonNull(dependencyManager, "dependencyManager cannot be null");
        this.dependencyName = buildDependencyName(project, module);
    }

    public static String buildDependencyName(ProjectDescriptor project, Module module) {
        if (module != null) {
            return module.getName();
        }
        return ProjectExternalDependenciesHelper.buildDependencyNameForProject(project.getName());
    }

    @Override
    public final CompiledDependency getCompiledDependency() throws OpenLCompilationException {
        if (compiledDependency != null) {
            log.debug("Compiled dependency '{}' is used from cache.", dependencyName);
            return compiledDependency;
        }
        log.debug("Dependency '{}' is not found in cache.", dependencyName);
        return compileDependency(dependencyName, dependencyManager);
    }

    protected ClassLoader buildClassLoader(AbstractDependencyManager dependencyManager) {
        ClassLoader projectClassLoader = dependencyManager.getExternalJarsClassLoader(getProject());
        OpenLClassLoader openLClassLoader = new OpenLClassLoader(null);
        openLClassLoader.addClassLoader(projectClassLoader);
        return openLClassLoader;
    }

    protected CompiledDependency compileDependency(String dependencyName,
            AbstractDependencyManager dependencyManager) throws OpenLCompilationException {
        RulesInstantiationStrategy rulesInstantiationStrategy;
        ClassLoader classLoader = buildClassLoader(dependencyManager);
        if (!isProject()) {
            rulesInstantiationStrategy = RulesInstantiationStrategyFactory
                .getStrategy(module, executionMode, dependencyManager, classLoader);
        } else {
            Collection<Module> modules = getModules();
            if (modules.isEmpty()) {
                throw new IllegalStateException("Expected at least one module in the project.");
            }
            rulesInstantiationStrategy = new SimpleMultiModuleInstantiationStrategy(getModules(),
                dependencyManager,
                classLoader,
                executionMode);
        }

        Map<String, Object> parameters = configureExternalParameters(dependencyManager);

        rulesInstantiationStrategy.setExternalParameters(parameters);
        rulesInstantiationStrategy.setServiceClass(EmptyInterface.class); // Prevent
        // interface
        // generation
        boolean oldValidationState = ValidationManager.isValidationEnabled();
        try {
            ValidationManager.turnOffValidation();
            CompiledOpenClass compiledOpenClass = rulesInstantiationStrategy.compile();
            compiledDependency = new CompiledDependency(dependencyName, compiledOpenClass);
            log.debug("Dependency '{}' is saved in cache.", dependencyName);
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

    protected CompiledDependency onCompilationFailure(Exception ex,
            AbstractDependencyManager dependencyManager) throws OpenLCompilationException {
        throw new OpenLCompilationException(String.format("Failed to load dependency '%s'.", dependencyName), ex);
    }

    @Override
    public String getDependencyName() {
        return dependencyName;
    }

    @Override
    public void reset() {
        if (compiledDependency != null) {
            OpenClassUtil.release(compiledDependency.getCompiledOpenClass());
        }
        compiledDependency = null;
    }

    public interface EmptyInterface {
    }
}