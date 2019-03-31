package org.openl.rules.project.impl.local;

import org.openl.rules.common.LockInfo;
import org.openl.rules.project.abstraction.LockEngine;

public class DummyLockEngine implements LockEngine {
    @Override
    public boolean tryLock(String projectName, String userName) {
        return true;
    }

    @Override
    public void unlock(String projectName) {
    }

    @Override
    public LockInfo getLockInfo(String projectName) {
        return new SimpleLockInfo(false, null, null);
    }
}
