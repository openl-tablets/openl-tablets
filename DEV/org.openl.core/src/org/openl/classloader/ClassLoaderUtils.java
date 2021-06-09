package org.openl.classloader;

import java.io.Closeable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yury Molchan.
 */
public final class ClassLoaderUtils {
    private static final Logger LOG = LoggerFactory.getLogger(ClassLoaderUtils.class);

    private ClassLoaderUtils() {
    }

    /**
     * Closes a {@link ClassLoader} which implements {@link Closeable} interface.
     */
    public static void close(ClassLoader classLoader) {
        if (classLoader instanceof Closeable) {
            try {
                ((Closeable) classLoader).close();
            } catch (Exception e) {
                LOG.error("Failed on close ClassLoader '{}'", classLoader, e);
            }
        } else {
            LOG.warn("Not possible to close ClassLoader '{}', because it does not implement Closeable interface.",
                classLoader);
        }
    }
}
