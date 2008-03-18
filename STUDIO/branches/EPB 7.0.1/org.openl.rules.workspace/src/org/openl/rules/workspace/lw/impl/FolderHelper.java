package org.openl.rules.workspace.lw.impl;

import org.openl.util.Log;

import java.io.File;


/**
 * Folder (File System) Helper for Local Workspace.
 *
 * @author Aleh Bykhavets
 */
public class FolderHelper {
    public static final String PROPERTIES_FOLDER = ".studioProps";
    public static final String FOLDER_PROPERTIES_FILE = "..studioProps.folder";
    public static final String RESOURCE_PROPERTIES_EXT = ".props";

    public static boolean clearFolder(File folder) {
        if (Log.isDebugEnabled()) {
            Log.debug("Clearing folder ''{0}''", folder);
        }

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
        if (Log.isDebugEnabled()) {
            Log.debug("Deleting folder ''{0}''", folder);
        }

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
        return (child != null)
            && (child.equals(parent) || isParent(parent, child.getParentFile()));
    }
}
