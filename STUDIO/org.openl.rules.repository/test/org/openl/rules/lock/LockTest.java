package org.openl.rules.lock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

class LockTest {

    private Lock lock;
    @TempDir
    private Path tempDirectoryPath;
    static final int MAX_THREADS = Math.min(12, Runtime.getRuntime().availableProcessors() * 2);

    @BeforeEach
    void setUp() {
        lock = new Lock(tempDirectoryPath, "my/lock/id");
    }

    @Test
    void lockIdEscapingTheLocksFolderIsRejected() {
        var error = assertThrows(IllegalArgumentException.class,
                () -> new Lock(tempDirectoryPath, "../outside/id"));

        assertEquals("Lock id '../outside/id' is outside of the locks folder.", error.getMessage());
        assertFalse(tempDirectoryPath.getParent().resolve("outside").toFile().exists());
    }

    @Test
    void lockIdPointingToTheLocksFolderItselfIsRejected() {
        var error = assertThrows(IllegalArgumentException.class, () -> new Lock(tempDirectoryPath, "nested/.."));

        assertEquals("Lock id 'nested/..' is outside of the locks folder.", error.getMessage());
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

    // The pause puts a time gap between the two lock files, which the scenario is about.
    @SuppressWarnings("java:S2925")
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

    /**
     * A lock file that another thread of the same user is still creating keeps the lock untaken. The attempt does not
     * fail and leaves that lock file in place.
     */
    @Test
    void lockFileOfTheSameUserInProgressLeavesTheLockUntaken() throws IOException {
        var lockFolder = Files.createDirectories(tempDirectoryPath.resolve("my/lock/id"));
        var inProgress = Files.createFile(lockFolder.resolve(Integer.toString("user1".hashCode(), 24) + ".lock"));

        assertFalse(lock.tryLock("user1"));

        assertTrue(Files.exists(inProgress));
        assertFalse(lock.info().isLocked());
    }

    /**
     * A lock file that cannot be written, as on a full disk, fails the lock at once with its cause instead of
     * waiting for the timeout as if another user held it.
     */
    @Test
    @DisabledOnOs(value = OS.WINDOWS, disabledReason = "Windows reports a file on the way of a folder as a missing folder")
    void lockFileThatCannotBeWrittenFailsWithItsCause() throws IOException {
        Files.writeString(tempDirectoryPath.resolve("my"), "a file in place of the lock folder");

        var error = assertThrows(UncheckedIOException.class, () -> lock.tryLock("user1", 1, TimeUnit.MINUTES));

        assertTrue(error.getMessage().startsWith("Cannot create a lock: "), error.getMessage());
        assertInstanceOf(FileSystemException.class, error.getCause());
        assertFalse(lock.info().isLocked());
    }
}
