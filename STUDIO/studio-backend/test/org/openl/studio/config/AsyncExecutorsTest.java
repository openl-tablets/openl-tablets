package org.openl.studio.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

class AsyncExecutorsTest {

    private final ThreadPoolTaskExecutor executor = AsyncExecutors.create("async-test-pool-", 1, 1);

    @AfterEach
    void stop() {
        SecurityContextHolder.clearContext();
        executor.destroy();
    }

    @Test
    void runsATaskAsTheUserWhoStartedItOnANamedThread() throws Exception {
        var user = new TestingAuthenticationToken("admin", "secret");
        SecurityContextHolder.getContext().setAuthentication(user);
        executor.afterPropertiesSet();
        var thread = new CompletableFuture<String>();
        var authentication = new CompletableFuture<Authentication>();

        executor.execute(() -> {
            thread.complete(Thread.currentThread().getName());
            authentication.complete(SecurityContextHolder.getContext().getAuthentication());
        });

        assertTrue(thread.get(5, TimeUnit.SECONDS).startsWith("async-test-pool-"));
        assertEquals(user, authentication.get(5, TimeUnit.SECONDS));
    }

    @Test
    void letsTheRunningTaskEndWhenTheApplicationStops() throws Exception {
        executor.afterPropertiesSet();
        var started = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var ended = new CompletableFuture<Boolean>();
        executor.execute(() -> {
            started.countDown();
            try {
                ended.complete(release.await(5, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                ended.completeExceptionally(e);
            }
        });
        assertTrue(started.await(5, TimeUnit.SECONDS));
        // The task is let go while the pool is being shut down.
        CompletableFuture.delayedExecutor(100, TimeUnit.MILLISECONDS).execute(release::countDown);

        executor.destroy();

        assertTrue(ended.getNow(false));
    }

    @Test
    void interruptsATaskWhoseFutureIsCancelled() throws Exception {
        var interruptible = AsyncExecutors.createInterruptible("async-interruptible-pool-", 1, 1);
        interruptible.afterPropertiesSet();
        try {
            var started = new CountDownLatch(1);
            var interrupted = new CompletableFuture<Boolean>();
            var future = interruptible.submitCompletable(() -> {
                started.countDown();
                try {
                    new CountDownLatch(1).await(5, TimeUnit.SECONDS);
                    interrupted.complete(false);
                } catch (InterruptedException e) {
                    interrupted.complete(true);
                }
                return "ended";
            });
            assertTrue(started.await(5, TimeUnit.SECONDS));

            future.cancel(true);

            assertTrue(interrupted.get(5, TimeUnit.SECONDS));
            assertTrue(future.isCancelled());
        } finally {
            interruptible.destroy();
        }
    }

    @Test
    void neverStartsAQueuedTaskWhoseFutureIsCancelled() throws Exception {
        var interruptible = AsyncExecutors.createInterruptible("async-interruptible-pool-", 1, 1);
        interruptible.afterPropertiesSet();
        try {
            var release = new CountDownLatch(1);
            var first = interruptible.submitCompletable(() -> release.await(5, TimeUnit.SECONDS));
            var queuedStarted = new AtomicBoolean();
            var queued = interruptible.submitCompletable(() -> queuedStarted.getAndSet(true));

            queued.cancel(true);
            release.countDown();

            assertTrue(first.get(5, TimeUnit.SECONDS));
            // The pool has one thread: once a task after the cancelled one ends, the cancelled one had its turn.
            assertEquals("next", interruptible.submitCompletable(() -> "next").get(5, TimeUnit.SECONDS));
            assertFalse(queuedStarted.get());
        } finally {
            interruptible.destroy();
        }
    }

    @Test
    void completesTheFutureTheWayItsTaskEnds() throws Exception {
        var interruptible = AsyncExecutors.createInterruptible("async-interruptible-pool-", 1, 1);
        interruptible.afterPropertiesSet();
        try {
            assertEquals("done", interruptible.submitCompletable(() -> "done").get(5, TimeUnit.SECONDS));

            var failure = new IllegalStateException("broken");
            var failed = interruptible.submitCompletable(() -> {
                throw failure;
            });
            var thrown = assertThrows(ExecutionException.class, () -> failed.get(5, TimeUnit.SECONDS));
            assertSame(failure, thrown.getCause());
        } finally {
            interruptible.destroy();
        }
    }
}
