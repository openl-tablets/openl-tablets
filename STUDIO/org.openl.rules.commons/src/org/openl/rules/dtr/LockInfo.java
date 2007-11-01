package org.openl.rules.dtr;

import java.util.Date;

public interface LockInfo {
    Date getLockedAt();
    String getLockedBy();
}
