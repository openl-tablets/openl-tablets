package org.openl.rules.project.instantiation;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Map;

import org.apache.commons.lang.reflect.ConstructorUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.CompiledOpenClass;
import org.openl.classloader.OpenLClassLoaderHelper;
import org.openl.classloader.SimpleBundleClassLoader;
import org.openl.dependency.IDependencyManager;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.main.OpenLWrapper;
import org.openl.rules.project.model.Module;

/**
 * Instantiation strategy for projects with Wrapper
 * 
 * @author PUdalau
 */
public class WrapperAdjustingInstantiationStrategy extends RulesInstantiationStrategy {
    
    private static final Log LOG = LogFactory.getLog(RulesInstantiationStrategyFactory.class);

    private OpenLWrapper wrapper;
    
    /**
     * <code>ClassLoader</code> that is used in strategy to compile and instantiate Openl rules.
     */
    private ClassLoader classLoader;
        
    public WrapperAdjustingInstantiationStrategy(Module module, boolean executionMode, 
            IDependencyManager dependencyManager) {
        super(module, executionMode, dependencyManager);
    }
    
    public WrapperAdjustingInstantiationStrategy(Module module, boolean executionMode, 
            IDependencyManager dependencyManager, ClassLoader classLoader) {
        super(module, executionMode, dependencyManager);
        this.classLoader = classLoader;
    }
    
    /**
     * Returns ClassLoader for the current module inside the project.<br>
     * If classLoader was set during the construction of the strategy - returns it.<br>
     * If no, creates {@link SimpleBundleClassLoader} with project classLoader of current module as parent. And
     * extends the classPath of it with all urls from current module project.
     * 
     * @return {@link ClassLoader} for the current module.
     */
    @SuppressWarnings("deprecation")
    protected ClassLoader getClassLoader() {
        if (classLoader == null) {
            ClassLoader parent = getModule().getProject().getClassLoader(false);            
            classLoader = new SimpleBundleClassLoader(parent);
            // Temporary decision. 
            // It is done to ensure that wrapper class will be loaded with current classloader.
            // Don`t need to load all urls from project, just wrapper class.
            URL[] urls = getModule().getProject().getClassPathUrls();          
            OpenLClassLoaderHelper.extendClasspath((SimpleBundleClassLoader)classLoader, urls);
        }
        return classLoader;        
    }
    
    @Override
    public Class<?> getServiceClass() throws ClassNotFoundException {
        // Service class for current implementation will be wrapper class, previously generated.
        
        Class<?> wrapperClass = null;
        
        // Ensure that service class (e.g. wrapper class for current strategy implementation) 
        // will be loaded by strategy classLoader.
        //
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClassLoader());
        try {
            wrapperClass = getWrapperClass();
            try {
                // Before returning wrapper class, need to compile the project to ensure
                // that all datatypes will be acessible from strategy classLoader.
                compile(wrapperClass, true);
            } catch (Exception e) {
                String errorMessage = String.format("Cannot compile %s module", getModule().getName());
                throw new OpenlNotCheckedException(errorMessage, e);
            } 
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
        return wrapperClass;
    }
    
    private Class<?> getWrapperClass() throws ClassNotFoundException {
        return Class.forName(getModule().getClassname(), false, getClassLoader());
    }
    
    public Object wrapperNewInstance(Class<?> wrapperClass) throws Exception {
        try {            
            // NOTICE: Our bean datatype classes are loaded during compilation and they may be used in wrapper 
            // method signatures.
            // However, wrapper fields and constructors do not have those classes references. 
            // Due to Java lazy class loading it's safe to access fields and constructor before Openl compilation.
            
            Constructor<?> ctr = findConstructor(wrapperClass, new Class[] { boolean.class, boolean.class, Map.class, 
                    IDependencyManager.class });
            
            if (ctr != null) {
                return ctr.newInstance(new Object[] { !isExecutionMode(), isExecutionMode(), getModule().getProperties(), 
                        getDependencyManager() });
            }

            ctr = findConstructor(wrapperClass, new Class[] {boolean.class, boolean.class});
            
            if (ctr != null) {
                return ctr.newInstance(new Object[] { !isExecutionMode(), isExecutionMode() });
            }

            ctr = wrapperClass.getConstructor(new Class[] {boolean.class});
            
            return ctr.newInstance(new Object[] { Boolean.TRUE });
        } catch (NoSuchMethodException e) {
            String errorMessage = 
                String.format("Cannot find method in wrapper class %s. " +
                		"You are using older version of OpenL Wrapper, please run Generate ... Wrapper", 
                    wrapperClass.getName());
            LOG.error(errorMessage, e);
            throw new OpenlNotCheckedException(errorMessage, e);
        }
    }
    
    @Override
    protected CompiledOpenClass compile(boolean useExisting) throws InstantiationException,
            IllegalAccessException {
        try {
            return compile(getWrapperClass(), useExisting);
        } catch (ClassNotFoundException e) {
            String errorMessage = String.format("Cannot find service class for %s", getModule().getClassname());
            LOG.error(errorMessage, e);
            throw new OpenlNotCheckedException(errorMessage, e);
        }
    }
    
    @Override
    protected OpenLWrapper instantiate(Class<?> wrapperClass, boolean useExisting) throws InstantiationException,
            IllegalAccessException {
        
        // Ensure that instantiation will be done in strategy classloader.
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClassLoader());
        
        try {
            preInitWrapper(wrapperClass);
            if (wrapper == null) {
                wrapper = (OpenLWrapper) wrapperNewInstance(wrapperClass);
            } else{
                if (!useExisting) {
                    reset(wrapperClass);
                    wrapper = (OpenLWrapper) wrapperNewInstance(wrapperClass);
                }
            }
            return wrapper;
        } catch (Exception e) {
            String errorMessage = String.format("Failed to instantiate wrapper %s", wrapperClass.getName());
            LOG.error(errorMessage, e);
            throw new OpenlNotCheckedException(errorMessage, e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    private void reset(Class<?> wrapperClass) throws NoSuchMethodException,
                                             IllegalAccessException,
                                             InvocationTargetException {
        // When calling getMethod() all declared methods from the class are loading with its parameters classes and 
        // return classes.
        Method m = wrapperClass.getMethod("reset", new Class[] {});
        m.invoke(null, new Object[] {}); // we reset to reload wrapper due
                                         // to its static implementation
    }

    /**
     * Compiles the wrapper class.
     * 
     * @param wrapperClass
     * @param useExisting
     * @return {@link CompiledOpenClass}
     * 
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    private CompiledOpenClass compile(Class<?> wrapperClass, boolean useExisting) throws InstantiationException,
            IllegalAccessException {
        OpenLWrapper wrapper = instantiate(wrapperClass, useExisting);
        return wrapper.getCompiledOpenClass();
    }    
    
    private Constructor<?> findConstructor(Class<?> clazz, Class<?>[] parameterTypes) {
        return ConstructorUtils.getMatchingAccessibleConstructor(clazz, parameterTypes);
    }

    private void preInitWrapper(Class<?> clazz) throws Exception {
        String projectFolder = getModule().getProject().getProjectFolder().getAbsolutePath();
        Field userHomeField = clazz.getField("__userHome");
        if (Modifier.isStatic(userHomeField.getModifiers())) {
            userHomeField.set(null, projectFolder);
        } else {
            String errorMessage = String.format("Field %s is not static in %s", userHomeField.getName(), 
                userHomeField.getDeclaringClass().getName());
            LOG.error(errorMessage);
            throw new OpenlNotCheckedException(errorMessage);
        }
        try {
            Field field = clazz.getField("__src");
            String sourcePath = (String) field.get(null);
            if (!new File(sourcePath).isAbsolute()) {
                field.set(null, projectFolder + '/' + sourcePath);
            }
        } catch (Exception e) {
            String errorMessage = "Failed to set up __src";
            LOG.error(errorMessage, e);
            throw new OpenlNotCheckedException(errorMessage, e);
        }
    }
    
}
