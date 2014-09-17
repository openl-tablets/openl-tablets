package org.openl.rules.repository;

import java.io.File;
import java.io.FileFilter;

/**
 * Folder (File System) Helper for Local Workspace.
 *
 * @author Aleh Bykhavets
 *
 */
public final class FolderHelper {
    private FolderHelper() {
    }

    /**
     * Lists folders only.
     */
    private static class FoldersOnlyFilter implements FileFilter {
        public boolean accept(File pathname) {
            return pathname.isDirectory();
        }
    }

    private static FileFilter foldersOnly;

    public static boolean checkOrCreateFolder(File location) {
        return location.exists() || location.mkdirs();
    }

    public static boolean clearFolder(File folder) {
        File[] files = folder.listFiles();

        if (files == null) {
            return true;
        }

        boolean failures = false;
        // delete sub elements one by one
        for (File file : files) {
            if (file.isFile()) {
                if (!file.delete()) {
                    failures = true;
                }
            } else {
                if (!deleteFolder(file)) {
                    failures = true;
                }
            }
        }

        return !failures;
    }

    public static boolean deleteFolder(File folder) {
        boolean failures = false;
        if (folder.isDirectory()) {
            // list sub elements
            File[] files = folder.listFiles();

            if (files == null) {
                // Some I/O error occurs
                return false;
            }

            // delete one by one
            for (File f : files) {
                if (f.isDirectory()) {
                    // recursive
                    if (!deleteFolder(f)) {
                        failures = true;
                    }
                } else {
                    // delete file
                    if (!f.delete()) {
                        failures = true;
                    }
                }
            }

            if (!folder.delete()) {
                failures = true;
            }
        }

        return !failures;
    }

    public static File generateSubLocation(File location, String name) {
        return new File(location, name);
    }

    public static FileFilter getFoldersOnlyFilter() {
        if (foldersOnly == null) {
            foldersOnly = new FoldersOnlyFilter();
        }

        return foldersOnly;
    }

    public static boolean isParent(File parent, File child) {
        return child != null && (child.equals(parent) || isParent(parent, child.getParentFile()));
    }
}
