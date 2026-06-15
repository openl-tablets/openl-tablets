package org.openl.rules.core.ce;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
