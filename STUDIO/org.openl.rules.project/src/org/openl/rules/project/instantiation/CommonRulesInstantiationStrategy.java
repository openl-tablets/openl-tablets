package org.openl.rules.project.instantiation;

import java.util.Map;
import java.util.Objects;

import org.openl.CompiledOpenClass;
import org.openl.dependency.IDependencyManager;
import org.openl.runtime.AEngineFactory;

public abstract class CommonRulesInstantiationStrategy implements RulesInstantiationStrategy {

    /**
     * <code>Class</code> object of interface or class corresponding to rules with all published methods and fields.
     */
    private Class<?> serviceClass;

    /**
     * Flag indicating is it execution mode or not. In execution mode all meta info that is not used in rules running is
     * being cleaned.
     */
    private boolean executionMode;

    /**
     * <code>ClassLoader</code> that is used in strategy to compile and instantiate Openl rules.
     */
    protected ClassLoader classLoader;

    /**
     * {@link IDependencyManager} for projects that have dependent modules.
     */
    private IDependencyManager dependencyManager;

    private Map<String, Object> externalParameters;

    /**
     * Creates rules instantiation strategy with empty {@link ClassLoader}.(See {@link #getClassLoader()} for more<br>
     * information which classLoader will be used).
     *
     * @param executionMode {@link #executionMode}
     * @param dependencyManager {@link #dependencyManager}
     */
    public CommonRulesInstantiationStrategy(boolean executionMode, IDependencyManager dependencyManager) {
        this(executionMode, dependencyManager, null);
    }

    /**
     * Creates rules instantiation strategy with defined classLoader.
     *
     * @param executionMode {@link #executionMode}
     * @param dependencyManager {@link #dependencyManager}
     * @param classLoader {@link #classLoader}
     */
    public CommonRulesInstantiationStrategy(boolean executionMode,
            IDependencyManager dependencyManager,
            ClassLoader classLoader) {
        this.dependencyManager = Objects.requireNonNull(dependencyManager, "dependencyManager cannot be null");
        this.executionMode = executionMode;
        this.classLoader = classLoader;
    }

    @Override
    public Object instantiate() throws RulesInstantiationException {
        return instantiate(getInstanceClass());
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
    public Class<?> getServiceClass() {
        return serviceClass;
    }

    @Override
    public Class<?> getInstanceClass() throws RulesInstantiationException {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClassLoader());
            if (isServiceClassDefined()) {
                return getServiceClass();
            } else {
                return getGeneratedRulesClass();
            }
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    @Override
    public boolean isServiceClassDefined() {
        return serviceClass != null;
    }

    protected boolean isExecutionMode() {
        return executionMode;
    }

    protected IDependencyManager getDependencyManager() {
        return dependencyManager;
    }

    @Override
    public void setServiceClass(Class<?> serviceClass) {
        this.serviceClass = serviceClass;
    }

    @Override
    public Class<?> getGeneratedRulesClass() throws RulesInstantiationException {
        return compile().getOpenClassWithErrors().getInstanceClass();
    }

    @Override
    public void reset() {
    }

    @Override
    public void forcedReset() {
        reset();
        classLoader = null;
    }

    /**
     * Inner implementation. Creates instance of class handling all rules invocations. The class will be instance of
     * class got with {@link #getServiceClass()}.
     *
     * @param rulesClass rule Class
     * @return instantiated object
     * @throws RulesInstantiationException
     */
    protected abstract Object instantiate(Class<?> rulesClass) throws RulesInstantiationException;

    @Override
    public Map<String, Object> getExternalParameters() {
        return externalParameters;
    }

    @Override
    public void setExternalParameters(Map<String, Object> parameters) {
        this.externalParameters = parameters;
    }

    @Override
    public CompiledOpenClass compile() throws RulesInstantiationException {
        return compileInternal(getEngineFactory());
    }

    protected abstract AEngineFactory getEngineFactory() throws RulesInstantiationException;

    protected final CompiledOpenClass compileInternal(AEngineFactory engineFactory) throws RulesInstantiationException {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClassLoader());
        try {
            return engineFactory.getCompiledOpenClass();
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

}
