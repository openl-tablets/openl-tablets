package org.openl.classloader;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

import groovy.lang.GroovyResourceLoader;

/**
 * Looks up Groovy sources, except for the JAX-RS request beans, which are generated at runtime and have none.
 */
class JaxrsBeanSkippingResourceLoader implements GroovyResourceLoader {
    private final GroovyResourceLoader delegate;

    JaxrsBeanSkippingResourceLoader(GroovyResourceLoader delegate) {
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
