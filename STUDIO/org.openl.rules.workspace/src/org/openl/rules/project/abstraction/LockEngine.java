package org.openl.rules.project.abstraction;

import org.openl.rules.common.LockInfo;
import org.openl.rules.common.ProjectException;

public interface LockEngine {
    boolean lock(String projectName, String userName) throws ProjectException;

    void unlock(String projectName);

    LockInfo getLockInfo(String projectName);
}
