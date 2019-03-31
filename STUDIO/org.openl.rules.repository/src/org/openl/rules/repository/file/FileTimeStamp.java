package org.openl.rules.repository.file;

import java.io.File;

/**
 * Handles file modification.
 */
class FileTimeStamp {
    private final long lastModified;
    private final File file;

    FileTimeStamp(File file) {
        this.file = file;
        this.lastModified = file.lastModified();
    }

    /**
     * @return true if a file has been deleted or recreated to a directory or modified
     */
    boolean isModified() {
        return !file.exists() || file.isDirectory() || file.lastModified() != lastModified;
    }
}
