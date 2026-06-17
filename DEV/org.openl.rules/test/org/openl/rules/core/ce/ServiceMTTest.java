package org.openl.rules.core.ce;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import org.openl.vm.SimpleRuntimeEnv;

class ServiceMTTest {

    private static int runOnce(ServiceMT serviceMT) {
        AtomicInteger counter = new AtomicInteger();
        SimpleRuntimeEnv env = new SimpleRuntimeEnv();
        serviceMT.execute(env, e -> counter.incrementAndGet());
        serviceMT.join(env);
        return counter.get();
    }

    @Test
    void shutdownStopsThePoolAndExecuteRecreatesIt() {
        ServiceMT serviceMT = ServiceMT.getInstance();

        assertEquals(1, runOnce(serviceMT), "the parallel action must run");

        serviceMT.shutdown(); // releases the pool; its worker threads terminate

        // a later execution lazily recreates the pool, so the singleton keeps working
        assertEquals(1, runOnce(serviceMT), "execute must recreate the pool after shutdown");
    }

    @Test
    void shutdownIsSafeWhenIdle() {
        ServiceMT serviceMT = ServiceMT.getInstance();
        serviceMT.shutdown();
        // shutting down again with no live pool must be a no-op, not an error
        assertDoesNotThrow(serviceMT::shutdown);
    }

    @Test
    void joinRethrowsTheRuntimeExceptionThrownByAParallelTask() {
        ServiceMT serviceMT = ServiceMT.getInstance();
        SimpleRuntimeEnv env = new SimpleRuntimeEnv();
        serviceMT.execute(env, e -> {
            throw new IllegalStateException("boom");
        });
        var ex = assertThrows(IllegalStateException.class, () -> serviceMT.join(env));
        assertEquals("boom", ex.getMessage(), "the original exception must propagate unwrapped");
    }

    @Test
    void joinRethrowsTheErrorThrownByAParallelTask() {
        ServiceMT serviceMT = ServiceMT.getInstance();
        SimpleRuntimeEnv env = new SimpleRuntimeEnv();
        serviceMT.execute(env, e -> {
            throw new AssertionError("fatal");
        });
        var ex = assertThrows(AssertionError.class, () -> serviceMT.join(env));
        assertEquals("fatal", ex.getMessage(), "errors must propagate unwrapped");
    }

    @Test
    void joinCancelsRemainingTasksWhenAnEarlierTaskFails() {
        ServiceMT serviceMT = ServiceMT.getInstance();
        SimpleRuntimeEnv env = new SimpleRuntimeEnv();
        // the first task fails, so join() must stop joining and cancel the still-pending second task in its finally
        serviceMT.execute(env, e -> {
            throw new IllegalStateException("boom");
        });
        serviceMT.execute(env, e -> {
        });
        assertThrows(IllegalStateException.class, () -> serviceMT.join(env));
        // the action queue is fully drained, so the singleton keeps working for the next call
        assertEquals(1, runOnce(serviceMT), "execute must keep working after a failed join");
    }

    @Test
    void executeSupportsNestedParallelTasks() {
        ServiceMT serviceMT = ServiceMT.getInstance();
        AtomicInteger counter = new AtomicInteger();
        SimpleRuntimeEnv env = new SimpleRuntimeEnv();
        // the outer task runs on a multi-thread env; a nested execute must clone it and run a child task in parallel
        serviceMT.execute(env, outer -> {
            serviceMT.execute(outer, inner -> counter.incrementAndGet());
            serviceMT.join(outer);
            counter.incrementAndGet();
        });
        serviceMT.join(env);
        assertEquals(2, counter.get(), "both the outer and the nested parallel task must run");
    }
}
