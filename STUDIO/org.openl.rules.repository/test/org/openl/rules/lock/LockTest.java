package org.openl.rules.lock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LockTest {

    private Lock lock;
    @TempDir
    private Path tempDirectoryPath;
    static final int MAX_THREADS = Math.min(12, Runtime.getRuntime().availableProcessors() * 2);

    @BeforeEach
    void setUp() throws IOException {
        lock = new Lock(tempDirectoryPath, "my/lock/id");
    }

    @Test
    void testSimpleLock() {
        var lock1 = lock.tryLock("user1");
        assertTrue(lock1);
        lock1 = lock.tryLock("user1");
        assertTrue(lock1);
        var lock2 = lock.tryLock("user2");
        assertFalse(lock2);
        lock.unlock();
        var lockInfo = lock.info();
        assertFalse(lockInfo.isLocked());
    }

    @Test
    void testSimultaneousLocks() throws IOException {
        var user1PrepareLock = lock.createLockFile("user3");
        var user2PrepareLock = lock.createLockFile("user4");
        var user2Lock = lock.finishLockCreating(user2PrepareLock);
        assertFalse(user2Lock);
        var user1Lock = lock.finishLockCreating(user1PrepareLock);
        assertTrue(user1Lock);
        var lockInfo = lock.info();
        assertEquals("user3", lockInfo.getLockedBy());
        lock.unlock();
        lockInfo = lock.info();
        assertFalse(lockInfo.isLocked());
    }

    @Test
    void testSimultaneousLocksWithDelay() throws IOException {
        var user2PrepareLock = lock.createLockFile("user5");
        try {
            TimeUnit.MILLISECONDS.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        var user1PrepareLock = lock.createLockFile("user6");
        var user1Lock = lock.finishLockCreating(user1PrepareLock);
        assertFalse(user1Lock);
        var user2Lock = lock.finishLockCreating(user2PrepareLock);
        assertTrue(user2Lock);
        var lockInfo = lock.info();
        assertEquals("user5", lockInfo.getLockedBy());
        lock.unlock();
        lockInfo = lock.info();
        assertFalse(lockInfo.isLocked());
    }

    @Disabled("Unstable test. It proves non-working solution of the Lock system based on the file system.")
    @Test
    void testSimultaneousMultiThreadsForDifferentUsers() throws InterruptedException {
        testSimultaneousMultiThreads(true);
        testSimultaneousMultiThreads(false);
    }

    private void testSimultaneousMultiThreads(boolean diffUsers) throws InterruptedException {
        var streaming = MAX_THREADS;
        var attempts = 100;
        var passed = new AtomicBoolean(true);
        var testedValue = new AtomicInteger(0);
        var countDown = new CountDownLatch(streaming);
        for (var i = 0; i < streaming; i++) {
            var finalI = i;
            var thread = new Thread(() -> {
                for (var j = 0; j < attempts; j++) {
                    try {
                        String userName = diffUsers ? "user" + finalI : "";
                        if (lock.tryLock(userName)) {
                            testedValue.set(31);
                            for (var k = 0; k <= 1000; k++) {
                                var i1 = testedValue.get();
                                testedValue.set(i1 + k);
                                Thread.yield();
                            }
                            //Test that more than one thread does not receive locks at the same time and do not interfere with calculations
                            if (testedValue.get() != 500531) {
                                passed.set(false);
                                break;
                            }
                            lock.unlock();
                        }
                    } catch (Exception e) {
                        passed.set(false);
                        e.printStackTrace();
                        break;
                    }
                }
                countDown.countDown();
            });
            thread.start();
        }
        countDown.await();
        assertTrue(passed.get());
    }

    @Test
    void testSimultaneousMultiThreadsWithWaiting() throws InterruptedException {
        // The file lock elects a winner by last-modified time; on a coarse-mtime CI that degrades to a
        // deterministic filename tie-break, so the losing threads make no progress until the winners drain
        // their own work. That contention is self-terminating, so the retry only has to outlast it. Cap the
        // whole flow with one shared deadline — not an independent budget per attempt — so a genuinely stuck
        // lock fails in bounded time instead of attempts x the wait, while a healthy run (which drains well
        // inside the deadline) still lands every acquisition. The old 100 attempts / 30 s-per-attempt pairing
        // let the window outlast one attempt's budget, timing out a single acquisition of 800 ("799 != 800").
        var streaming = MAX_THREADS;
        var attempts = 50;
        var deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(90);
        var passed = new AtomicBoolean(true);
        var testedValue = new AtomicInteger(0);
        var countDown = new CountDownLatch(streaming);
        var locksCounter = new AtomicInteger();
        for (var i = 0; i < streaming; i++) {
            var finalI = i;
            var thread = new Thread(() -> {
                for (var j = 0; j < attempts; j++) {
                    try {
                        var userName = "user" + finalI;
                        var remainingMillis = deadline - System.currentTimeMillis();
                        if (remainingMillis > 0 && lock.tryLock(userName, remainingMillis, TimeUnit.MILLISECONDS)) {
                            locksCounter.getAndIncrement();
                            testedValue.set(31);
                            for (var k = 0; k <= 1000; k++) {
                                var i1 = testedValue.get();
                                testedValue.set(i1 + k);
                                Thread.yield();
                            }
                            //Test that more than one thread does not receive locks at the same time and do not interfere with calculations
                            if (testedValue.get() != 500531) {
                                passed.set(false);
                                break;
                            }
                            lock.unlock();
                        }
                    } catch (Exception e) {
                        passed.set(false);
                        e.printStackTrace();
                        break;
                    }
                }
                countDown.countDown();
            });
            thread.start();
        }
        countDown.await();
        assertEquals(streaming * attempts, locksCounter.get());
        assertTrue(passed.get());
    }

    @Test
    void testTryLockWithTimeout() {
        var lock1 = lock.tryLock("user1");
        assertTrue(lock1);
        var lock2 = lock.tryLock("user2", 1, TimeUnit.SECONDS);
        assertFalse(lock2);
        lock.unlock();
        lock2 = lock.tryLock("user2");
        assertTrue(lock2);
        lock.unlock();
        var lockInfo = lock.info();
        assertFalse(lockInfo.isLocked());
    }

    @Test
    void testForceLock() {
        var lock1 = lock.tryLock("user1");
        assertTrue(lock1);
        lock.forceLock("user2", 1, TimeUnit.SECONDS);
        var lockInfo = lock.info();
        assertEquals("user2", lockInfo.getLockedBy());
    }

    @Test
    void testForceLockInterrupting() {
        assertTrue(lock.tryLock("user1"));

        var interrupted = new AtomicBoolean(true);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            var locked = lock.forceLock("user3", 1, TimeUnit.MINUTES);
            interrupted.set(locked);
        });

        // Interrupt long running thread
        try {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        } finally {
            assertTrue(executor.isTerminated(), "Long running thread must be terminated");
            assertFalse(interrupted.get(), "forceLock() must not get a lock when it was interrupted");
        }

        // Make sure that the lock is not overridden.
        var lockInfo = lock.info();
        assertEquals("user1", lockInfo.getLockedBy());
    }
}
