package org.openl.studio.projects.service.files;

import java.util.Arrays;
import java.util.Objects;

/**
 * A file staged for a write: its mount-relative path and content bytes.
 *
 * @author Yury Molchan
 */
record FileEntry(String fullPath, byte[] data) {

    @Override
    public boolean equals(Object obj) {
        return obj instanceof FileEntry(var otherFullPath, var otherData)
                && Objects.equals(fullPath, otherFullPath)
                && Arrays.equals(data, otherData);
    }

    @Override
    public int hashCode() {
        return 31 * Objects.hashCode(fullPath) + Arrays.hashCode(data);
    }

    @Override
    public String toString() {
        return "FileEntry[fullPath=%s, data=%d bytes]".formatted(fullPath, data == null ? 0 : data.length);
    }
}
