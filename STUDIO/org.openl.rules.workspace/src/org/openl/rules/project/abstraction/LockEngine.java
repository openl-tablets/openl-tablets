package org.openl.rules.project.abstraction;

import org.openl.rules.lock.LockInfo;

public interface LockEngine {
    boolean tryLock(String repoId, String branch, String projectName, String userName);

    void unlock(String repoId, String branch, String projectName);

    void forceUnlock(String repoId, String branch, String projectName);

    LockInfo getLockInfo(String repoId, String branch, String projectName);

    /**
     * Releases the locks of every project of a repository, in every branch. The locks no longer hold once the
     * repository stores its projects in another place.
     *
     * @param repoId the id of the repository
     */
    void forceUnlockRepository(String repoId);
}
