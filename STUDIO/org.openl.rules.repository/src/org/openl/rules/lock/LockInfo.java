package org.openl.rules.lock;

import java.util.Date;

/**
 * Lock description.
 *
 * @author Yury Molchan
 *
 */
public class LockInfo {

    public static final LockInfo NO_LOCK = new LockInfo(false, null, null);

    private final boolean locked;
    private final Date date;
    private final String userName;

    public LockInfo(boolean locked, Date date, String userName) {
        this.locked = locked;
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

    public String getLockedBy() {
        return userName;
    }

    public boolean isLocked() {
        return locked;
    }
}
