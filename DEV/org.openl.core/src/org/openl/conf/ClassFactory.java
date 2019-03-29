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
            Log.error("Can't load the class \"{0}\" compiled using newer version of JDK than current JRE ({1})",
                e,
                name,
                System.getProperty("java.version"));
            throw RuntimeExceptionWrapper.wrap(e);
        } catch (Throwable t) {
            Log.error("Can't load class: " + name, t);
            throw RuntimeExceptionWrapper.wrap(t);
        }
    }

    public static Object newInstance(Class<?> cc, String uri) throws OpenConfigurationException {
        try {
            return cc.newInstance();
        } catch (Throwable t) {
            throw new OpenConfigurationException("Can't create a new " + cc.getName(), uri, t);
        }
    }

    public static Object newInstance(String classname, IConfigurableResourceContext cxt, String uri) {
        try {
            return cxt.getClassLoader().loadClass(classname).newInstance();
        } catch (Throwable t) {
            throw new OpenConfigurationException("Can't create a new " + classname, uri, t);
        }
    }

    public static Class<?> validateClassExistsAndPublic(String className, ClassLoader cl, String uri) {
        Class<?> c;
        try {
            c = cl.loadClass(className);
        } catch (Throwable t) {
            throw new OpenConfigurationException("Can't load class: " + className, uri, t);
        }

        if (!Modifier.isPublic(c.getModifiers())) {
            throw new OpenConfigurationException(c.getName() + " must be public ", uri, null);
        }

        return c;

    }

    public static Constructor<?> validateHasConstructor(Class<?> clazz, Class<?>[] params, String uri) {
        Constructor<?> c;
        try {
            c = clazz.getConstructor(params);

        } catch (Throwable t) {
            String methodString = MethodUtil.printMethod("", params);
            throw new OpenConfigurationException(
                "Class " + clazz.getName() + " does not have a constructor " + methodString,
                uri,
                t);
        }

        if (!Modifier.isPublic(c.getModifiers())) {
            throw new OpenConfigurationException(
                "Constructor " + clazz.getName() + MethodUtil.printMethod("", params) + " must be public ",
                uri,
                null);
        }
        return c;
    }

    public static Method validateHasMethod(Class<?> clazz, String methodName, Class<?>[] params, String uri) {
        Method m;
        try {
            m = clazz.getMethod(methodName, params);
        } catch (Throwable t) {
            String methodString = MethodUtil.printMethod(methodName, params);
            throw new OpenConfigurationException("Class " + clazz.getName() + " does not have a method " + methodString,
                uri,
                t);
        }

        if (!Modifier.isPublic(m.getModifiers())) {
            throw new OpenConfigurationException(methodName + " must be public ", uri, null);
        }
        return m;
    }

    public static void validateHaveNewInstance(Class<?> clazz, String uri) throws OpenConfigurationException {
        if (Modifier.isAbstract(clazz.getModifiers())) {
            throw new OpenConfigurationException(clazz.getName() + " must not be abstract ", uri, null);
        }

        try {
            Constructor<?> constr = clazz.getConstructor(NO_PARAMS);
            if (!Modifier.isPublic(constr.getModifiers())) {
                throw new OpenConfigurationException("Default constructor of " + clazz.getName() + " must be public",
                    uri,
                    null);
            }
        } catch (OpenConfigurationException ex) {
            throw ex;
        } catch (Throwable t) {
            throw new OpenConfigurationException(clazz.getName() + " must have a default constructor", uri, null);
        }
    }

    public static void validateSuper(Class<?> clazz,
            Class<?> superClazz,
            String uri) throws OpenConfigurationException {
        if (!superClazz.isAssignableFrom(clazz)) {
            String verb = superClazz.isInterface() ? "implement" : "extend";
            throw new OpenConfigurationException(clazz.getName() + " does not " + verb + " " + superClazz.getName(),
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

    public synchronized Object getResource(IConfigurableResourceContext cxt) throws OpenConfigurationException {
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
    protected Object getResourceInternal(IConfigurableResourceContext cxt) throws OpenConfigurationException {
        try {
            return cxt.getClassLoader().loadClass(className).newInstance();
        } catch (Throwable t) {
            throw new OpenConfigurationException("Error creating " + className, getUri(), t);
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
    public void validate(IConfigurableResourceContext cxt) throws OpenConfigurationException {
        Class<?> c = validateClassExistsAndPublic(className, cxt.getClassLoader(), getUri());

        if (getExtendsClassName() != null) {
            Class<?> c2 = validateClassExistsAndPublic(getExtendsClassName(), cxt.getClassLoader(), getUri());

            validateSuper(c, c2, getUri());
        }

        validateHaveNewInstance(c, getUri());
    }

}
