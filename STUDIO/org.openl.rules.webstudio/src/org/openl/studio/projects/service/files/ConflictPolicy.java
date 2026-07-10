package org.openl.studio.projects.service.files;

/**
 * Policy for handling an uploaded entry whose target file already exists.
 *
 * @author Yury Molchan
 */
public enum ConflictPolicy {

    /**
     * Abort the whole upload if any entry collides with an existing file.
     */
    FAIL,

    /**
     * Replace the existing file with the uploaded entry.
     */
    OVERWRITE,

    /**
     * Keep the existing file and skip the uploaded entry.
     */
    SKIP,

    /**
     * Replace the whole target folder: the upload becomes its exact content, and existing
     * files absent from the upload are deleted.
     */
    REPLACE
}
