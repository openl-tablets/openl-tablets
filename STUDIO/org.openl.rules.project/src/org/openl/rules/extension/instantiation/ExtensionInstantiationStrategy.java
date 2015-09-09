package org.openl.rules.extension.instantiation;

import org.apache.commons.collections4.CollectionUtils;
import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.instantiation.RulesInstantiationException;
import org.openl.rules.project.instantiation.SingleModuleInstantiationStrategy;
import org.openl.rules.project.model.Extension;
import org.openl.rules.project.model.MethodFilter;
import org.openl.rules.project.model.Module;
import org.openl.rules.runtime.InterfaceClassGeneratorImpl;
import org.openl.source.IOpenSourceCodeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExtensionInstantiationStrategy extends SingleModuleInstantiationStrategy {
    private final Logger log = LoggerFactory.getLogger(ExtensionInstantiationStrategy.class);

    /**
     * Rules engine factory for module that contains only Excel file.
     */
    private ExtensionEngineFactory<?> engineFactory;
    private final Extension extension;

    public ExtensionInstantiationStrategy(Module module,
            boolean executionMode,
            IDependencyManager dependencyManager,
            ClassLoader classLoader,
            Extension extension) {
        super(module, executionMode, dependencyManager, classLoader);
        this.extension = extension;
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
            throw new RulesInstantiationException("Can't resolve interface.", e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    @SuppressWarnings("unchecked")
    protected ExtensionEngineFactory<?> getEngineFactory() {
        Class<Object> serviceClass;
        try {
            serviceClass = (Class<Object>) getServiceClass();
        } catch (ClassNotFoundException e) {
            log.debug("Failed to get service class.", e);
            serviceClass = null;
        }
        if (engineFactory == null || (serviceClass != null && !engineFactory.getInterfaceClass()
                .equals(serviceClass))) {
            IExtensionDescriptor extensionDescriptor = ExtensionDescriptorFactory.getExtensionDescriptor(extension,
                    getClassLoader());

            IOpenSourceCodeModule source = extensionDescriptor.getSourceCode(getModule());
            source.setParams(prepareExternalParameters());

            String openlName = extensionDescriptor.getOpenLName();
            engineFactory = new ExtensionEngineFactory<Object>(openlName, source, serviceClass);

            // Information for interface generation, if generation required.
            Module m = getModule();
            MethodFilter methodFilter = m.getMethodFilter();
            if (methodFilter != null && (CollectionUtils.isNotEmpty(methodFilter.getExcludes()) || CollectionUtils.isNotEmpty(
                    methodFilter.getIncludes()))) {
                String[] includes = new String[] {};
                String[] excludes = new String[] {};
                includes = methodFilter.getIncludes().toArray(includes);
                excludes = methodFilter.getExcludes().toArray(excludes);
                engineFactory.setInterfaceClassGenerator(new InterfaceClassGeneratorImpl(includes, excludes));
            }

            engineFactory.setExecutionMode(isExecutionMode());
            engineFactory.setDependencyManager(getDependencyManager());
        }
        return engineFactory;
    }

    @Override
    public Object instantiate(Class<?> rulesClass) throws RulesInstantiationException {

        // Ensure that instantiation will be done in strategy classLoader.
        //
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClassLoader());
        try {
            return getEngineFactory().newEngineInstance();
        } catch (Exception e) {
            throw new RulesInstantiationException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

}
