package org.openl.rules.lock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openl.util.FileUtils;

public class LockTest {

    private Lock lock;
    private Path tempDirectoryPath;
    static final int MAX_THREADS = Runtime.getRuntime().availableProcessors() * 2;

    @Before
    public void setUp() throws IOException {
        tempDirectoryPath = Files.createTempDirectory("openl-locks");
        lock = new Lock(tempDirectoryPath, "my/lock/id");
    }

    @After
    public void tearDown() throws IOException {
        FileUtils.delete(tempDirectoryPath.toFile());
        if (tempDirectoryPath.toFile().exists()) {
            fail("Cannot delete folder " + tempDirectoryPath);
        }
    }

    @Test
    public void testSimpleLock() {
        boolean lock1 = lock.tryLock("user1");
        assertTrue(lock1);
        lock1 = lock.tryLock("user1");
        assertTrue(lock1);
        boolean lock2 = lock.tryLock("user2");
        assertFalse(lock2);
        lock.unlock();
        LockInfo lockInfo = lock.info();
        assertFalse(lockInfo.isLocked());
    }

    @Test
    public void testSimultaneousLocks() throws IOException {
        Path user1PrepareLock = lock.createLockFile("user3");
        Path user2PrepareLock = lock.createLockFile("user4");
        boolean user2Lock = lock.finishLockCreating(user2PrepareLock);
        assertFalse(user2Lock);
        boolean user1Lock = lock.finishLockCreating(user1PrepareLock);
        assertTrue(user1Lock);
        LockInfo lockInfo = lock.info();
        assertEquals("user3", lockInfo.getLockedBy());
        lock.unlock();
        lockInfo = lock.info();
        assertFalse(lockInfo.isLocked());
    }

    @Test
    public void testSimultaneousLocksWithDelay() throws IOException {
        Path user2PrepareLock = lock.createLockFile("user5");
        try {
            TimeUnit.MILLISECONDS.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        Path user1PrepareLock = lock.createLockFile("user6");
        boolean user1Lock = lock.finishLockCreating(user1PrepareLock);
        assertFalse(user1Lock);
        boolean user2Lock = lock.finishLockCreating(user2PrepareLock);
        assertTrue(user2Lock);
        LockInfo lockInfo = lock.info();
        assertEquals("user5", lockInfo.getLockedBy());
        lock.unlock();
        lockInfo = lock.info();
        assertFalse(lockInfo.isLocked());
    }

    @Test
    public void testSimultaneousMultiThreadsForDifferentUsers() throws InterruptedException {
        testSimultaneousMultiThreads(true);
        testSimultaneousMultiThreads(false);
    }

    private void testSimultaneousMultiThreads(boolean diffUsers) throws InterruptedException {
        int streaming = MAX_THREADS;
        int attempts = 100;
        AtomicBoolean passed = new AtomicBoolean(true);
        AtomicInteger testedValue = new AtomicInteger(0);
        CountDownLatch countDown = new CountDownLatch(streaming);
        for (int i = 0; i < streaming; i++) {
            int finalI = i;
            Thread thread = new Thread(() -> {
                for (int j = 0; j < attempts; j++) {
                    try {
                        String userName = diffUsers ? "user" + finalI : "";
                        if (lock.tryLock(userName)) {
                            testedValue.set(31);
                            for (int k = 0; k <= 1000; k++) {
                                int i1 = testedValue.get();
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
    public void testSimultaneousMultiThreadsWithWaiting() throws InterruptedException {
        int streaming = MAX_THREADS;
        int attempts = 100;
        AtomicBoolean passed = new AtomicBoolean(true);
        AtomicInteger testedValue = new AtomicInteger(0);
        CountDownLatch countDown = new CountDownLatch(streaming);
        AtomicInteger locksCounter = new AtomicInteger();
        for (int i = 0; i < streaming; i++) {
            int finalI = i;
            Thread thread = new Thread(() -> {
                for (int j = 0; j < attempts; j++) {
                    try {
                        String userName = "user" + finalI;
                        if (lock.tryLock(userName, 30, TimeUnit.SECONDS)) {
                            locksCounter.getAndIncrement();
                            testedValue.set(31);
                            for (int k = 0; k <= 1000; k++) {
                                int i1 = testedValue.get();
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
    public void testTryLockWithTimeout() {
        boolean lock1 = lock.tryLock("user1");
        assertTrue(lock1);
        boolean lock2 = lock.tryLock("user2", 1, TimeUnit.SECONDS);
        assertFalse(lock2);
        lock.unlock();
        lock2 = lock.tryLock("user2");
        assertTrue(lock2);
        lock.unlock();
        LockInfo lockInfo = lock.info();
        assertFalse(lockInfo.isLocked());
    }

    @Test
    public void testForceLock() {
        boolean lock1 = lock.tryLock("user1");
        assertTrue(lock1);
        lock.forceLock("user2", 1, TimeUnit.SECONDS);
        LockInfo lockInfo = lock.info();
        assertEquals("user2", lockInfo.getLockedBy());
    }

    @Test
    public void testForceLockInterrupting() {
        assertTrue(lock.tryLock("user1"));

        AtomicBoolean interrupted = new AtomicBoolean(true);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            boolean locked = lock.forceLock("user3", 1, TimeUnit.MINUTES);
            interrupted.set(locked);
        });

        // Interrupt long running thread
        try {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        } finally {
            assertTrue("Long running thread must be terminated", executor.isTerminated());
            assertFalse("forceLock() must not get a lock when it was interrupted", interrupted.get());
        }

        // Make sure that the lock isn't overridden.
        LockInfo lockInfo = lock.info();
        assertEquals("user1", lockInfo.getLockedBy());
    }
}
