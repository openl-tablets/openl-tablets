package org.openl.rules.maven;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * Guards the leak prevention {@link AppServer} relies on.
 *
 * <p>Groovy keeps per-class metadata in a cache that is backed by {@link ClassValue} by default. An entry
 * of such a cache is reachable from its class, so a class that lives for the whole JVM - {@code String},
 * {@code Object} - pins the classloader the cache came from. The verification web application is then
 * never released (GROOVY-7591). {@code AppServer} avoids that by setting the {@code groovy.use.classvalue}
 * system property to {@code false} before the server starts.
 *
 * <p>Groovy reads the property once, in a static initializer, so every case here loads Groovy in a
 * classloader of its own. Groovy 5 dropped the property and always cached through {@link ClassValue},
 * which is why this check exists: an upgrade must not bring the leak back unnoticed.
 */
class GroovyClassValueLeakTest {

    private static final String USE_CLASS_VALUE = "groovy.use.classvalue";

    /**
     * Asserts that the property {@code AppServer} sets still keeps {@link ClassValue} out of the cache.
     */
    @Test
    void propertySetByAppServerKeepsClassValueOutOfTheCache() throws Exception {
        assertFalse(cachesThroughClassValue("false"),
                USE_CLASS_VALUE + "=false no longer moves Groovy off ClassValue, so AppServer leaks its classloader");
    }

    /**
     * Asserts that the property is what makes the difference, not a Groovy default that already avoids
     * {@link ClassValue}. Without it the cache is expected to leak.
     */
    @Test
    void cacheLeaksThroughClassValueWithoutTheProperty() throws Exception {
        assertTrue(cachesThroughClassValue(null),
                "Groovy no longer caches through ClassValue on its own, so this test proves nothing anymore");
    }

    /**
     * Builds Groovy's class-metadata cache under the given property value and reports whether it holds a
     * {@link ClassValue}.
     *
     * @param useClassValue value for {@code groovy.use.classvalue}, or {@code null} to leave it unset
     */
    private static boolean cachesThroughClassValue(@Nullable String useClassValue) throws Exception {
        var restore = System.getProperty(USE_CLASS_VALUE);
        if (useClassValue == null) {
            System.clearProperty(USE_CLASS_VALUE);
        } else {
            System.setProperty(USE_CLASS_VALUE, useClassValue);
        }
        try (var groovy = new URLClassLoader(new URL[]{groovyLocation()}, ClassLoader.getPlatformClassLoader())) {
            return holdsClassValue(createCache(groovy));
        } finally {
            if (restore == null) {
                System.clearProperty(USE_CLASS_VALUE);
            } else {
                System.setProperty(USE_CLASS_VALUE, restore);
            }
        }
    }

    /**
     * Locates the Groovy artifact on the test classpath so that it can be loaded again in isolation.
     */
    private static URL groovyLocation() throws ClassNotFoundException {
        return Class.forName("org.codehaus.groovy.reflection.GroovyClassValueFactory")
                .getProtectionDomain()
                .getCodeSource()
                .getLocation();
    }

    /**
     * Asks the given Groovy for a class-metadata cache, the way Groovy asks for it itself.
     */
    private static Object createCache(ClassLoader groovy) throws Exception {
        var computeValueType = groovy.loadClass("org.codehaus.groovy.reflection.GroovyClassValue$ComputeValue");
        var computeValue = Proxy
                .newProxyInstance(groovy, new Class<?>[]{computeValueType}, (proxy, method, args) -> null);
        var factory = groovy.loadClass("org.codehaus.groovy.reflection.GroovyClassValueFactory");
        var createGroovyClassValue = factory.getDeclaredMethod("createGroovyClassValue", computeValueType);
        createGroovyClassValue.setAccessible(true);
        return createGroovyClassValue.invoke(null, computeValue);
    }

    /**
     * Tells whether the cache is a {@link ClassValue} or keeps one, both of which leak the classloader.
     */
    private static boolean holdsClassValue(Object cache) throws IllegalAccessException {
        if (cache instanceof ClassValue) {
            return true;
        }
        for (var field : cache.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            if (field.get(cache) instanceof ClassValue) {
                return true;
            }
        }
        return false;
    }
}
