package org.openl.rules.project.instantiation;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

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

public class SimpleDependencyLoader implements IDependencyLoader {

    private final Logger log = LoggerFactory.getLogger(SimpleDependencyLoader.class);

    protected final String dependencyName;
    protected final Collection<Module> modules;
    protected CompiledDependency compiledDependency = null;
    private boolean executionMode = false;
    private boolean singleModuleMode = false;
    private final boolean projectDependency;

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

    @Override
    public boolean isProjectDependency(String dependencyName) {
        return Objects.equals(this.dependencyName, dependencyName) && projectDependency;
    }

    @Override
    public boolean isModuleDependency(String dependencyName) {
        return Objects.equals(this.dependencyName, dependencyName) && !projectDependency;
    }

    public SimpleDependencyLoader(String dependencyName,
            Collection<Module> modules,
            boolean singleModuleMode,
            boolean executionMode,
            boolean projectDependency) {
        this.dependencyName = Objects.requireNonNull(dependencyName, "dependencyName cannot be null");
        this.modules = Objects.requireNonNull(modules, "modules cannot be null");
        if (this.modules.isEmpty()) {
            throw new IllegalArgumentException("Collection of modules cannot be empty.");
        }
        this.executionMode = executionMode;
        this.singleModuleMode = singleModuleMode;
        this.projectDependency = projectDependency;
    }

    @Override
    public CompiledDependency load(String dependencyName, IDependencyManager dm) throws OpenLCompilationException {
        if (Objects.equals(this.dependencyName, dependencyName)) {
            if (!(dm instanceof AbstractDependencyManager)) {
                throw new IllegalStateException(String.format("This loader works only with subclasses of %s.", AbstractDependencyManager.class
                        .getTypeName() ));
            }

            final AbstractDependencyManager dependencyManager = (AbstractDependencyManager) dm;

            if (compiledDependency != null) {
                log.debug("Dependency '{}' has been used from cache.", dependencyName);
                return compiledDependency;
            }
            log.debug("Dependency '{}' is not found in cache.", dependencyName);
            return compileDependency(dependencyName, dependencyManager);
        }
        return null;
    }

    protected ClassLoader buildClassLoader(AbstractDependencyManager dependencyManager) {
        return dependencyManager.getClassLoader(modules.iterator().next().getProject());
    }

    protected CompiledDependency compileDependency(String dependencyName,
            AbstractDependencyManager dependencyManager) throws OpenLCompilationException {
        RulesInstantiationStrategy rulesInstantiationStrategy;
        ClassLoader classLoader = buildClassLoader(dependencyManager);
        if (!projectDependency && modules.size() == 1) {
            rulesInstantiationStrategy = RulesInstantiationStrategyFactory
                .getStrategy(modules.iterator().next(), executionMode, dependencyManager, classLoader);
        } else {
            if (projectDependency && !modules.isEmpty()) {
                rulesInstantiationStrategy = new SimpleMultiModuleInstantiationStrategy(modules,
                    dependencyManager,
                    classLoader,
                    executionMode);
            } else {
                throw new IllegalStateException("Сollection of modules must not be empty.");
            }
        }

        Map<String, Object> parameters = configureParameters(dependencyManager);

        rulesInstantiationStrategy.setExternalParameters(parameters);
        rulesInstantiationStrategy.setServiceClass(EmptyInterface.class); // Prevent
        // interface
        // generation
        boolean oldValidationState = OpenLValidationManager.isValidationEnabled();
        try {
            OpenLValidationManager.turnOffValidation();
            CompiledOpenClass compiledOpenClass = rulesInstantiationStrategy.compile();
            compiledDependency = new CompiledDependency(dependencyName, compiledOpenClass);
            log.debug("Dependency '{}' has been saved in cache.", dependencyName);
            return compiledDependency;
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            return onCompilationFailure(ex, dependencyManager);
        } finally {
            if (oldValidationState) {
                OpenLValidationManager.turnOnValidation();
            }
        }
    }

    protected CompiledDependency onCompilationFailure(Exception ex,
            AbstractDependencyManager dependencyManager) throws OpenLCompilationException {
        throw new OpenLCompilationException(String.format("Failed to load dependency '%s'.", dependencyName),  ex);
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