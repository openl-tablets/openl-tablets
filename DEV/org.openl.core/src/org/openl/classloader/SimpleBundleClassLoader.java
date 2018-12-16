package org.openl.classloader;

import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * ClassLoader that have bundle classLoaders. When loading any class, at first
 * tries to find it in bundle classLoaders if can`t tries to find it in his
 * parent.
 * 
 * 
 */
public class SimpleBundleClassLoader extends OpenLBundleClassLoader {

    public SimpleBundleClassLoader(ClassLoader parent) {
        this(new URL[0], parent);
    }

    public SimpleBundleClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        Set<ClassLoader> c = Collections.newSetFromMap(new IdentityHashMap<ClassLoader, Boolean>());
        c.add(this);
        return loadClass(name, c);
    }

    protected Class<?> loadClass(String name, Set<ClassLoader> c) throws ClassNotFoundException {
        Class<?> clazz = findClassInBundles(name, c);

        if (clazz != null) {
            return clazz;
        }

        return super.loadClass(name);
    }

    /**
     * Searches for class in bundle classLoaders.
     */
    protected Class<?> findClassInBundles(String name) {
        Set<ClassLoader> c = Collections.newSetFromMap(new IdentityHashMap<ClassLoader, Boolean>());
        return findClassInBundles(name, c);
    }

    protected Class<?> findClassInBundles(String name, Set<ClassLoader> c) {

        for (ClassLoader bundleClassLoader : bundleClassLoaders) {
            if (c.contains(bundleClassLoader)) {
                continue;
            }
            c.add(bundleClassLoader);
            try {
                // if current class loader contains appropriate class - it will
                // be returned as a result
                //
                Class<?> clazz = null;
                if (bundleClassLoader instanceof SimpleBundleClassLoader && bundleClassLoader.getParent() == this) {
                    SimpleBundleClassLoader sbc = ((SimpleBundleClassLoader) bundleClassLoader);
                    clazz = sbc.findLoadedClassInBundle(name);
                    if (clazz == null) {
                        clazz = sbc.findClassInBundles(name, c);
                    }
                } else {
                    if (bundleClassLoader instanceof SimpleBundleClassLoader) {
                        clazz = ((SimpleBundleClassLoader) bundleClassLoader).loadClass(name, c);
                    } else {
                        clazz = bundleClassLoader.loadClass(name);
                    }
                }
                // Class<?> clazz = findLoadedClass(name);
                if (clazz != null) {
                    return clazz;
                }
            } catch (ClassNotFoundException e) {
            }
        }

        return null;
    }

    @Override
    public URL getResource(String name) {
        for (ClassLoader bundleClassLoader : bundleClassLoaders) {
            URL url = bundleClassLoader.getResource(name);
            if (url != null) {
                return url;
            }
        }
        return super.getResource(name);
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        for (ClassLoader bundleClassLoader : bundleClassLoaders) {
            InputStream inputStream = bundleClassLoader.getResourceAsStream(name);
            if (inputStream != null) {
                return inputStream;
            }
        }
        return super.getResourceAsStream(name);
    }

    protected Class<?> findLoadedClassInBundle(String name) {
        return findLoadedClass(name);
    }

}
