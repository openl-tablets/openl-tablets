/*
 * Created on Jun 10, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.conf;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.openl.binding.MethodUtil;
import org.openl.util.Log;
import org.openl.util.RuntimeExceptionWrapper;

/**
 * @author snshor
 *
 */
public class ClassFactory extends AConfigurationElement {

    static final Class<?>[] NO_PARAMS = {};
    protected String className;
    protected String extendsClassName;

    protected boolean singleton;

    Object cachedObject = null;

    public static Class<?> forName(String name, ClassLoader cl) {
        try {
            return cl.loadClass(name);
        } catch (ClassNotFoundException ex) {
            throw RuntimeExceptionWrapper.wrap(ex);
        } catch (NoClassDefFoundError ex) {
            Log.debug("Potential problem loading class: {0}", ex, name);
            throw RuntimeExceptionWrapper.wrap(ex);
        } catch (UnsupportedClassVersionError e) {
            Log.error("Cannot load class '{0}' compiled using newer version of JDK than current JRE ({1})",
                e,
                name,
                System.getProperty("java.version"));
            throw RuntimeExceptionWrapper.wrap(e);
        } catch (Throwable t) {
            Log.error(String.format("Failed to load class '%s'.", name), t);
            throw RuntimeExceptionWrapper.wrap(t);
        }
    }

    public static Object newInstance(Class<?> cc, String uri) {
        try {
            return cc.newInstance();
        } catch (Throwable t) {
            throw new OpenLConfigurationException(String.format("Failed to instantiate class '%s'.", cc.getTypeName()),
                uri,
                t);
        }
    }

    public static Object newInstance(String classname, IConfigurableResourceContext cxt, String uri) {
        try {
            return cxt.getClassLoader().loadClass(classname).newInstance();
        } catch (Throwable t) {
            throw new OpenLConfigurationException(String.format("Failed to instantiate class '%s'.", classname),
                uri,
                t);
        }
    }

    public static Class<?> validateClassExistsAndPublic(String className, ClassLoader cl, String uri) {
        Class<?> c;
        try {
            c = cl.loadClass(className);
        } catch (Throwable t) {
            throw new OpenLConfigurationException(String.format("Failed to load class '%s'.", className), uri, t);
        }

        if (!Modifier.isPublic(c.getModifiers())) {
            throw new OpenLConfigurationException(String.format("Class '%s' must be a public.", c.getTypeName()),
                uri,
                null);
        }

        return c;

    }

    public static Constructor<?> validateHasConstructor(Class<?> clazz, Class<?>[] params, String uri) {
        Constructor<?> c;
        try {
            c = clazz.getConstructor(params);
        } catch (Throwable t) {
            String methodString = MethodUtil.printMethod("", params);
            throw new OpenLConfigurationException(String
                .format("Constructor '%s' is not found in class '%s'.", methodString, clazz.getTypeName()), uri, t);
        }

        if (!Modifier.isPublic(c.getModifiers())) {
            throw new OpenLConfigurationException(String.format("Constructor '%s.%s' is not a public.",
                clazz.getTypeName(),
                c.getName() + MethodUtil.printMethod("", params)), uri, null);
        }
        return c;
    }

    public static Method validateHasMethod(Class<?> clazz, String methodName, Class<?>[] params, String uri) {
        Method m;
        try {
            m = clazz.getMethod(methodName, params);
        } catch (Throwable t) {
            String methodString = MethodUtil.printMethod(methodName, params);
            throw new OpenLConfigurationException(String
                .format("Method '%s' is not found in class '%s'.", methodString, clazz.getTypeName()), uri, t);
        }

        if (!Modifier.isPublic(m.getModifiers())) {
            throw new OpenLConfigurationException(String.format("Method '%s' is not a public.", methodName), uri, null);
        }
        return m;
    }

    public static void validateHaveNewInstance(Class<?> clazz, String uri) {
        if (Modifier.isAbstract(clazz.getModifiers())) {
            throw new OpenLConfigurationException(String.format("Expected non abstract class '%s'.",
                clazz.getTypeName()), uri, null);
        }

        try {
            Constructor<?> constr = clazz.getConstructor(NO_PARAMS);
            if (!Modifier.isPublic(constr.getModifiers())) {
                throw new OpenLConfigurationException(String.format("Default constructor is not public in class '%s'.",
                    clazz.getTypeName()), uri, null);
            }
        } catch (OpenLConfigurationException ex) {
            throw ex;
        } catch (Throwable t) {
            throw new OpenLConfigurationException(String.format("Default constructor is not found in class '%s'.",
                clazz.getTypeName()), uri, null);
        }
    }

    public static void validateSuper(Class<?> clazz, Class<?> superClazz, String uri) {
        if (!superClazz.isAssignableFrom(clazz)) {
            String verb = superClazz.isInterface() ? "implement" : "extend";
            throw new OpenLConfigurationException(
                String.format("Class '%s' does not %s '%s'.", clazz.getTypeName(), verb, superClazz.getTypeName()),
                uri,
                null);
        }

    }

    /**
     * @return
     */
    public String getClassName() {
        return className;
    }

    /**
     * @return
     */
    public String getExtendsClassName() {
        return extendsClassName;
    }

    public synchronized Object getResource(IConfigurableResourceContext cxt) {
        if (isSingleton()) {
            if (cachedObject == null) {
                cachedObject = getResourceInternal(cxt);
            }
            return cachedObject;
        }

        return getResourceInternal(cxt);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.newconf.IConfigurationElement#getResource(org.openl.newconf.IConfigurationContext)
     */
    protected Object getResourceInternal(IConfigurableResourceContext cxt) {
        try {
            return cxt.getClassLoader().loadClass(className).newInstance();
        } catch (Throwable t) {
            throw new OpenLConfigurationException(String.format("Failed to instantiate class '%s'.", className),
                getUri(),
                t);
        }
    }

    /**
     * @return
     */
    public boolean isSingleton() {
        return singleton;
    }

    /**
     * @param string
     */
    public void setClassName(String string) {
        className = string;
    }

    /**
     * @param string
     */
    public void setExtendsClassName(String string) {
        extendsClassName = string;
    }

    /**
     * @param b
     */
    public void setSingleton(boolean b) {
        singleton = b;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.newconf.IConfigurationElement#validate(org.openl.newconf.IConfigurationContext)
     */
    @Override
    public void validate(IConfigurableResourceContext cxt) {
        Class<?> c = validateClassExistsAndPublic(className, cxt.getClassLoader(), getUri());

        if (getExtendsClassName() != null) {
            Class<?> c2 = validateClassExistsAndPublic(getExtendsClassName(), cxt.getClassLoader(), getUri());

            validateSuper(c, c2, getUri());
        }

        validateHaveNewInstance(c, getUri());
    }

}