package org.openl.rules.workspace.dtr;

import java.util.Date;

import org.openl.rules.workspace.WorkspaceUser;

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
    WorkspaceUser getLockedBy();
    
    boolean isLocked();
}
