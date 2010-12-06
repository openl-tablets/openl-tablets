package org.openl.classloader;

import java.util.Set;

public class SimpleBundleClassLoader extends OpenLBundleClassLoader {

    public SimpleBundleClassLoader() {
        super();
    }
    
    public SimpleBundleClassLoader(ClassLoader parent) {
        super(parent);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        Class<?> clazz = findClass(name);
        
        
        if (clazz != null) {
            return clazz;
        }
        
        return super.loadClass(name);
    }
    
    protected  Class<?> findClass(String name) {
        
        Set<ClassLoader> bundleClassLoaders = getBundleClassLoaders();
        
        for (ClassLoader bundleClassLoader : bundleClassLoaders) {
            try {
                // if current class loader contains appropriate class - it will
                // be returned as a result
                //
                Class<?> clazz = bundleClassLoader.loadClass(name);
                if (clazz != null) {
                    return clazz;
                }
            } catch (ClassNotFoundException e) {
                // ignore exception
            }
        }
        
        return null;
    }

}
