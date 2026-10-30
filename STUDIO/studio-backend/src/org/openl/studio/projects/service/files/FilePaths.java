package org.openl.studio.projects.service.files;

import java.util.Arrays;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import org.openl.util.FileUtils;

/**
 * Helpers for slash-separated, mount-relative file paths.
 *
 * <p>Centralizes the small path operations the files API repeats across services and mounts:
 * the trailing name, the parent directory, joining and slash trimming.
 *
 * @author Yury Molchan
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FilePaths {

    /**
     * Trailing file or folder name of the path. An empty or root-only path yields an empty name.
     */
    static String name(String path) {
        return FileUtils.getName(path);
    }

    /**
     * Parent directory of the path, or an empty string when the path has no parent.
     */
    public static String parent(@Nullable String path) {
        if (path == null) {
            return "";
        }
        var slash = path.lastIndexOf('/');
        return slash < 0 ? "" : path.substring(0, slash);
    }

    /**
     * Names of the folders that hold the file at the path, from the outermost one. A path without a parent
     * yields no names.
     */
    static String[] parentFolders(String path) {
        var segments = path.split("/");
        return Arrays.copyOf(segments, segments.length - 1);
    }

    /**
     * The path of a file inside a folder that holds it. Leading slashes of both paths are ignored, and a path the
     * folder does not hold is returned as it is, without its leading slashes.
     */
    public static String inside(String folder, String path) {
        var prefix = stripLeadingSlashes(folder) + '/';
        var file = stripLeadingSlashes(path);
        return file.startsWith(prefix) ? file.substring(prefix.length()) : file;
    }

    /**
     * Appends the child path to the parent directory. An empty parent yields the child path as it is.
     */
    static String join(String parent, String child) {
        return parent.isEmpty() ? child : parent + '/' + child;
    }

    /**
     * Removes any leading slashes from the value.
     */
    static String stripLeadingSlashes(String value) {
        var start = 0;
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
        var start = 0;
        var end = value.length();
        while (start < end && value.charAt(start) == '/') {
            start++;
        }
        while (end > start && value.charAt(end - 1) == '/') {
            end--;
        }
        return value.substring(start, end);
    }
}
