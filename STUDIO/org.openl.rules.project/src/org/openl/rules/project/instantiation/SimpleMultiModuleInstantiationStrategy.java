package org.openl.rules.project.instantiation;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.openl.CompiledOpenClass;
import org.openl.classloader.OpenLClassLoader;
import org.openl.dependency.CompiledDependency;
import org.openl.dependency.IDependencyManager;
import org.openl.engine.OpenLCompileManager;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.project.model.MethodFilter;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.runtime.InterfaceClassGenerator;
import org.openl.rules.runtime.RulesEngineFactory;
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

    private RulesEngineFactory<?> engineFactory;
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
    public Collection<Module> getModules() {
        return modules;
    }

    @Override
    public void reset() {
        super.reset();
        engineFactory = null;
    }

    @Override
    protected ClassLoader initClassLoader() throws RulesInstantiationException {
        OpenLClassLoader classLoader = new OpenLClassLoader(Thread.currentThread().getContextClassLoader());
        try {
            Set<ProjectDescriptor> projectDescriptors = modules.stream()
                    .map(Module::getProject)
                    .collect(Collectors.toSet());
            for (ProjectDescriptor pd : projectDescriptors) {
                try {
                    CompiledDependency compiledDependency = getDependencyManager()
                            .loadDependency(AbstractDependencyManager.buildResolvedDependency(pd));
                    CompiledOpenClass compiledOpenClass = compiledDependency.getCompiledOpenClass();
                    classLoader.addClassLoader(compiledOpenClass.getClassLoader());
                } catch (OpenLCompilationException e) {
                    throw new RulesInstantiationException(e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            // If exception is thrown, we must close classLoader in this method and rethrow exception.
            // If no exception, classLoader will be closed later.
            IOUtils.closeQuietly(classLoader);
            throw e;
        }
        return classLoader;
    }


    @Override
    public Class<?> getGeneratedRulesClass() throws RulesInstantiationException {
        // Using project class loader for interface generation.
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClassLoader());
        try {
            return getEngineFactory().getInterfaceClass();
        } catch (Exception e) {
            throw new RulesInstantiationException("Failed to resolve an interface.", e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    @Override
    public Object instantiate(Class<?> rulesClass, boolean ignoreCompilationErrors) throws RulesInstantiationException {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClassLoader());
        try {
            return getEngineFactory().newEngineInstance(ignoreCompilationErrors);
        } catch (Exception e) {
            throw new RulesInstantiationException("Failed to instantiate.", e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected RulesEngineFactory<?> getEngineFactory() {
        Class<?> serviceClass = getServiceClass();
        if (engineFactory == null) {
            engineFactory = new RulesEngineFactory<>(createVirtualSourceCodeModule(), (Class<Object>) serviceClass);
            engineFactory.setExecutionMode(isExecutionMode());

            // Information for interface generation, if generation required.
            Collection<String> allIncludes = new HashSet<>();
            Collection<String> allExcludes = new HashSet<>();
            for (Module m : getModules()) {
                MethodFilter methodFilter = m.getMethodFilter();
                if (methodFilter != null) {
                    if (methodFilter.getIncludes() != null) {
                        allIncludes.addAll(methodFilter.getIncludes());
                    }
                    if (methodFilter.getExcludes() != null) {
                        allExcludes.addAll(methodFilter.getExcludes());
                    }
                }
            }
            if (!allIncludes.isEmpty() || !allExcludes.isEmpty()) {
                String[] includes = new String[]{};
                String[] excludes = new String[]{};
                includes = allIncludes.toArray(includes);
                excludes = allExcludes.toArray(excludes);
                engineFactory.setInterfaceClassGenerator(new InterfaceClassGenerator(includes, excludes));
            }
            engineFactory.setDependencyManager(getDependencyManager());
        }

        return engineFactory;
    }

    @Override
    public void setServiceClass(Class<?> serviceClass) {
        super.setServiceClass(serviceClass);
        if (engineFactory != null) {
            engineFactory.setInterfaceClass((Class) serviceClass);
        }
    }

    /**
     * @return Special empty virtual {@link IOpenSourceCodeModule} with dependencies on all modules.
     */
    private IOpenSourceCodeModule createVirtualSourceCodeModule() {
        List<IDependency> dependencies = getModules().stream()
                .map(AbstractDependencyManager::buildResolvedDependency)
                .distinct()
                .collect(Collectors.toList());
        Map<String, Object> params = new HashMap<>();
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
        IOpenSourceCodeModule source = new VirtualSourceCodeModule();
        source.setParams(params);

        return source;
    }
}