package org.openl.dependency;

import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;

public class OpenLClassLoader extends URLClassLoader{

    public OpenLClassLoader(URL[] urls) {
        super(urls);
    }

    public OpenLClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public OpenLClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        super(urls, parent, factory);
    }

    @Override
    public void addURL(URL url) {
        super.addURL(url);
    }

    
}
