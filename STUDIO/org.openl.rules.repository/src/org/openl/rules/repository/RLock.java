package org.openl.rules.repository;

import java.util.Date;

import org.openl.rules.common.CommonUser;
import org.openl.rules.common.LockInfo;
import org.openl.rules.repository.exceptions.RRepositoryException;

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

        public void lock(CommonUser user) throws RRepositoryException {
            throw new UnsupportedOperationException();
        }

        public void unlock(CommonUser user) throws RRepositoryException {
            throw new UnsupportedOperationException();
        }
    };

    Date getLockedAt();

    CommonUser getLockedBy();

    boolean isLocked();
    
    void lock(CommonUser user) throws RRepositoryException;

    void unlock(CommonUser user) throws RRepositoryException;
}
