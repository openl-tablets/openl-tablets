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

    /**
     * Define and load a class from the bytecode.
     * @param className the class name
     * @param bytes the byte code of the class
     * @param loader OpenLClassLoader instance
     * @return initialized class
     * @throws ClassNotFoundException
     */
    public static Class<?> defineClass(String className, byte[] bytes, ClassLoader loader) throws ClassNotFoundException {
        var openLClassLoader = loader instanceof OpenLClassLoader ? (OpenLClassLoader) loader : new OpenLClassLoader(loader);
        openLClassLoader.addGeneratedClass(className, bytes);
        return Class.forName(className, true, openLClassLoader); // Force static initializers to run.
    }
}
