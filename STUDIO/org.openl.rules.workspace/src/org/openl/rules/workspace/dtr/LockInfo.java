package org.openl.rules.workspace.dtr;

import java.util.Date;

public interface LockInfo {
    /**
     * Get date/time when lock was set.
     * 
     * @return
     */
    Date getLockedAt();
    
    /**
     * Get id of user who set the lock.
     * 
     * @return
     */
    String getLockedBy();
}
