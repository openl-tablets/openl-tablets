package org.openl.studio.projects.service.files;

/**
 * A file staged for a write: its mount-relative path and content bytes.
 *
 * @author Yury Molchan
 */
record FileEntry(String fullPath, byte[] data) {
}
