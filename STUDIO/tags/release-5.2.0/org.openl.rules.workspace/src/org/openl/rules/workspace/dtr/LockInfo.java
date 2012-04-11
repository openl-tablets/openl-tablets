package org.openl.rules.workspace.dtr;

import java.util.Date;

import org.openl.rules.workspace.WorkspaceUser;

/**
 * Information on Project Lock.
 *
 * @author Aleh Bykhavets
 *
 */
public interface LockInfo {
    /**
     * Get date/time when the lock was set.
     *
     * @return date when the lock was set
     */
    Date getLockedAt();

    /**
     * Get id of user who set the lock.
     *
     * @return user who set the lock
     */
    WorkspaceUser getLockedBy();

    boolean isLocked();
}
