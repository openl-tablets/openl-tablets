package org.openl.rules.repository;

import java.util.Date;

public interface RLock {
    boolean isLocked();
    Date getLockedAt();
    CommonUser getLockedBy();
}
