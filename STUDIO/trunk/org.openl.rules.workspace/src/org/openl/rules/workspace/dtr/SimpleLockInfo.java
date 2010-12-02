package org.openl.rules.workspace.dtr;

import java.util.Date;

import org.openl.rules.workspace.WorkspaceUser;

public class SimpleLockInfo implements LockInfo {
    private WorkspaceUser user;
    private Date lockedAt;
    

    public SimpleLockInfo(WorkspaceUser user, Date lockedAt) {
        this.user = user;
        this.lockedAt = lockedAt;
    }

    public Date getLockedAt() {
        return lockedAt;
    }

    public WorkspaceUser getLockedBy() {
        return user;
    }

    public boolean isLocked() {
        return user != null;
    }

}
