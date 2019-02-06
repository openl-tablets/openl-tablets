package org.openl.rules.project.abstraction;

import org.openl.rules.common.LockInfo;

public interface LockEngine {
    boolean lock(String projectName, String userName) throws LockException;

    void unlock(String projectName);

    LockInfo getLockInfo(String projectName);
}
