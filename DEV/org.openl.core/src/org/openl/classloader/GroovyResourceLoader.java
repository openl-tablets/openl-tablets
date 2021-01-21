package org.openl.classloader;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

class GroovyResourceLoader implements groovy.lang.GroovyResourceLoader {
    groovy.lang.GroovyResourceLoader delegate;

    GroovyResourceLoader(groovy.lang.GroovyResourceLoader delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate cannot be null");
    }

    @Override
    public URL loadGroovySource(String filename) throws MalformedURLException {
        if (filename.startsWith("org.openl.jaxrs.")) {
            return null;
        }
        return delegate.loadGroovySource(filename);
    }
}
