package org.openl.dependency;

import java.net.URL;

public class OpenLDelegatingClassLoader extends OpenLClassLoader {

    private ClassLoader delegate;

    public OpenLDelegatingClassLoader(ClassLoader delegate) {
        super(new URL[0]);
        this.delegate = delegate;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {

        try {
            // try to find class using current class loader.
            return super.loadClass(name);
        } catch (ClassNotFoundException ex) {
        }
        
        return delegate.loadClass(name);
    }
    
}
