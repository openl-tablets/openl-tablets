package org.openl.studio.audit.service;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * Keeps the snapshot of the settings the deployment gave OpenL Studio at its last start: a fingerprint of each value,
 * by property, never a value itself.
 */
public interface SettingsSnapshotStore {

    /**
     * The snapshot of the last start, or nothing before the first one.
     */
    Optional<Map<String, String>> load() throws IOException;

    /**
     * Keeps the given snapshot in place of the one of the last start.
     */
    void save(Map<String, String> snapshot) throws IOException;
}
