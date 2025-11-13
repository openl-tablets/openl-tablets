package org.openl.rules.workspace.dtr.impl;

/**
 * Immutable cache record for ProjectIndex with expiration tracking.
 * Combines the project index with its last update timestamp and provides
 * methods to check if the cache has expired.
 */
record ProjectIndexCache(ProjectIndex index, long lastUpdateTime) {
    private static final long INDEX_UPDATE_INTERVAL = 30 * 60 * 1000; // 30 minutes in milliseconds

    /**
     * Creates a new ProjectIndexCache with current timestamp.
     */
    ProjectIndexCache(ProjectIndex index) {
        this(index, System.currentTimeMillis());
    }

    /**
     * Checks if this cache entry has expired based on the 30-minute interval.
     *
     * @return true if more than 30 minutes have passed since last update
     */
    boolean isExpired() {
        return System.currentTimeMillis() - lastUpdateTime >= INDEX_UPDATE_INTERVAL;
    }

    /**
     * Returns a copy of the cached index.
     *
     * @return a defensive copy of the project index
     */
    ProjectIndex getCopy() {
        return index.copy();
    }
}
