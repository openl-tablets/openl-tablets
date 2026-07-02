package org.openl.studio.projects.service.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import org.openl.rules.webstudio.web.trace.debug.DebugListener;
import org.openl.rules.webstudio.web.trace.debug.TraceDebugger;
import org.openl.studio.projects.model.ProjectIdModel;
import org.openl.studio.projects.model.trace.TraceDebugMapper;

class DebugSessionTest {

    private static DebugSession session() {
        var projectId = ProjectIdModel.builder().repository("repo").projectName("A").build();
        return new DebugSession(projectId, "table", new TraceDebugger(DebugListener.NOOP), null);
    }

    @Test
    void mapperIsBuiltOnceAndCached() {
        var session = session();
        var mapper = mock(TraceDebugMapper.class);
        var calls = new AtomicInteger();
        Supplier<TraceDebugMapper> factory = () -> {
            calls.incrementAndGet();
            return mapper;
        };

        assertSame(mapper, session.mapper(factory));
        assertSame(mapper, session.mapper(factory));
        assertSame(mapper, session.mapper(factory));
        assertEquals(1, calls.get(), "the heavy mapper must be built once and reused");
    }

    @Test
    void inLockReturnsTheSuppliedValue() {
        assertEquals("value", session().inLock(() -> "value"));
    }

    @Test
    void inLockRunsTheAction() {
        var ran = new AtomicBoolean();
        session().inLock(() -> ran.set(true));
        assertTrue(ran.get());
    }

    @Test
    void inLockSerialisesConcurrentAccess() throws InterruptedException {
        var session = session();
        int threads = 8;
        int iterations = 1_000;
        var inside = new AtomicInteger();
        var maxObserved = new AtomicInteger();
        var overlapped = new AtomicBoolean();

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        var start = new CountDownLatch(1);
        var done = new CountDownLatch(threads);
        for (int t = 0; t < threads; t++) {
            pool.execute(() -> {
                try {
                    start.await();
                    for (int i = 0; i < iterations; i++) {
                        session.inLock(() -> {
                            int now = inside.incrementAndGet();
                            if (now != 1) {
                                overlapped.set(true);
                            }
                            maxObserved.accumulateAndGet(now, Math::max);
                            inside.decrementAndGet();
                        });
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertTrue(done.await(30, TimeUnit.SECONDS), "lock workers did not finish");
        pool.shutdownNow();
        assertFalse(overlapped.get(), "two threads were inside the lock at once");
        assertEquals(1, maxObserved.get(), "the lock must admit one thread at a time");
    }
}
