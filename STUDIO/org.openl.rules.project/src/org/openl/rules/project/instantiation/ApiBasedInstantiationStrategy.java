package org.openl.rules.project.instantiation;

import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.model.MethodFilter;
import org.openl.rules.project.model.Module;
import org.openl.rules.runtime.InterfaceClassGenerator;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.util.CollectionUtils;

/**
 * The simplest {@link RulesInstantiationStrategy} for module that contains only Excel file.
 *
 * @author PUdalau
 */
public class ApiBasedInstantiationStrategy extends SingleModuleInstantiationStrategy {

    /**
     * Rules engine factory for module that contains only Excel file.
     */
    private RulesEngineFactory<?> engineFactory;

    public ApiBasedInstantiationStrategy(Module module, IDependencyManager dependencyManager, boolean executionMode) {
        super(module, dependencyManager, executionMode);
    }

    public ApiBasedInstantiationStrategy(Module module,
                                         IDependencyManager dependencyManager,
                                         ClassLoader classLoader,
                                         boolean executionMode) {
        super(module, dependencyManager, classLoader, executionMode);
    }

    @Override
    public void reset() {
        super.reset();
        if (engineFactory != null) {
            getEngineFactory().reset(false);
        }
    }

    @Override
    public void forcedReset() {
        super.forcedReset();
        engineFactory = null;
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
