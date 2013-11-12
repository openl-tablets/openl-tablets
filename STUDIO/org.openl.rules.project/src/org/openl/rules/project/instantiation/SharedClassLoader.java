package org.openl.rules.project.instantiation;

import java.util.ArrayList;
import java.util.List;

public class SharedClassLoader extends ClassLoader {

    private List<ClassLoader> classLoaders = new ArrayList<ClassLoader>();

    public SharedClassLoader(ClassLoader parent) {
        super(parent);
    }
    
    public void addClassLoader(ClassLoader classLoader) {
        classLoaders.add(classLoader);
    }

    public void removeClassLoader(ClassLoader classLoader) {
        classLoaders.remove(classLoader);
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        
        for (ClassLoader loader : classLoaders) {
            try {
                Class<?> loadedClass = loader.loadClass(name);
                if (loadedClass != null) {
                    return loadedClass;
                }
            } catch (ClassNotFoundException ex) {
            }
        }

        return super.loadClass(name, resolve);
    }

}
