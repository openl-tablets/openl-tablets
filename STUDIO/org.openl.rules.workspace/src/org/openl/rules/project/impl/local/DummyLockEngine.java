package org.openl.rules.project.impl.local;

import org.openl.rules.lock.LockInfo;
import org.openl.rules.project.abstraction.LockEngine;

public class DummyLockEngine implements LockEngine {
    @Override
    public boolean tryLock(String repoId, String branch, String projectName, String userName) {
        return true;
    }

    @Override
    public void unlock(String repoId, String branch, String projectName) {
        //nothing to do
    }

    @Override
    public void forceUnlock(String repoId, String branch, String projectName) {
        //nothing to do
    }

    @Override
    public LockInfo getLockInfo(String repoId, String branch, String projectName) {
        return LockInfo.NO_LOCK;
    }
}
