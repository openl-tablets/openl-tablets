package org.openl.rules.lock;

import java.nio.file.Path;

/**
 * Manages lock objects in the defined location. Usually, one lock manager is responsible for one category of resource.
 *
 * @author Yury Molchan
 */
public class LockManager {

    private final Path locksLocation;

    public LockManager(Path locksLocation) {
        this.locksLocation = locksLocation;
    }

    /**
     * Creates a lock object for the given ID.
     */
    public Lock getLock(String lockId) {
        return new Lock(locksLocation, lockId);
    }

}
