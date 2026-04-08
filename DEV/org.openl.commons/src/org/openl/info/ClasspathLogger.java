package org.openl.info;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import lombok.extern.slf4j.Slf4j;

import org.openl.util.ClassUtils;

@Slf4j
final class ClasspathLogger extends OpenLLogger {


    @Override
    protected String getName() {
        return "cp";
    }

    @Override
    protected void discover() {
        log("Libs in the classpath:");
        var classLoader = ClassUtils.getCurrentClassLoader(getClass());
        while (classLoader != null) {
            log(getClassLoaderName(classLoader));
            if (classLoader instanceof URLClassLoader loader) {
                URL[] urls = loader.getURLs();
                for (URL url : urls) {
                    log("  {}", url);
                }
            }
            classLoader = classLoader.getParent();
        }
    }

    private static String getClassLoaderName(ClassLoader classLoader) {
        Class<?> clazz = classLoader.getClass();
        String name = clazz.getName();
        try {
            Class<?> cls = clazz.getMethod("toString").getDeclaringClass();
            if (cls != Object.class) {
                name = classLoader.toString() + "  Class: " + name + " #" + System.identityHashCode(classLoader);
            }
        } catch (NoSuchMethodException e) {
            // Ignore
        }

        try {
            Method getName = clazz.getMethod("getName");
            Object getNameStr = getName.invoke(classLoader);
            if (getNameStr != null) {
                name += "  Name: " + getNameStr.toString();
            }
        } catch (Exception e) {
            log.debug("Ignored error: ", e);
            // Ignore
        }
        return name;
    }
}
