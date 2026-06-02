package org.openl.studio.projects.service.files;

/**
 * Policy for handling an archive entry whose target file already exists.
 *
 * @author Yury Molchan
 */
public enum ConflictPolicy {

    /**
     * Abort the whole upload if any entry collides with an existing file.
     */
    FAIL,

    /**
     * Replace the existing file with the archive entry.
     */
    OVERWRITE,

    /**
     * Keep the existing file and skip the archive entry.
     */
    SKIP
}
