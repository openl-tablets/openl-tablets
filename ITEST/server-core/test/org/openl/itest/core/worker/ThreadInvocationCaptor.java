package org.openl.itest.core.worker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

class ThreadInvocationCaptor {

    private final Map<Long, AtomicInteger> counterMap = Collections.synchronizedMap(new HashMap<>());
    private final CountDownLatch waiter;
    private final int atLeast;
    private final int nThread;

    ThreadInvocationCaptor(int atLeast, int nThread) {
        this.atLeast = atLeast;
        this.nThread = nThread;
        this.waiter = new CountDownLatch(nThread * atLeast);
    }

    void capture() {
        Long threadId = Thread.currentThread().getId();
        AtomicInteger times = counterMap.get(threadId);
        if (times == null) {
            times = new AtomicInteger();
            counterMap.put(threadId, times);
        }
        int cnt = times.incrementAndGet();
        if (cnt <= atLeast) {
            waiter.countDown();
        }
    }

    void await(int timeout) {
        try {
            waiter.await(timeout, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace(); // For debug purposes
            Thread.currentThread().interrupt();
        }
        assertEquals(String.format("'%s' must be created", nThread), nThread, counterMap.size());
        for (AtomicInteger times : counterMap.values()) {
            assertTrue(String.format("Each thread must be executed at least '%s' times", atLeast),
                times.get() >= atLeast);
        }
    }
}
