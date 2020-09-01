package org.openl.rules.lock;

import java.util.Date;

/**
 * Lock description.
 *
 * @author Yury Molchan
 *
 */
public class LockInfo {

    public static final LockInfo NO_LOCK = new LockInfo(null, null);

    private final Date date;
    private final String userName;

    public LockInfo(Date date, String userName) {
        this.date = date;
        this.userName = userName;
    }

    /**
     * Get date/time when the lock was set.
     *
     * @return date when the lock was set
     */
    public Date getLockedAt() {
        return date;
    }

    /**
     * Returns an identification who or what sets the lock.
     */
    public String getLockedBy() {
        return userName;
    }

    public boolean isLocked() {
        return date != null;
    }
}
