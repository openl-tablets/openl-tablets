package org.openl.classloader;

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
        super(parent);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        Class<?> clazz = findClassInBundles(name);

        if (clazz != null) {
            return clazz;
        }

        return super.loadClass(name);
    }

    /**
     * Searches for class in bundle classLoaders.
     */
    protected Class<?> findClassInBundles(String name) {

        Set<ClassLoader> bundleClassLoaders = getBundleClassLoaders();

        for (ClassLoader bundleClassLoader : bundleClassLoaders) {
            try {
                // if current class loader contains appropriate class - it will
                // be returned as a result
                //
                Class<?> clazz = null;
                if (bundleClassLoader instanceof SimpleBundleClassLoader && bundleClassLoader.getParent() == this) {
                    clazz = ((SimpleBundleClassLoader) bundleClassLoader).findLoadedClassInBundle(name);
                    if (clazz == null){
                        clazz = ((SimpleBundleClassLoader) bundleClassLoader).findClassInBundles(name);
                    }
                } else {
                    clazz = bundleClassLoader.loadClass(name);
                }
                // Class<?> clazz = findLoadedClass(name);
                if (clazz != null) {
                    return clazz;
                }
            } catch (ClassNotFoundException e) {
                // ignore exception
            } 
        }

        return null;
    }

    protected Class<?> findLoadedClassInBundle(String name) {
        return findLoadedClass(name);
    }

}
