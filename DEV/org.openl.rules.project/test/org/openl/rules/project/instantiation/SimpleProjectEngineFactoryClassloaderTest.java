package org.openl.rules.project.instantiation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder;

class SimpleProjectEngineFactoryClassloaderTest {

    @Test
    void singleModuleTest() throws Exception {
        SimpleProjectEngineFactory<Object> factory = new SimpleProjectEngineFactoryBuilder<>()
                .setProject("test-resources/classpath/project1")
                .build();
        Object instance = factory.newInstance();
        assertNotNull(instance);
    }

    @Test
    void singleModuleWithoutDepTest() throws Exception {
        assertThrows(OpenlNotCheckedException.class, () -> {
            SimpleProjectEngineFactory<Object> factory = new SimpleProjectEngineFactoryBuilder<>()
                    .setProject("test-resources/classpath/project2")
                    .build();
            factory.newInstance();
        });
    }

    @Test
    void classloader_Test() throws Exception {
        SimpleProjectEngineFactory<Object> factory = new SimpleProjectEngineFactoryBuilder<>()
                .setWorkspace("test-resources/classpath")
                .setProject("test-resources/classpath/project2")
                .build();
        Object instance = factory.newInstance();
        assertNotNull(instance);
    }

    /**
     * Within a single factory, repeated {@code newInstance()} calls must reuse one proxy class; otherwise the
     * factory's long-lived classloader accumulates uniquely named classes until Metaspace is exhausted (issue #1230).
     *
     * Across factories the proxy class must stay distinct: each factory owns a separate classloader that defines its
     * own proxy, even though the deterministic proxy name is identical. This also guards against the reuse ever
     * bleeding across factories (e.g. via a global, name-keyed cache).
     */
    @Test
    void proxyClassIsReusedWithinFactoryButDistinctAcrossFactories() throws Exception {
        var firstFactory = new SimpleProjectEngineFactoryBuilder<>()
                .setProject("test-resources/classpath/project1")
                .build();
        var secondFactory = new SimpleProjectEngineFactoryBuilder<>()
                .setProject("test-resources/classpath/project1")
                .build();

        var first = firstFactory.newInstance();
        var second = firstFactory.newInstance();
        var other = secondFactory.newInstance();

        assertNotSame(first, second, "Each call must return a distinct instance");
        assertSame(first.getClass(), second.getClass(), "A single factory must reuse one proxy class across calls");

        assertNotSame(first.getClass(), other.getClass(), "Each factory must define its own proxy class");
        assertNotSame(first.getClass().getClassLoader(),
                other.getClass().getClassLoader(),
                "Each factory must use its own classloader");
        assertEquals(first.getClass().getName(),
                other.getClass().getName(),
                "The deterministic proxy name is shared, but the classes stay distinct per classloader");
    }
}
