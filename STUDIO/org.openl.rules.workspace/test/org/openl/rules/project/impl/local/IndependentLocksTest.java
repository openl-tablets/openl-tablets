package org.openl.rules.project.impl.local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.openl.rules.common.LockInfo;
import org.openl.rules.project.abstraction.LockException;
import org.openl.util.FileUtils;

public class IndependentLocksTest {

    private static final String PROJECT = "project";
    private static final String USER1 = "user1";
    private static final String USER2 = "user2";

    private String tempDirectoryPath;

    @Before
    public void setUp() throws LockException {
        tempDirectoryPath = FileUtils.getTempDirectoryPath();
        IndependentLocks.unlock(tempDirectoryPath, PROJECT);
    }

    @Test
    public void testSimpleLock() throws LockException {
        boolean lock1 = IndependentLocks.createLock(tempDirectoryPath, PROJECT, USER1);
        assertTrue(lock1);
        boolean lock2 = IndependentLocks.createLock(tempDirectoryPath, PROJECT, USER2);
        assertFalse(lock2);
        IndependentLocks.unlock(tempDirectoryPath, PROJECT);
        LockInfo lockInfo = IndependentLocks.getLockInfo(tempDirectoryPath, PROJECT);
        assertFalse(lockInfo.isLocked());
    }

    @Test
    public void testSimultaneousLocks() throws LockException {
        File user1PrepareLock = IndependentLocks.createLockFile(tempDirectoryPath, PROJECT, USER1, USER1);
        File user2PrepareLock = IndependentLocks.createLockFile(tempDirectoryPath, PROJECT, USER2, USER2);
        boolean user2Lock = IndependentLocks.finishLockCreating(tempDirectoryPath, PROJECT, user2PrepareLock, USER2);
        assertFalse(user2Lock);
        boolean user1Lock = IndependentLocks.finishLockCreating(tempDirectoryPath, PROJECT, user1PrepareLock, USER1);
        assertTrue(user1Lock);
        LockInfo lockInfo = IndependentLocks.getLockInfo(tempDirectoryPath, PROJECT);
        assertEquals(USER1, lockInfo.getLockedBy().getUserName());
        IndependentLocks.unlock(tempDirectoryPath, PROJECT);
        lockInfo = IndependentLocks.getLockInfo(tempDirectoryPath, PROJECT);
        assertFalse(lockInfo.isLocked());
    }

    @Test
    public void testSimultaneousLocksWithDelay() throws LockException {
        File user2PrepareLock = IndependentLocks.createLockFile(tempDirectoryPath, PROJECT, USER2, USER2);
        try {
            TimeUnit.MILLISECONDS.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        File user1PrepareLock = IndependentLocks.createLockFile(tempDirectoryPath, PROJECT, USER1, USER1);
        boolean user1Lock = IndependentLocks.finishLockCreating(tempDirectoryPath, PROJECT, user1PrepareLock, USER1);
        assertFalse(user1Lock);
        boolean user2Lock = IndependentLocks.finishLockCreating(tempDirectoryPath, PROJECT, user2PrepareLock, USER2);
        assertTrue(user2Lock);
        LockInfo lockInfo = IndependentLocks.getLockInfo(tempDirectoryPath, PROJECT);
        assertEquals(USER2, lockInfo.getLockedBy().getUserName());
        IndependentLocks.unlock(tempDirectoryPath, PROJECT);
        lockInfo = IndependentLocks.getLockInfo(tempDirectoryPath, PROJECT);
        assertFalse(lockInfo.isLocked());
    }

}
