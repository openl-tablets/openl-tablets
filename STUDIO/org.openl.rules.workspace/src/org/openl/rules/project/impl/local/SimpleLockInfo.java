package org.openl.rules.project.impl.local;

import java.util.Date;

import org.openl.rules.common.CommonUser;
import org.openl.rules.common.LockInfo;
import org.openl.rules.workspace.WorkspaceUserImpl;

public class SimpleLockInfo implements LockInfo {
    public static final SimpleLockInfo NO_LOCK = new SimpleLockInfo(false, null, null);

    private final boolean locked;
    private final Date date;
    private final String userName;

    public SimpleLockInfo(boolean locked, Date date, String userName) {
        this.locked = locked;
        this.date = date;
        this.userName = userName;
    }

    @Override
    public Date getLockedAt() {
        return date;
    }

    @Override
    public CommonUser getLockedBy() {
        return new WorkspaceUserImpl(userName);
    }

    @Override
    public boolean isLocked() {
        return locked;
    }
}
