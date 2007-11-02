package org.openl.rules.workspace.dtr.impl;

import org.openl.rules.workspace.dtr.LockInfo;

import java.util.Date;

public class LockInfoImpl implements LockInfo {
    private Date lockedAt;
    private String lockedBy;

    public LockInfoImpl(Date lockedAt, String lockedBy) {
        this.lockedAt = lockedAt;
        this.lockedBy = lockedBy;
    }

    public Date getLockedAt() {
        return lockedAt;
    }

    public String getLockedBy() {
        return lockedBy;
    }
}
