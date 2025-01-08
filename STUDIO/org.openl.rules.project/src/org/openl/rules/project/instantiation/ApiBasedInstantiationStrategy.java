package org.openl.rules.project.instantiation;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.openl.classloader.OpenLClassLoader;
import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.model.MethodFilter;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.runtime.InterfaceClassGenerator;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.util.CollectionUtils;

/**
 * The simplest {@link RulesInstantiationStrategy} for module that contains only Excel file.
 *
 * @author PUdalau
 */
public class ApiBasedInstantiationStrategy extends CommonRulesInstantiationStrategy {

    /**
     * Rules engine factory for module that contains only Excel file.
     */
    private RulesEngineFactory<?> engineFactory;
    private final Module module;

    public ApiBasedInstantiationStrategy(Module module,
                                         IDependencyManager dependencyManager,
                                         ClassLoader classLoader,
                                         boolean executionMode) {
        super(executionMode, dependencyManager, classLoader);
        this.module = module;
    }

    @Override
    public void reset() {
        super.reset();
        if (engineFactory != null) {
            getEngineFactory().reset();
        }
    }

    @Override
    public void forcedReset() {
        super.forcedReset();
        engineFactory = null;
    }
    public Module getModule() {
        return module;
    }

    // Single module strategy does not compile dependencies. Exception not required.
    @Override
    public ClassLoader getClassLoader() {
        if (classLoader == null) {
            classLoader = initClassLoader();
        }
        return classLoader;
    }

    @Override
    protected ClassLoader initClassLoader() {
        ProjectDescriptor project = getModule().getProject();
        return new OpenLClassLoader(project.getClassPathUrls(), Thread.currentThread().getContextClassLoader());
    }

    @Override
    public Collection<Module> getModules() {
        return Collections.singleton(getModule());
    }

    private Map<String, Object> prepareExternalParameters() {
        Map<String, Object> externalProperties = new HashMap<>();
        if (getModule().getProperties() != null) {
            externalProperties.putAll(getModule().getProperties());
        }
        if (getExternalParameters() != null) {
            externalProperties.putAll(getExternalParameters());
        }
        return externalProperties;
    }

    @Override
    public Class<?> getGeneratedRulesClass() throws RulesInstantiationException {
        // Service class for current implementation will be class, generated at
        // runtime by factory.

        // Using project class loader for interface generation.
        //
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClassLoader());
        try {
            return getEngineFactory().getInterfaceClass();
        } catch (Exception e) {
            throw new RulesInstantiationException("Failed to resolve a interface.", e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected RulesEngineFactory<?> getEngineFactory() {
        Class<Object> serviceClass = (Class<Object>) getServiceClass();
        if (engineFactory == null) {

            Module module = getModule();
            IOpenSourceCodeModule source = new ModulePathSourceCodeModule(module);
            source.setParams(prepareExternalParameters());

            engineFactory = new RulesEngineFactory<>(source, serviceClass);

            // Information for interface generation, if generation required.
            MethodFilter methodFilter = module.getMethodFilter();
            if (methodFilter != null && (CollectionUtils.isNotEmpty(methodFilter.getExcludes()) || CollectionUtils
                    .isNotEmpty(methodFilter.getIncludes()))) {
                String[] includes = new String[]{};
                String[] excludes = new String[]{};
                includes = methodFilter.getIncludes().toArray(includes);
                excludes = methodFilter.getExcludes().toArray(excludes);
                engineFactory.setInterfaceClassGenerator(new InterfaceClassGenerator(includes, excludes));
            }

            engineFactory.setExecutionMode(isExecutionMode());
            engineFactory.setDependencyManager(getDependencyManager());
        }
        return engineFactory;
    }

    @Override
    public Object instantiate(Class<?> rulesClass, boolean ignoreCompilationErrors) throws RulesInstantiationException {

        // Ensure that instantiation will be done in strategy classLoader.
        //
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClassLoader());
        try {
            return getEngineFactory().newEngineInstance(ignoreCompilationErrors);
        } catch (Exception e) {
            throw new RulesInstantiationException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    @Override
    public void setServiceClass(Class<?> serviceClass) {
        super.setServiceClass(serviceClass);
        if (engineFactory != null) {
            engineFactory.setInterfaceClass((Class) serviceClass);
        }
    }
}
