package org.openl.rules.repository;

import java.util.Date;

public interface RLock {
    Date getLockedAt();

    CommonUser getLockedBy();

    boolean isLocked();
}
