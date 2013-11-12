package org.openl.classloader;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

public class OpenLClassLoaderHelper {
    
    public static ClassLoader getContextClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    public static void extendClasspath(OpenLClassLoader classLoader, URL[] urls) {
        if (urls != null) {
            for (int i = 0; i < urls.length; i++) {
                classLoader.addURL(urls[i]);
            }
        }
    }

    public static void extendClasspath(URLClassLoader classLoader, URL[] urls) {

        try {
            Method addURL = URLClassLoader.class.getDeclaredMethod("addURL", new Class[] { URL.class });
            addURL.setAccessible(true);

            if (urls != null) {
                for (int i = 0; i < urls.length; i++) {
                    addURL.invoke(classLoader, new Object[] { urls[i] });
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
