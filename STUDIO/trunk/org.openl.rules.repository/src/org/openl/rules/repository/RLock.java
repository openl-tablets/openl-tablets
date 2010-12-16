package org.openl.rules.repository;

import java.util.Date;

import org.openl.rules.common.CommonUser;
import org.openl.rules.common.LockInfo;

public interface RLock extends LockInfo{
    public final static RLock NO_LOCK = new RLock() {
        public Date getLockedAt() {
            return null;
        }

        public CommonUser getLockedBy() {
            return null;
        }

        public boolean isLocked() {
            return false;
        }
    };

    Date getLockedAt();

    CommonUser getLockedBy();

    boolean isLocked();
}
