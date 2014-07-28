package org.openl.rules.project.instantiation;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Map;

import org.apache.commons.lang3.reflect.ConstructorUtils;
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
public class WrapperAdjustingInstantiationStrategy extends SingleModuleInstantiationStrategy {

    private final Log log = LogFactory.getLog(WrapperAdjustingInstantiationStrategy.class);

    private OpenLWrapper wrapper;

    public WrapperAdjustingInstantiationStrategy(Module module, boolean executionMode,
            IDependencyManager dependencyManager) {
        super(module, executionMode, dependencyManager);
    }

    public WrapperAdjustingInstantiationStrategy(Module module, boolean executionMode,
            IDependencyManager dependencyManager, ClassLoader classLoader) {
        super(module, executionMode, dependencyManager, classLoader);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected ClassLoader initClassLoader() {
        ClassLoader parent = getModule().getProject().getClassLoader(false);
        SimpleBundleClassLoader classLoader = new SimpleBundleClassLoader(parent);
        // Temporary decision.
        // It is done to ensure that wrapper class will be loaded with current
        // classloader.
        // Don`t need to load all urls from project, just wrapper class.
        URL[] urls = getModule().getProject().getClassPathUrls();
        OpenLClassLoaderHelper.extendClasspath(classLoader, urls);
        return classLoader;
    }

    @Override
    public void reset() {
        super.reset();
        try {
            if (isWrapperClassLoaded()) {
                reset(getServiceClass());
            }
            wrapper = null;
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error(String.format("Failed to reset wrapper '%s'.", getModule().getClassname()), e);
            }
        }
    }

    protected boolean isWrapperClassLoaded() {
        return super.isServiceClassDefined();
    }

    @Override
    public void forcedReset() {
        super.forcedReset();
        super.setServiceClass(null);// it will cause reloading of service class
                                    // with
        // new classloader later
    }

    @Override
    public Class<?> getServiceClass() throws ClassNotFoundException {
        if (!isWrapperClassLoaded()) {
            // Service class for current implementation will be wrapper class,
            // previously generated.

            Class<?> wrapperClass = null;

            // Ensure that service class (e.g. wrapper class for current
            // strategy implementation)
            // will be loaded by strategy classLoader.
            //
            ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(getClassLoader());
            try {
                wrapperClass = getWrapperClass();
                try {
                    // Before returning wrapper class, need to compile the
                    // project to ensure
                    // that all datatypes will be acessible from strategy
                    // classLoader.
                    compile(wrapperClass);
                } catch (Exception e) {
                    String errorMessage = String.format("Cannot compile %s module", getModule().getName());
                    throw new OpenlNotCheckedException(errorMessage, e);
                }
            } finally {
                Thread.currentThread().setContextClassLoader(oldClassLoader);
            }
            super.setServiceClass(wrapperClass);
        }
        return super.getServiceClass();
    }

    @Override
    public void setServiceClass(Class<?> serviceClass) {
        if (log.isWarnEnabled()) {
            log.warn(String
                    .format("Service class changing is not allowed for static wrapper. Defauld static wrapper class will be used insdead of '%s'",
                            serviceClass.getName()));
        }
    }

    private Class<?> getWrapperClass() throws ClassNotFoundException {
        return Class.forName(getModule().getClassname(), false, getClassLoader());
    }

    public Object wrapperNewInstance(Class<?> wrapperClass) throws Exception {
        try {
            // NOTICE: Our bean datatype classes are loaded during compilation
            // and they may be used in wrapper
            // method signatures.
            // However, wrapper fields and constructors do not have those
            // classes references.
            // Due to Java lazy class loading it's safe to access fields and
            // constructor before Openl compilation.

            Constructor<?> ctr = findConstructor(wrapperClass, new Class[] { boolean.class, boolean.class, Map.class,
                    IDependencyManager.class });

            if (ctr != null) {
                return ctr.newInstance(!isExecutionMode(), isExecutionMode(),
                        prepareExternalParameters(), getDependencyManager());
            }

            ctr = findConstructor(wrapperClass, new Class[] { boolean.class, boolean.class });

            if (ctr != null) {
                return ctr.newInstance(!isExecutionMode(), isExecutionMode());
            }

            ctr = wrapperClass.getConstructor(new Class[] { boolean.class });

            return ctr.newInstance(Boolean.TRUE);
        } catch (NoSuchMethodException e) {
            String errorMessage = String.format("Cannot find method in wrapper class %s. "
                    + "You are using older version of OpenL Wrapper, please run Generate ... Wrapper",
                    wrapperClass.getName());
            if (log.isErrorEnabled()) {
                log.error(errorMessage, e);
            }
            throw new OpenlNotCheckedException(errorMessage, e);
        }
    }

    @Override
    public CompiledOpenClass compile() throws RulesInstantiationException {
        try {
            return compile(getServiceClass());
        } catch (ClassNotFoundException e) {
            String errorMessage = String.format("Cannot find service class for %s", getModule().getClassname());
            if (log.isErrorEnabled()) {
                log.error(errorMessage, e);
            }
            throw new RulesInstantiationException(errorMessage, e);
        } catch (UnsupportedClassVersionError e) {
            String errorMessage = String.format(
                    "Can't load a class compiled using newer version of JDK than current JRE (%s)",
                    System.getProperty("java.version"));
            if (log.isErrorEnabled()) {
                log.error(errorMessage, e);
            }
            throw new RulesInstantiationException(errorMessage, e);
        }
    }

    @Override
    public OpenLWrapper instantiate(Class<?> wrapperClass) throws RulesInstantiationException {

        // Ensure that instantiation will be done in strategy classloader.
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClassLoader());

        try {
            preInitWrapper(wrapperClass);
            if (wrapper == null) {
                wrapper = (OpenLWrapper) wrapperNewInstance(wrapperClass);
            }
            return wrapper;
        } catch (Exception e) {
            String errorMessage = String.format("Failed to instantiate wrapper %s", wrapperClass.getName());
            if (log.isErrorEnabled()) {
                log.error(errorMessage, e);
            }
            throw new RulesInstantiationException(errorMessage, e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    private void reset(Class<?> wrapperClass) throws NoSuchMethodException, IllegalAccessException,
            InvocationTargetException {
        // When calling getMethod() all declared methods from the class are
        // loading with its parameters classes and
        // return classes.
        Method m = wrapperClass.getMethod("reset", new Class[] {});
        m.invoke(null); // we reset to reload wrapper due
                                         // to its static implementation
    }

    /**
     * Compiles the wrapper class.
     * 
     * @return {@link CompiledOpenClass}
     * @throws RulesInstantiationException
     * 
     */
    private CompiledOpenClass compile(Class<?> wrapperClass) throws RulesInstantiationException {
        OpenLWrapper wrapper = instantiate(wrapperClass);

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
            String errorMessage = String.format("Field %s is not static in %s", userHomeField.getName(), userHomeField
                    .getDeclaringClass().getName());
            if (log.isErrorEnabled()) {
                log.error(errorMessage);
            }
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
            if (log.isErrorEnabled()) {
                log.error(errorMessage, e);
            }
            throw new OpenlNotCheckedException(errorMessage, e);
        }
    }

    @Override
    public boolean isServiceClassDefined() {
        return true;
    }
}
