package org.openl.classloader;

/**
 * @author nsamatov.
 */
public interface ClassLoaderCloser {
    public void close(ClassLoader classLoader);
}
