package org.openl.studio.projects.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class NotificationDebouncerTest {

    private final NotificationDebouncer debouncer = new NotificationDebouncer(50);

    @AfterEach
    void tearDown() {
        debouncer.shutdown();
    }

    /**
     * Waits until everything already on the scheduler has run: a probe delivery on a fresh key is
     * scheduled after any earlier task, and the single scheduler thread runs tasks in time order.
     */
    private void awaitScheduledWorkFlushed() throws InterruptedException {
        var probe = new CountDownLatch(1);
        debouncer.debounce("flush-probe-" + probe.hashCode(), probe::countDown);
        assertTrue(probe.await(5, TimeUnit.SECONDS));
    }

    @Test
    void a_burst_of_signals_delivers_once() throws Exception {
        var delivered = new AtomicInteger();
        var latch = new CountDownLatch(1);

        for (int i = 0; i < 10; i++) {
            debouncer.debounce("user", () -> {
                delivered.incrementAndGet();
                latch.countDown();
            });
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        // A probe on another key is scheduled after any absorbed duplicate would have been; the
        // single scheduler thread runs tasks in time order, so once it fires nothing else is due.
        awaitScheduledWorkFlushed();
        assertEquals(1, delivered.get());
    }

    @Test
    void distinct_keys_deliver_independently() throws Exception {
        var latch = new CountDownLatch(2);

        debouncer.debounce("jane", latch::countDown);
        debouncer.debounce("john", latch::countDown);

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void a_signal_after_the_window_opens_a_new_delivery() throws Exception {
        var first = new CountDownLatch(1);
        debouncer.debounce("user", first::countDown);
        assertTrue(first.await(5, TimeUnit.SECONDS));

        var second = new CountDownLatch(1);
        debouncer.debounce("user", second::countDown);
        assertTrue(second.await(5, TimeUnit.SECONDS));
    }

    @Test
    void the_payloads_of_a_burst_merge_into_one_delivery() throws Exception {
        var delivered = new ConcurrentLinkedQueue<Set<String>>();
        var latch = new CountDownLatch(1);

        debouncer.debounce("user|p", List.of("rules/A.xlsx"), files -> {
            delivered.add(files);
            latch.countDown();
        });
        debouncer.debounce("user|p", List.of("rules/B.xlsx"), files -> {
            delivered.add(files);
            latch.countDown();
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        awaitScheduledWorkFlushed();
        assertEquals(List.of(Set.of("rules/A.xlsx", "rules/B.xlsx")), List.copyOf(delivered));
    }

    @Test
    void a_signal_during_a_delivery_opens_a_new_window_with_its_own_payload() throws Exception {
        var delivered = new ConcurrentLinkedQueue<Set<String>>();
        var firstRunning = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var done = new CountDownLatch(2);

        debouncer.debounce("user|p", List.of("rules/A.xlsx"), files -> {
            delivered.add(files);
            firstRunning.countDown();
            try {
                release.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            done.countDown();
        });

        // The first delivery has drained its window and is running; this signal must not be swept
        // into it and must not be lost — it opens a new window carrying its own file.
        assertTrue(firstRunning.await(5, TimeUnit.SECONDS));
        debouncer.debounce("user|p", List.of("rules/B.xlsx"), files -> {
            delivered.add(files);
            done.countDown();
        });
        release.countDown();

        assertTrue(done.await(5, TimeUnit.SECONDS));
        assertEquals(List.of(Set.of("rules/A.xlsx"), Set.of("rules/B.xlsx")), List.copyOf(delivered));
    }

    @Test
    void a_failing_delivery_never_kills_the_scheduler() throws Exception {
        var failing = new CountDownLatch(1);
        debouncer.debounce("user", () -> {
            failing.countDown();
            throw new IllegalStateException("boom");
        });

        // The key frees up before the delivery runs, so a later delivery opens its own window.
        assertTrue(failing.await(5, TimeUnit.SECONDS));
        var next = new CountDownLatch(1);
        debouncer.debounce("user", next::countDown);
        assertTrue(next.await(5, TimeUnit.SECONDS));
    }
}
