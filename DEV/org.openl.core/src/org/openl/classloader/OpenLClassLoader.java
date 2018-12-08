package org.openl.classloader;

import java.net.URL;
import java.net.URLClassLoader;

public class OpenLClassLoader extends URLClassLoader {

    public OpenLClassLoader() {
        this(new URL[0]);
    }
    
    public OpenLClassLoader(URL[] urls) {
        super(urls);
    }

    public OpenLClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

}
