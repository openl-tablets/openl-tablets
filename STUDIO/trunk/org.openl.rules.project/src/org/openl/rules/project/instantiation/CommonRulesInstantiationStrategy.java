package org.openl.rules.project.instantiation;

import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.model.Module;

public abstract class CommonRulesInstantiationStrategy implements RulesInstantiationStrategy {

    /**
     * <code>Class</code> object of interface or class corresponding to rules
     * with all published methods and fields.
     */
    private Class<?> serviceClass;

    /**
     * Flag indicating is it execution mode or not. In execution mode all meta
     * info that is not used in rules running is being cleaned.
     */
    private boolean executionMode;

    /**
     * <code>ClassLoader</code> that is used in strategy to compile and
     * instantiate Openl rules.
     */
    private ClassLoader classLoader;

    /**
     * {@link IDependencyManager} for projects that have dependent modules.
     */
    private IDependencyManager dependencyManager;

    /**
     * Creates rules instantiation strategy with empty {@link ClassLoader}.(See
     * {@link #getClassLoader()} for more<br>
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
        this.executionMode = executionMode;
        this.dependencyManager = dependencyManager;
        this.classLoader = classLoader;
    }

    @Override
    public Object instantiate() throws RulesInstantiationException, ClassNotFoundException {
        return instantiate(getInstanceClass());
    }


    @Override
    public ClassLoader getClassLoader() {
        if (classLoader == null) {
            classLoader = initClassLoader();
        }

        return classLoader;
    }

    protected abstract ClassLoader initClassLoader();

    protected void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public Class<?> getServiceClass() throws ClassNotFoundException {
        return serviceClass;
    }

    @Override
    public Class<?> getInstanceClass() throws ClassNotFoundException, RulesInstantiationException {
        if (isServiceClassDefined()) {
            return getServiceClass();
        } else {
            return getGeneratedRulesClass();
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

    @SuppressWarnings("deprecation")
    @Override
    public void forcedReset() {
        reset();
        // renew all classloaders
        for (Module module : getModules()) {
            module.getProject().getClassLoader(true);
        }
    }

    /**
     * Inner implementation. Creates instance of class handling all rules
     * invocations. The class will be instance of class got with
     * {@link #getServiceClass()}.
     * 
     * @param rulesClass
     * @param useExisting
     * @return
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    protected abstract Object instantiate(Class<?> rulesClass) throws RulesInstantiationException;
}
