package org.openl.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;

import org.openl.classloader.OpenLClassLoader;

class ASMProxyFactoryTest {

    public interface Echo {
        String echo(String value);
    }

    /**
     * Holds a nested type whose binary name is {@code ...Holder$proxy}, i.e. exactly what a naive
     * {@code interface + "$proxy"} scheme would produce for {@code Holder}. Used to prove the proxy name does not
     * clash with a real (nested) class.
     */
    public interface Holder {
        String echo(String value);

        interface proxy {
        }
    }

    private static OpenLClassLoader newClassLoader() {
        return new OpenLClassLoader(Echo.class.getClassLoader());
    }

    /**
     * The proxy class is fully defined by the proxied interfaces. Generating a uniquely named class on every call
     * pollutes the (long-lived) classloader's Metaspace, see issue #1230. The same interface must reuse a single
     * generated proxy class.
     */
    @Test
    void proxyClassIsReusedForSameInterface() {
        var classLoader = newClassLoader();
        var first = ASMProxyFactory.newProxyInstance(classLoader, (method, args) -> args[0], Echo.class);
        var second = ASMProxyFactory.newProxyInstance(classLoader, (method, args) -> args[0], Echo.class);

        assertNotSame(first, second, "Each call must return a distinct instance");
        assertSame(first.getClass(), second.getClass(), "The generated proxy class must be reused");
    }

    @Test
    void proxyInstancesUseTheirOwnHandler() {
        var classLoader = newClassLoader();
        var upper = ASMProxyFactory.newProxyInstance(classLoader,
                (method, args) -> ((String) args[0]).toUpperCase(),
                Echo.class);
        var lower = ASMProxyFactory.newProxyInstance(classLoader,
                (method, args) -> ((String) args[0]).toLowerCase(),
                Echo.class);

        assertEquals("ABC", upper.echo("aBc"));
        assertEquals("abc", lower.echo("aBc"));
    }

    @Test
    void proxyHandlerIsAccessible() {
        var classLoader = newClassLoader();
        ASMProxyHandler handler = (method, args) -> args[0];
        var proxy = ASMProxyFactory.newProxyInstance(classLoader, handler, Echo.class);

        assertSame(handler, ASMProxyFactory.getProxyHandler(proxy));
    }

    @Test
    void getProxyHandlerRejectsNonProxy() {
        assertThrows(IllegalArgumentException.class, () -> ASMProxyFactory.getProxyHandler("not a proxy"));
    }

    /**
     * The proxy name must not resolve to a real class that happens to share the naive {@code interface + "$proxy"}
     * name, otherwise {@code getProxyClass()} would load that class and fail to instantiate it.
     */
    @Test
    void proxyNameDoesNotCollideWithRealNestedClass() {
        var classLoader = newClassLoader();
        var proxy = ASMProxyFactory.newProxyInstance(classLoader, (method, args) -> args[0], Holder.class);

        assertTrue(ASMProxyFactory.isProxy(proxy), "A real proxy must be generated, not the nested Holder$proxy type");
        assertNotSame(Holder.proxy.class, proxy.getClass());
    }

    /**
     * The escape that encodes additional interfaces must be collision-free for the dot/dollar/underscore characters
     * that all legitimately appear in binary names, and must not leak {@code '.'}/{@code '$'} into the name segment.
     */
    @Test
    void escapeKeepsAmbiguousBinaryNamesDistinct() {
        assertNotEquals(ASMProxyFactory.escape("a.b"), ASMProxyFactory.escape("a_b"));
        assertNotEquals(ASMProxyFactory.escape("a.b"), ASMProxyFactory.escape("a$b"));
        assertNotEquals(ASMProxyFactory.escape("a_b"), ASMProxyFactory.escape("a$b"));

        var escaped = ASMProxyFactory.escape("a.b$c_d");
        assertFalse(escaped.contains("."), "escaped segment must stay inside the package");
        assertFalse(escaped.contains("$"), "escaped segment must be safe to delimit with '$'");
    }

    /**
     * Interface sets that the previous {@code replace('.', '_')} scheme mapped to the same name must now produce
     * distinct proxy class names.
     */
    @Test
    void proxyClassNameIsUniquePerInterfaceSet() {
        var withRunnable = ASMProxyFactory.proxyClassName(new Class<?>[]{Echo.class, Runnable.class});
        var withHolder = ASMProxyFactory.proxyClassName(new Class<?>[]{Echo.class, Holder.class});
        var single = ASMProxyFactory.proxyClassName(new Class<?>[]{Echo.class});

        assertNotEquals(withRunnable, withHolder);
        assertNotEquals(single, withRunnable);
    }

    /**
     * Concurrent first calls must not fail with a duplicate class definition and must all resolve to a single proxy
     * class.
     *
     * <p>The duplicate definition surfaced only as a rare race on the very first definition. The scenario therefore
     * runs many rounds, each on its own class loader, so the race has many chances to appear.
     */
    @Test
    void concurrentFirstCallsShareOneProxyClass() throws Exception {
        int threads = 16;
        int rounds = 500;
        var pool = Executors.newFixedThreadPool(threads);
        try {
            for (int round = 0; round < rounds; round++) {
                var classLoader = newClassLoader();
                var barrier = new CyclicBarrier(threads);
                var classes = ConcurrentHashMap.<Class<?>>newKeySet();
                var futures = new Future<?>[threads];
                for (int i = 0; i < threads; i++) {
                    futures[i] = pool.submit(() -> {
                        barrier.await();
                        var echo = ASMProxyFactory.newProxyInstance(classLoader, (method, args) -> args[0], Echo.class);
                        classes.add(echo.getClass());
                        return null;
                    });
                }
                for (var future : futures) {
                    future.get();
                }
                assertEquals(1, classes.size(), "All threads must share a single generated proxy class");
            }
        } finally {
            pool.shutdownNow();
        }
    }
}
