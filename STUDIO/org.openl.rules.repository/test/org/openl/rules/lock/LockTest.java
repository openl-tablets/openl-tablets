package org.openl.rules.lock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

public class LockTest {

    private Lock lock;

    @Before
    public void setUp() throws IOException {
        Path tempDirectoryPath = Files.createTempDirectory("openl-locks");
        lock = new Lock(tempDirectoryPath, "my/lock/id");
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
    public void testTryLockWithTimeout() throws IOException {
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

}
