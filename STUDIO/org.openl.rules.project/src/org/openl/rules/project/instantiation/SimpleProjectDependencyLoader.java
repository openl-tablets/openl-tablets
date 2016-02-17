package org.openl.rules.project.instantiation;

import java.util.Collection;
import java.util.Map;

import org.openl.CompiledOpenClass;
import org.openl.dependency.CompiledDependency;
import org.openl.dependency.IDependencyManager;
import org.openl.dependency.loader.IDependencyLoader;
import org.openl.exception.OpenLCompilationException;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.project.dependencies.ProjectExternalDependenciesHelper;
import org.openl.rules.project.model.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleProjectDependencyLoader implements IDependencyLoader {

    private final Logger log = LoggerFactory.getLogger(SimpleProjectDependencyLoader.class);

    private final String dependencyName;
    private final Collection<Module> modules;
    private CompiledDependency compiledDependency = null;
    private boolean executionMode = false;
    private boolean singleModuleMode = false;

    protected Map<String, Object> configureParameters(IDependencyManager dependencyManager) {
        Map<String, Object> params = dependencyManager.getExternalParameters();
        if (!singleModuleMode) {
            params = ProjectExternalDependenciesHelper.getExternalParamsWithProjectDependencies(params, getModules()
            );
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
    
    public SimpleProjectDependencyLoader(String dependencyName, Collection<Module> modules, boolean singleModuleMode, boolean executionMode) {
        if (dependencyName == null) {
            throw new IllegalArgumentException("dependencyName arg can't be null!");
        }
        if (modules == null || modules.isEmpty()) {
            throw new IllegalArgumentException("modules arg can't be null or empty!");
        }
        this.dependencyName = dependencyName;
        this.modules = modules;
        this.executionMode = executionMode;
        this.singleModuleMode = singleModuleMode;
    }

    public SimpleProjectDependencyLoader(String dependencyName, Collection<Module> modules, boolean singleModuleMode) {
        this(dependencyName, modules, singleModuleMode, false);
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
            boolean isCircularDependency = dependencyManager.getCompilationStack().contains(dependencyName);
            if (!isCircularDependency && !dependencyManager.getCompilationStack().isEmpty()){
                AbstractProjectDependencyManager.DependencyReference dr = new AbstractProjectDependencyManager.DependencyReference(dependencyManager.getCompilationStack().getLast(), dependencyName);
                dependencyManager.getDependencyReferences().add(dr);
            }

            if (compiledDependency != null) {
                log.debug("Dependency for dependencyName = {} from cache was returned.", dependencyName);
                return compiledDependency;
            }

            try {
                if (isCircularDependency) {
                    OpenLMessagesUtils.addError("Circular dependency detected in module: " + dependencyName);
                    return null;
                }
                
                RulesInstantiationStrategy rulesInstantiationStrategy;
                ClassLoader classLoader = dependencyManager.getClassLoader(modules.iterator().next().getProject());
                if (modules.size() == 1) {
                    dependencyManager.getCompilationStack().add(dependencyName);
                    log.debug("Creating dependency for dependencyName = {}", dependencyName);
                    rulesInstantiationStrategy = RulesInstantiationStrategyFactory.getStrategy(modules.iterator()
                            .next(), executionMode, dependencyManager, classLoader);
                } else {
                    if (modules.size() > 1) {
                        rulesInstantiationStrategy = new SimpleMultiModuleInstantiationStrategy(modules,
                                dependencyManager,
                                classLoader, executionMode);
                    } else {
                        throw new IllegalStateException("Modules collection can't be empty");
                    }
                }

                Map<String, Object> parameters = configureParameters(dependencyManager);

                rulesInstantiationStrategy.setExternalParameters(parameters);
                rulesInstantiationStrategy.setServiceClass(EmptyInterface.class); // Prevent
                // interface
                // generation
                try {
                    CompiledOpenClass compiledOpenClass = rulesInstantiationStrategy.compile();
                    CompiledDependency cd = new CompiledDependency(dependencyName, compiledOpenClass);
                    log.debug("Dependency for dependencyName = {} was stored to cache.", dependencyName);
                    compiledDependency = cd;
                    return compiledDependency;
                } catch (Exception ex) {
                    log.error(ex.getMessage(), ex);
                    return onCompilationFailure(ex, dependencyManager);
                }
            } finally {
                dependencyManager.getCompilationStack().pollLast();
            }
        }
        return null;
    }

    protected CompiledDependency onCompilationFailure(Exception ex, AbstractProjectDependencyManager dependencyManager) throws OpenLCompilationException {
        throw new OpenLCompilationException("Can't load dependency with name '" + dependencyName + "'.", ex);
    }

    public String getDependencyName() {
        return dependencyName;
    }
    
    public void reset() {
        compiledDependency = null;
    }

    public interface EmptyInterface {
    }
}