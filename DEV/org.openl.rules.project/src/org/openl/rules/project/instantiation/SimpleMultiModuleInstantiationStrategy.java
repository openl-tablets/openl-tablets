package org.openl.rules.project.instantiation;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.openl.CompiledOpenClass;
import org.openl.classloader.OpenLClassLoader;
import org.openl.dependency.CompiledDependency;
import org.openl.dependency.IDependencyManager;
import org.openl.engine.OpenLCompileManager;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.project.model.Module;
import org.openl.rules.source.impl.VirtualSourceCodeModule;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.code.IDependency;
import org.openl.util.IOUtils;

/**
 * The simplest way of multimodule instantiation strategy. There will be created virtual module that depends on each
 * predefined module(means virtual module will have dependency for each module).
 *
 * @author PUdalau
 */
public class SimpleMultiModuleInstantiationStrategy extends CommonRulesInstantiationStrategy {

    private final Collection<Module> modules;

    public SimpleMultiModuleInstantiationStrategy(Collection<Module> modules,
                                                  IDependencyManager dependencyManager,
                                                  ClassLoader classLoader,
                                                  boolean executionMode) {
        super(executionMode, dependencyManager, classLoader);
        this.modules = modules;
    }

    public SimpleMultiModuleInstantiationStrategy(Collection<Module> modules,
                                                  IDependencyManager dependencyManager,
                                                  boolean executionMode) {
        this(modules, dependencyManager, null, executionMode);
    }

    @Override
    protected Collection<Module> getModules() {
        return modules;
    }

    @Override
    protected ClassLoader initClassLoader() throws RulesInstantiationException {
        OpenLClassLoader classLoader = new OpenLClassLoader(Thread.currentThread().getContextClassLoader());
        try {
            modules.stream()
                    .map(Module::getProject)
                    .map(AbstractDependencyManager::buildResolvedDependency)
                    .map(x -> {
                        try {
                            return getDependencyManager().loadDependency(x);
                        } catch (OpenLCompilationException e) {
                            throw new RuntimeException(e.getMessage(), e);
                        }
                    })
                    .map(CompiledDependency::getCompiledOpenClass)
                    .map(CompiledOpenClass::getClassLoader)
                    .forEach(classLoader::addClassLoader);
        } catch (Exception e) {
            // If exception is thrown, we must close classLoader in this method and rethrow exception.
            // If no exception, classLoader will be closed later.
            IOUtils.closeQuietly(classLoader);
            throw new RulesInstantiationException(e.getMessage(), e);
        }
        return classLoader;
    }

    /**
     * @return Special empty virtual {@link IOpenSourceCodeModule} with dependencies on all modules.
     */
    @Override
    protected IOpenSourceCodeModule createSource() {
        List<IDependency> dependencies = getModules().stream()
                .map(AbstractDependencyManager::buildResolvedDependency)
                .distinct()
                .collect(Collectors.toList());

        var params = new HashMap<String, Object>();
        if (getExternalParameters() != null) {
            params.putAll(getExternalParameters());
        }
        if (params.get(OpenLCompileManager.EXTERNAL_DEPENDENCIES_KEY) != null) {
            @SuppressWarnings("unchecked")
            List<IDependency> externalDependencies = (List<IDependency>) params
                    .get(OpenLCompileManager.EXTERNAL_DEPENDENCIES_KEY);
            dependencies.addAll(externalDependencies);
        }
        params.put(OpenLCompileManager.EXTERNAL_DEPENDENCIES_KEY, dependencies);

        var source = new VirtualSourceCodeModule();
        source.setParams(params);
        return source;
    }
}