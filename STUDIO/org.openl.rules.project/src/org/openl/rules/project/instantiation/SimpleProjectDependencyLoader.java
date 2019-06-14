package org.openl.rules.project.instantiation;

import java.util.Collection;
import java.util.Map;

import org.openl.CompiledOpenClass;
import org.openl.OpenClassUtil;
import org.openl.dependency.CompiledDependency;
import org.openl.dependency.IDependencyManager;
import org.openl.dependency.loader.IDependencyLoader;
import org.openl.engine.OpenLValidationManager;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.project.dependencies.ProjectExternalDependenciesHelper;
import org.openl.rules.project.model.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleProjectDependencyLoader implements IDependencyLoader {

    private final Logger log = LoggerFactory.getLogger(SimpleProjectDependencyLoader.class);

    protected final String dependencyName;
    protected final Collection<Module> modules;
    protected CompiledDependency compiledDependency = null;
    private boolean executionMode = false;
    private boolean singleModuleMode = false;
    private final boolean isProject;

    protected Map<String, Object> configureParameters(IDependencyManager dependencyManager) {
        Map<String, Object> params = dependencyManager.getExternalParameters();
        if (!singleModuleMode) {
            params = ProjectExternalDependenciesHelper.getExternalParamsWithProjectDependencies(params, getModules());
            return params;
        }
        return params;
    }

    public Collection<Module> getModules() {
        return modules;
    }

    public CompiledDependency getCompiledDependency() {
        return compiledDependency;
    }

    public boolean isExecutionMode() {
        return executionMode;
    }

    public SimpleProjectDependencyLoader(String dependencyName,
            Collection<Module> modules,
            boolean singleModuleMode,
            boolean executionMode,
            boolean isProject) {
        if (dependencyName == null) {
            throw new IllegalArgumentException("dependencyName arg must not be null!");
        }
        if (modules == null || modules.isEmpty()) {
            throw new IllegalArgumentException("modules arg must not be null or empty!");
        }
        this.dependencyName = dependencyName;
        this.modules = modules;
        this.executionMode = executionMode;
        this.singleModuleMode = singleModuleMode;
        this.isProject = isProject;
    }

    @Override
    public CompiledDependency load(String dependencyName, IDependencyManager dm) throws OpenLCompilationException {
        AbstractProjectDependencyManager dependencyManager;
        if (dm instanceof AbstractProjectDependencyManager) {
            dependencyManager = (AbstractProjectDependencyManager) dm;
        } else {
            throw new IllegalStateException("This loader works only with AbstractProjectDependencyManager!");
        }

        if (this.dependencyName.equals(dependencyName)) {
            boolean isCircularDependency = !ProjectExternalDependenciesHelper
                .isProject(dependencyName) && dependencyManager.getCompilationStack().contains(dependencyName);
            if (!isCircularDependency && !dependencyManager.getCompilationStack().isEmpty()) {
                AbstractProjectDependencyManager.DependencyReference dr = new AbstractProjectDependencyManager.DependencyReference(
                    dependencyManager.getCompilationStack().getLast(),
                    dependencyName);
                dependencyManager.getDependencyReferences().add(dr);
            }

            if (compiledDependency != null) {
                log.debug("Dependency for dependencyName = {} from cache has been returned.", dependencyName);
                return compiledDependency;
            }

            try {
                if (isCircularDependency) {
                    throw new OpenLCompilationException(
                        "Circular dependency has been detected in module: " + dependencyName);
                }

                return compileDependency(dependencyName, dependencyManager);
            } finally {
                dependencyManager.getCompilationStack().pollLast();
            }
        }
        return null;
    }

    protected ClassLoader buildClassLoader(AbstractProjectDependencyManager dependencyManager) {
        return dependencyManager.getClassLoader(modules.iterator().next().getProject());
    }

    protected CompiledDependency compileDependency(String dependencyName,
            AbstractProjectDependencyManager dependencyManager) throws OpenLCompilationException {
        RulesInstantiationStrategy rulesInstantiationStrategy;
        ClassLoader classLoader = buildClassLoader(dependencyManager);
        log.debug("Creating dependency for dependencyName = {}", dependencyName);
        dependencyManager.getCompilationStack().add(dependencyName);
        if (!isProject && modules.size() == 1) {
            rulesInstantiationStrategy = RulesInstantiationStrategyFactory
                .getStrategy(modules.iterator().next(), executionMode, dependencyManager, classLoader);
        } else {
            if (isProject && !modules.isEmpty()) {
                rulesInstantiationStrategy = new SimpleMultiModuleInstantiationStrategy(modules,
                    dependencyManager,
                    classLoader,
                    executionMode);
            } else {
                throw new IllegalStateException("Modules collection must not be empty");
            }
        }

        Map<String, Object> parameters = configureParameters(dependencyManager);

        rulesInstantiationStrategy.setExternalParameters(parameters);
        rulesInstantiationStrategy.setServiceClass(EmptyInterface.class); // Prevent
        // interface
        // generation
        boolean validationWasOn = OpenLValidationManager.isValidationEnabled();
        try {
            OpenLValidationManager.turnOffValidation();
            CompiledOpenClass compiledOpenClass = rulesInstantiationStrategy.compile();
            CompiledDependency cd = new CompiledDependency(dependencyName, compiledOpenClass);
            log.debug("Dependency for dependencyName = {} has been stored in cache.", dependencyName);
            compiledDependency = cd;
            return compiledDependency;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            return onCompilationFailure(ex, dependencyManager);
        } finally {
            if (validationWasOn) {
                OpenLValidationManager.turnOnValidation();
            }
        }
    }

    protected CompiledDependency onCompilationFailure(Exception ex,
            AbstractProjectDependencyManager dependencyManager) throws OpenLCompilationException {
        throw new OpenLCompilationException("Failed to load dependency '" + dependencyName + "'.", ex);
    }

    public String getDependencyName() {
        return dependencyName;
    }

    public void reset() {
        if (compiledDependency != null) {
            OpenClassUtil.release(compiledDependency.getCompiledOpenClass());
        }
        compiledDependency = null;
    }

    public interface EmptyInterface {
    }
}