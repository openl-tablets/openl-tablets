package org.openl.rules.workspace.lw.impl;

import org.openl.util.Log;

import java.io.FileFilter;
import java.io.File;

/**
 * Folder (File System) Helper for Local Workspace.
 * 
 * @author Aleh Bykhavets
 *
 */
public class FolderHelper {
    private static FileFilter foldersOnly;

    public static FileFilter getFoldersOnlyFilter() {
        if (foldersOnly == null) {
            foldersOnly = new FoldersOnlyFilter();
        }

        return foldersOnly;
    }

    public static boolean clearFolder(File folder) {
        Log.debug("Clearing folder ''{0}''", folder);

        File[] files = folder.listFiles();
        
        if (files == null) return true;

        boolean failures = false;
        // delete sub elements one by one
        for (File file : files) {
            if (file.isFile()) {
                if (!file.delete()) {
                    failures = true;
                    Log.debug("Failed to delete file ''{0}''", file.getAbsolutePath());
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
        Log.debug("Deleting folder ''{0}''", folder);

        boolean failures = false;
        if (folder.isDirectory()) {
            // list sub elements
            File[] files = folder.listFiles();

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
                        Log.debug("Failed to delete file ''{0}''", f.getAbsolutePath());
                    }
                }
            }

            if (!folder.delete()) {
                Log.debug("Failed to delete folder ''{0}''", folder.getAbsolutePath());
            }
        }

        return !failures;
    }

    public static File generateSubLocation(File location, String name) {
        return new File(location, name);
    }

    public static boolean checkOrCreateFolder(File location) {
        if (location.exists()) {
            // ok
            return true;
        } else {
            return location.mkdirs();
        }
    }

    public static boolean isParent(File parent, File child) {
        return child != null && (child.equals(parent) || isParent(parent, child.getParentFile()));
    }

    /**
     * Lists folders only.
     */
    private static class FoldersOnlyFilter implements FileFilter {
        public boolean accept(File pathname) {
            if (pathname.isDirectory()) {
                String name = pathname.getName();

                // reject special directories
                if (".svn".equalsIgnoreCase(name)) return false;
                if (".cvs".equalsIgnoreCase(name)) return false;

                // accept directory
                return true;
            } else {
                return false;
            }
        }
    }
}
