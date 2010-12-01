package org.openl.rules.project.instantiation;

import org.openl.CompiledOpenClass;
import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.model.Module;

/**
 * Compiles {@link Module} and gets {@link CompiledOpenClass} and instance of
 * Wrapper(or Proxy for interface)
 * 
 * @author PUdalau
 */
public abstract class RulesInstantiationStrategy {
    
    private Module module;
    private Class<?> clazz;
    private boolean executionMode;
    private ClassLoader classLoader;
    private IDependencyManager dependencyManager;

    public RulesInstantiationStrategy(Module module, boolean executionMode, IDependencyManager dependencyManager) {
        this(module, executionMode, dependencyManager, null);
    }
    
    public RulesInstantiationStrategy(Module module, boolean executionMode, IDependencyManager dependencyManager, ClassLoader classLoader) {
        this.module = module;
        this.executionMode = executionMode;
        this.dependencyManager = dependencyManager;
        this.classLoader = classLoader;
    }

    public Module getModule() {
        return module;
    }

    protected boolean isExecutionMode() {
        return executionMode;
    }
    
    protected IDependencyManager getDependencyManager() {
        return dependencyManager;
    }

    /**
     * Returns <code>Class</code> object of interface or class corresponding to
     * rules with all published methods and fields.
     * 
     * @return interface or class
     * @throws ClassNotFoundException
     */
    public Class<?> getServiceClass() throws ClassNotFoundException {
        if (clazz == null) {
            clazz = getClassLoader().loadClass(module.getClassname());
        }

        return clazz;
    }

    /**
     * Compiles module.
     * 
     * @param reload Boolean flag that indicates whether classloader must be
     *            reloaded or used existing.
     * @return CompiledOpenClass that represents overall info about module
     *         rules.
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     */
    public CompiledOpenClass compile(ReloadType reloadType) throws InstantiationException, IllegalAccessException,
            ClassNotFoundException {
        if (reloadType == ReloadType.FORCED) {
            forcedReset();
        }
        return compile(getServiceClass(), reloadType == ReloadType.NO);
    }

    /**
     * Creates instance of class handling all rules invocations. The class will
     * be instance of class got with {@link #getServiceClass()}.
     * 
     * @param reload Boolean flag that indicates whether classloader must be
     *            reloaded or used existing.
     * @return instance of {@link #getServiceClass()} result
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     * 
     * @deprecated ReloadType should be moved to class which uses instantiation strategies and can be able to manage class loaders in right way. 
     */
    public Object instantiate(ReloadType reloadType) throws InstantiationException, IllegalAccessException,
            ClassNotFoundException {
        if (reloadType == ReloadType.FORCED) {
            forcedReset();
        }
        return instantiate(getServiceClass(), reloadType == ReloadType.NO);
    }

    protected void forcedReset() {
        getModule().getProject().getClassLoader(true);
    }

    /**
     * Returns ClassLoader for the current module inside the project.
     * 
     * @return {@link ClassLoader} for the current module.
     */
    protected ClassLoader getClassLoader() {
        
        if (classLoader != null) {
            return classLoader;
        }
        
        return module.getProject().getClassLoader(false);
    }
    
    protected abstract Object instantiate(Class<?> clazz, boolean useExisting) throws InstantiationException,
            IllegalAccessException;

    protected abstract CompiledOpenClass compile(Class<?> clazz, boolean useExisting) throws InstantiationException,
            IllegalAccessException;
}
