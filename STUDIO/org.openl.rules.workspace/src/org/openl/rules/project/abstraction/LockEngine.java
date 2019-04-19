package org.openl.rules.project.abstraction;

import org.openl.rules.common.LockInfo;

public interface LockEngine {
    boolean tryLock(String branch, String projectName, String userName) throws LockException;

    void unlock(String branch, String projectName);

    LockInfo getLockInfo(String branch, String projectName);
}
