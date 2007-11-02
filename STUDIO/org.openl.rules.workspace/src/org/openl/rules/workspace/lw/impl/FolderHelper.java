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

    public static void clearFolder(File folder) {
        Log.debug("Clearing folder ''{0}''", folder);

        File[] files = folder.listFiles();

        // delete sub elements one by one
        for (File file : files) {
            if (file.isFile()) {
                if (!file.delete()) {
                    Log.debug("Failed to delete file ''{0}''", file.getAbsolutePath());
                }
            } else {
                deleteFolder(file);
            }
        }
    }

    public static void deleteFolder(File folder) {
        Log.debug("Deleting folder ''{0}''", folder);

        if (folder.isDirectory()) {
            // list sub elements
            File[] files = folder.listFiles();

            // delete one by one
            for (File file : files) {
                if (file.isDirectory()) {
                    // recursive
                    deleteFolder(folder);
                } else {
                    // delete file
                    if (!file.delete()) {
                        Log.debug("Failed to delete file ''{0}''", file.getAbsolutePath());
                    }
                }
            }

            if (!folder.delete()) {
                Log.debug("Failed to delete folder ''{0}''", folder.getAbsolutePath());
            }
        }
        // do nothing if folder is file
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

    /**
     * Lists folders only.
     */
    private static class FoldersOnlyFilter implements FileFilter {
        public boolean accept(File pathname) {
            return pathname.isDirectory();
        }
    }
}
