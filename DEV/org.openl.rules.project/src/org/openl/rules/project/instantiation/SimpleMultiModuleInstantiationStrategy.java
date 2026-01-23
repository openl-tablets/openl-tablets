package org.openl.rules.project.instantiation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.openl.CompiledOpenClass;
import org.openl.classloader.OpenLClassLoader;
import org.openl.dependency.CompiledDependency;
import org.openl.dependency.IDependencyManager;
import org.openl.engine.OpenLCompileManager;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.project.model.MethodFilter;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.xml.XmlRulesDeploySerializer;
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
public class SimpleMultiModuleInstantiationStrategy implements RulesInstantiationStrategy {

    /**
     * <code>Class</code> object of interface or class corresponding to rules with all published methods and fields.
     */
    private Class<?> serviceClass;

    private final Collection<Module> modules;
    /**
     * Flag indicating is it execution mode or not. In execution mode all meta info that is not used in rules running is
     * being cleaned.
     */
    private final boolean executionMode;

    /**
     * <code>ClassLoader</code> that is used in strategy to compile and instantiate Openl rules.
     */
    private ClassLoader classLoader;

    private RulesEngineFactory<?> engineFactory;

    /**
     * {@link IDependencyManager} for projects that have dependent modules.
     */
    private final IDependencyManager dependencyManager;

    private Map<String, Object> externalParameters;


    public SimpleMultiModuleInstantiationStrategy(Collection<Module> modules,
                                                  IDependencyManager dependencyManager,
                                                  ClassLoader classLoader,
                                                  boolean executionMode) {
        this.modules = modules;
        this.dependencyManager = Objects.requireNonNull(dependencyManager, "dependencyManager cannot be null");
        this.executionMode = executionMode;
        this.classLoader = classLoader;
    }

    public SimpleMultiModuleInstantiationStrategy(Collection<Module> modules,
                                                  IDependencyManager dependencyManager,
                                                  boolean executionMode) {
        this(modules, dependencyManager, null, executionMode);
    }

    @Override
    public Object instantiate() throws RulesInstantiationException {
        return instantiate(false);
    }

    @Override
    public Object instantiate(boolean ignoreCompilationErrors) throws RulesInstantiationException {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClassLoader());
            return getEngineFactory().newEngineInstance(ignoreCompilationErrors);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    @Override
    public ClassLoader getClassLoader() throws RulesInstantiationException {
        if (classLoader == null) {
            classLoader = initClassLoader();
        }
        return classLoader;
    }

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

    @Override
    public Class<?> getInstanceClass() throws RulesInstantiationException {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClassLoader());
            if (serviceClass != null) {
                return serviceClass;
            } else {
                return getEngineFactory().getInterfaceClass();
            }
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    protected IDependencyManager getDependencyManager() {
        return dependencyManager;
    }

    @Override
    public void setServiceClass(Class<?> serviceClass) {
        this.serviceClass = serviceClass;
        if (engineFactory != null) {
            engineFactory.setInterfaceClass((Class) serviceClass);
        }
    }

    protected Map<String, Object> getExternalParameters() {
        return externalParameters;
    }

    @Override
    public void setExternalParameters(Map<String, Object> parameters) {
        this.externalParameters = parameters;
    }

    @Override
    public CompiledOpenClass compile() throws RulesInstantiationException {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClassLoader());
        try {
            return getEngineFactory().getCompiledOpenClass();
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    protected RulesEngineFactory<?> getEngineFactory() {
        if (engineFactory == null) {

            // Information for interface generation, if generation required.
            Collection<String> allIncludes = new HashSet<>();
            Collection<String> allExcludes = new HashSet<>();
            for (Module m : modules) {
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
            String[] includes = new String[]{};
            String[] excludes = new String[]{};
            if (!allIncludes.isEmpty() || !allExcludes.isEmpty()) {
                includes = allIncludes.toArray(includes);
                excludes = allExcludes.toArray(excludes);
            }

            engineFactory = new RulesEngineFactory<>(createSource(), serviceClass);
            engineFactory.setInterfaceClassGenerator(new InterfaceClassGenerator(includes, excludes, isProvideRuntimeContext()));
            engineFactory.setExecutionMode(executionMode);
            engineFactory.setDependencyManager(getDependencyManager());
        }

        return engineFactory;
    }

    private boolean isProvideRuntimeContext() {
        if (!modules.isEmpty()) {
            Path deployXmlPath = modules.iterator().next().getProject().getProjectFolder().resolve("rules-deploy.xml");
            if (Files.exists(deployXmlPath)) {
                try (var stream = Files.newInputStream(deployXmlPath)) {
                    return Boolean.TRUE.equals(new XmlRulesDeploySerializer().deserialize(stream).isProvideRuntimeContext());
                } catch (Exception ignored) {
                }
            }
        }
        return false;
    }

    /**
     * @return Special empty virtual {@link IOpenSourceCodeModule} with dependencies on all modules.
     */
    protected IOpenSourceCodeModule createSource() {
        List<IDependency> dependencies = modules.stream()
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
