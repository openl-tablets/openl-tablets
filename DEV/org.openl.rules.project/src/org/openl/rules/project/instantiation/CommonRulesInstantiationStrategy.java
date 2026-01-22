package org.openl.rules.project.instantiation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

import org.openl.CompiledOpenClass;
import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.model.MethodFilter;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.xml.XmlRulesDeploySerializer;
import org.openl.rules.runtime.InterfaceClassGenerator;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.source.IOpenSourceCodeModule;

public abstract class CommonRulesInstantiationStrategy implements RulesInstantiationStrategy {

    /**
     * <code>Class</code> object of interface or class corresponding to rules with all published methods and fields.
     */
    private Class<?> serviceClass;

    protected final Collection<Module> modules;
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

    /**
     * Creates rules instantiation strategy with defined classLoader.
     *
     * @param executionMode     {@link #executionMode}
     * @param dependencyManager {@link #dependencyManager}
     * @param classLoader       {@link #classLoader}
     */
    public CommonRulesInstantiationStrategy(Collection<Module> modules,
                                            boolean executionMode,
                                            IDependencyManager dependencyManager,
                                            ClassLoader classLoader) {
        this.modules = modules;
        this.dependencyManager = Objects.requireNonNull(dependencyManager, "dependencyManager cannot be null");
        this.executionMode = executionMode;
        this.classLoader = classLoader;
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

    protected abstract ClassLoader initClassLoader() throws RulesInstantiationException;

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
        RulesEngineFactory engineFactory1 = getEngineFactory();
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClassLoader());
        try {
            return engineFactory1.getCompiledOpenClass();
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

    abstract protected IOpenSourceCodeModule createSource();

}
