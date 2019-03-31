package org.openl.rules.workspace.dtr.impl;

import java.util.Date;

import org.openl.rules.common.LockInfo;
import org.openl.rules.repository.RLock;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.WorkspaceUserImpl;

/**
 *
 * @author Aleh Bykhavets
 *
 */
public class LockInfoImpl implements LockInfo {

    /** nil object to avoid NullPointer exceptions */
    public static final LockInfoImpl NO_LOCK = new LockInfoImpl();

    private boolean isLocked;
    private Date lockedAt;
    private WorkspaceUser lockedBy;

    private LockInfoImpl() {
        isLocked = false;
    }

    public LockInfoImpl(RLock ralLock) {
        isLocked = ralLock.isLocked();

        if (isLocked) {
            lockedAt = ralLock.getLockedAt();
            lockedBy = new WorkspaceUserImpl(ralLock.getLockedBy().getUserName());
        }
    }

    @Override
    public Date getLockedAt() {
        return lockedAt;
    }

    @Override
    public WorkspaceUser getLockedBy() {
        return lockedBy;
    }

    @Override
    public boolean isLocked() {
        return isLocked;
    }
}
