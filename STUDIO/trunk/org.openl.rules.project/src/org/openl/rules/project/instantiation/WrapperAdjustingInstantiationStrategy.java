package org.openl.rules.project.instantiation;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

import org.apache.commons.lang.reflect.ConstructorUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.CompiledOpenClass;
import org.openl.dependency.IDependencyManager;
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

    public WrapperAdjustingInstantiationStrategy(Module module, boolean executionMode, IDependencyManager dependencyManager) {
        super(module, executionMode, dependencyManager);
    }
    
    public WrapperAdjustingInstantiationStrategy(Module module, boolean executionMode, IDependencyManager dependencyManager, ClassLoader classLoader) {
        super(module, executionMode, dependencyManager, classLoader);
    }


    @Override
    protected CompiledOpenClass compile(Class<?> clazz, boolean useExisting) throws InstantiationException,
            IllegalAccessException {
        OpenLWrapper wrapper = instantiate(clazz, useExisting);
        return wrapper.getCompiledOpenClass();
    }

    public Object wrapperNewInstance(Class<?> c) throws Exception {
        try {
            Method m = c.getMethod("reset", new Class[] {});
            m.invoke(null, new Object[] {}); // we reset to reload wrapper due
                                             // to its static implementation
            Constructor<?> ctr = findConstructor(c, new Class[] { boolean.class, boolean.class, Map.class, IDependencyManager.class });
            
            if (ctr != null) {
                return ctr.newInstance(new Object[] { !isExecutionMode(), isExecutionMode(), getModule().getProperties(), getDependencyManager() });
            }

            ctr = c.getConstructor(new Class[] {boolean.class, boolean.class});

            return ctr.newInstance(new Object[] { !isExecutionMode(), isExecutionMode()});
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Using older version of OpenL Wrapper, please run Generate ... Wrapper");
        }
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
            throw new RuntimeException("Field " + userHomeField.getName() + " is not static in "
                    + userHomeField.getDeclaringClass().getName());
        }
        try {
            Field field = clazz.getField("__src");
            String sourcePath = (String) field.get(null);
            if (!new File(sourcePath).isAbsolute()) {
                field.set(null, projectFolder + '/' + sourcePath);
            }
        } catch (Exception e) {
            throw new RuntimeException("failed to set up __src", e);
        }

    }

    @Override
    protected OpenLWrapper instantiate(Class<?> clazz, boolean useExisting) throws InstantiationException,
            IllegalAccessException {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(clazz.getClassLoader());
        try {
            preInitWrapper(clazz);
            if (!useExisting || wrapper == null) {
                wrapper = (OpenLWrapper) wrapperNewInstance(clazz);
            }
            return wrapper;
        } catch (Exception e) {
            LOG.error("Failed to instantiate wrapper \"" + clazz.getName() + "\"", e);
            throw new RuntimeException("Failed to instantiate wrapper \"" + clazz.getName() + "\"", e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }
}
