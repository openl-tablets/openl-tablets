package org.openl.rules.project.instantiation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.CompiledOpenClass;
import org.openl.main.OpenLWrapper;
import org.openl.rules.project.model.Module;

/**
 * Instantiation strategy for projects with Wrapper
 * 
 * @author PUdalau
 */
public class WrapperAdjustingInstantiationStrategy extends RulesInstantiationStrategy {
    private static final Log LOG = LogFactory.getLog(RulesInstantiationStrategyFactory.class);

    public WrapperAdjustingInstantiationStrategy(Module module) {
        super(module);
    }

    @Override
    protected CompiledOpenClass compile(Class<?> clazz, boolean useExisting) throws InstantiationException,
            IllegalAccessException {
        OpenLWrapper wrapper = instantiate(clazz, useExisting);
        return wrapper.getCompiledOpenClass();
    }

    public static Object wrapperNewInstance(Class<?> c) throws Exception {
        Constructor<?> ctr;
        try {
            ctr = c.getConstructor(new Class[] { boolean.class });
            return ctr.newInstance(new Object[] { Boolean.TRUE });
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Using older version of OpenL Wrapper, please run Generate ... Wrapper");
        }
    }

    private void preInitWrapper(Class<?> clazz) throws Exception {
        Field userHomeField = clazz.getField("__userHome");
        if (Modifier.isStatic(userHomeField.getModifiers())) {
            String projectFolder = getModule().getProject().getProjectFolder().getAbsolutePath();
            userHomeField.set(null, projectFolder);
        } else {
            throw new RuntimeException("Field " + userHomeField.getName() + " is not static in "
                    + userHomeField.getDeclaringClass().getName());
        }
    }

    @Override
    protected OpenLWrapper instantiate(Class<?> clazz, boolean useExisting) throws InstantiationException,
            IllegalAccessException {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(clazz.getClassLoader());
        try {
            preInitWrapper(clazz);
            OpenLWrapper wrapper = (OpenLWrapper) wrapperNewInstance(clazz);
            if (!useExisting) {
                wrapper.reload();
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
