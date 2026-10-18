package org.openl.studio.projects.service.files;

/**
 * What a write of a single file did with the file at its path.
 *
 * @author Yury Molchan
 */
public enum FileWriteOutcome {

    /**
     * No file was there, and the file is created.
     */
    CREATED,

    /**
     * A file was there, and its content is replaced.
     */
    REPLACED,

    /**
     * A file was there, and it is left as it is.
     */
    KEPT
}
