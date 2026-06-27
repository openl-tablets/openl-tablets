package org.openl.rules.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the per-project model store backing parallel multi-project editing: identity per key, bounded
 * LRU eviction (idle, non-pinned), and safe concurrent first-touch.
 */
class ProjectModelRegistryTest {

    private static ProjectModelKey key(String folder) {
        return new ProjectModelKey("design", folder, "main");
    }

    private static Function<ProjectModelKey, ProjectModel> mockFactory() {
        return k -> mock(ProjectModel.class);
    }

    @Test
    void getOrCreateReturnsSameInstancePerKey() {
        var registry = new ProjectModelRegistry(mockFactory(), 8, m -> true);

        var a1 = registry.getOrCreate(key("A"));
        var a2 = registry.getOrCreate(key("A"));
        var b = registry.getOrCreate(key("B"));

        assertSame(a1, a2);
        assertNotSame(a1, b);
        assertEquals(2, registry.size());
    }

    @Test
    void getReturnsNullWhenAbsent() {
        var registry = new ProjectModelRegistry(mockFactory(), 8, m -> true);

        assertNull(registry.get(key("A")));
        var a = registry.getOrCreate(key("A"));
        assertSame(a, registry.get(key("A")));
    }

    @Test
    void removeDestroysModel() {
        var registry = new ProjectModelRegistry(mockFactory(), 8, m -> true);
        var a = registry.getOrCreate(key("A"));

        registry.remove(key("A"));

        verify(a).destroy();
        assertNull(registry.get(key("A")));
    }

    @Test
    void clearDestroysAll() {
        var registry = new ProjectModelRegistry(mockFactory(), 8, m -> true);
        var a = registry.getOrCreate(key("A"));
        var b = registry.getOrCreate(key("B"));

        registry.clear();

        verify(a).destroy();
        verify(b).destroy();
        assertEquals(0, registry.size());
    }

    @Test
    void evictsLeastRecentlyUsedIdleModelOverCap() {
        var registry = new ProjectModelRegistry(mockFactory(), 2, m -> true);
        var a = registry.getOrCreate(key("A"));
        registry.getOrCreate(key("B"));
        // Touch A so B becomes the least-recently-used entry.
        registry.getOrCreate(key("A"));

        var c = registry.getOrCreate(key("C"));

        assertEquals(2, registry.size());
        assertNull(registry.get(key("B")));
        assertSame(a, registry.get(key("A")));
        assertSame(c, registry.get(key("C")));
    }

    @Test
    void pinnedModelIsNeverEvicted() {
        var registry = new ProjectModelRegistry(mockFactory(), 2, m -> true);
        var a = registry.getOrCreate(key("A"));
        registry.setPinned(key("A"));
        var b = registry.getOrCreate(key("B"));

        registry.getOrCreate(key("C"));

        assertSame(a, registry.get(key("A")));
        verify(b).destroy();
        assertNull(registry.get(key("B")));
    }

    @Test
    void nonEvictableModelsExceedCapButAreNotDestroyed() {
        var registry = new ProjectModelRegistry(mockFactory(), 1, m -> false);
        var a = registry.getOrCreate(key("A"));
        var b = registry.getOrCreate(key("B"));

        assertEquals(2, registry.size());
        verify(a, never()).destroy();
        verify(b, never()).destroy();
    }

    @Test
    void removeMatchingDestroysSelectedExceptPinned() {
        var registry = new ProjectModelRegistry(mockFactory(), 8, m -> true);
        var a = registry.getOrCreate(key("A"));
        var b = registry.getOrCreate(key("B"));
        registry.setPinned(key("A"));

        registry.removeMatching(m -> true);

        assertSame(a, registry.get(key("A")));
        assertNull(registry.get(key("B")));
        verify(b).destroy();
        verify(a, never()).destroy();
    }

    @Test
    void concurrentGetOrCreateCreatesSingleInstance() throws InterruptedException {
        var factoryCalls = new AtomicInteger();
        Function<ProjectModelKey, ProjectModel> factory = k -> {
            factoryCalls.incrementAndGet();
            return mock(ProjectModel.class);
        };
        var registry = new ProjectModelRegistry(factory, 64, m -> true);

        int threads = 16;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        var start = new CountDownLatch(1);
        var done = new CountDownLatch(threads);
        Set<ProjectModel> results = ConcurrentHashMap.newKeySet();
        for (int i = 0; i < threads; i++) {
            pool.execute(() -> {
                try {
                    start.await();
                    results.add(registry.getOrCreate(key("A")));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertTrue(done.await(10, TimeUnit.SECONDS));
        pool.shutdownNow();

        assertEquals(1, results.size(), "all threads must observe the same model");
        assertEquals(1, factoryCalls.get(), "factory must run exactly once per key");
    }
}
