package org.openl.rules.project.abstraction;

import org.openl.rules.lock.LockInfo;

public interface LockEngine {
    boolean tryLock(String repoId, String branch, String projectName, String userName);

    void unlock(String repoId, String branch, String projectName);

    void forceUnlock(String repoId, String branch, String projectName);

    LockInfo getLockInfo(String repoId, String branch, String projectName);
}
