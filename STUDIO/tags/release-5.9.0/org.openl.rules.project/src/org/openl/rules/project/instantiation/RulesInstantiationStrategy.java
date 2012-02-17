package org.openl.rules.project.instantiation;

import java.net.URL;

import org.openl.CompiledOpenClass;
import org.openl.classloader.OpenLClassLoaderHelper;
import org.openl.classloader.SimpleBundleClassLoader;
import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.model.Module;

/**
 * Compiles {@link Module} and gets {@link CompiledOpenClass} and instance of
 * Wrapper(or Proxy for interface)
 * 
 * @author PUdalau
 */
public abstract class RulesInstantiationStrategy {
    
    /**
     * Root <code>Module</code> that is used as start point for Openl compilation.
     */
    private Module module;
    
    /**
     * <code>Class</code> object of interface or class corresponding to
     * rules with all published methods and fields.
     */
    private Class<?> rulesClass;
    
    /**
     * Flag indicating is it execution mode or not. 
     * In execution mode all meta info that is not used in rules running is being cleaned.
     */
    private boolean executionMode;
    
    /**
     * <code>ClassLoader</code> that is used in strategy to compile and instantiate Openl rules.
     */
    private ClassLoader classLoader;
    
    /**
     * {@link IDependencyManager} for projects that have dependent modules.
     */
    private IDependencyManager dependencyManager;
    
    /**
     * Creates rules instantiation strategy with empty {@link ClassLoader}.(See {@link #getClassLoader()} for more<br> 
     * information which classLoader will be used).
     * 
     * @param module {@link #module}
     * @param executionMode {@link #executionMode}
     * @param dependencyManager {@link #dependencyManager}
     */
    public RulesInstantiationStrategy(Module module, boolean executionMode, IDependencyManager dependencyManager) {
        this(module, executionMode, dependencyManager, null);
    }
    
    /**
     * Creates rules instantiation strategy with defined classLoader.
     * 
     * @param module {@link #module}
     * @param executionMode {@link #executionMode}
     * @param dependencyManager {@link #dependencyManager}
     * @param classLoader {@link #classLoader}
     */
    public RulesInstantiationStrategy(Module module, boolean executionMode, IDependencyManager dependencyManager, 
            ClassLoader classLoader) {
        this.module = module;
        this.executionMode = executionMode;
        this.dependencyManager = dependencyManager;        
        this.classLoader = classLoader;
    }

    public Module getModule() {
        return module;
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
        return compile(reloadType == ReloadType.NO);
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
    
    /**
     * Returns ClassLoader for the current module inside the project.
     * If classLoader was set during the construction of the strategy - returns it.<br>
     * If no, creates {@link SimpleBundleClassLoader} with project classLoader of current module as parent.
     * 
     * @return {@link ClassLoader} that will be used for openl compilation.
     */
    
    @SuppressWarnings("deprecation")
    protected ClassLoader getClassLoader() {        
        if (classLoader == null) {
            ClassLoader parent = getModule().getProject().getClassLoader(false);
            URL[] urls = getModule().getProject().getClassPathUrls();          
            classLoader = new SimpleBundleClassLoader(parent);
            OpenLClassLoaderHelper.extendClasspath((SimpleBundleClassLoader)classLoader, urls);
        }
        
        return classLoader;
    }
    
    /**
     * Returns <code>Class</code> object of interface or class corresponding to
     * rules with all published methods and fields.
     * 
     * @return interface or class
     * @throws ClassNotFoundException     
     */
    public abstract Class<?> getServiceClass() throws ClassNotFoundException;

    protected boolean isExecutionMode() {
        return executionMode;
    }
    
    protected IDependencyManager getDependencyManager() {
        return dependencyManager;
    }
    
    public void setRulesInterface(Class<?> rulesInterface){
        this.rulesClass = rulesInterface;
    }
    
    public Class<?> getRulesClass(){
        return rulesClass;
    }    
    
    
    @SuppressWarnings("deprecation")
    protected void forcedReset() {
        getModule().getProject().getClassLoader(true);
    }    
    
    /**
     * Inner implementation. Creates instance of class handling all rules invocations. The class will
     * be instance of class got with {@link #getServiceClass()}.
     * 
     * @param rulesClass
     * @param useExisting
     * @return
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    protected abstract Object instantiate(Class<?> rulesClass, boolean useExisting) throws InstantiationException,
            IllegalAccessException;

    protected abstract CompiledOpenClass compile(boolean useExisting) throws InstantiationException,
            IllegalAccessException;
}
