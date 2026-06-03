package org.openl.studio.projects.service.files;

import org.openl.util.FileUtils;

/**
 * Helpers for slash-separated, mount-relative file paths.
 *
 * <p>Centralizes the small path operations the files API repeats across services and mounts:
 * the trailing name, the parent directory and slash trimming.
 *
 * @author Yury Molchan
 */
final class FilePaths {

    private FilePaths() {
    }

    /**
     * Trailing file or folder name of the path. An empty or root-only path yields an empty name.
     */
    static String name(String path) {
        return FileUtils.getName(path);
    }

    /**
     * Parent directory of the path, or an empty string when the path has no parent.
     */
    static String parent(String path) {
        if (path == null) {
            return "";
        }
        int slash = path.lastIndexOf('/');
        return slash < 0 ? "" : path.substring(0, slash);
    }

    /**
     * Removes any leading slashes from the value. A {@code null} value is returned unchanged.
     */
    static String stripLeadingSlashes(String value) {
        if (value == null) {
            return null;
        }
        int start = 0;
        while (start < value.length() && value.charAt(start) == '/') {
            start++;
        }
        return value.substring(start);
    }

    /**
     * Removes leading and trailing slashes from the value. A {@code null} value becomes an empty string.
     */
    static String trimSlashes(String value) {
        if (value == null) {
            return "";
        }
        int start = 0;
        int end = value.length();
        while (start < end && value.charAt(start) == '/') {
            start++;
        }
        while (end > start && value.charAt(end - 1) == '/') {
            end--;
        }
        return value.substring(start, end);
    }
}
