package org.openl.rules.workspace.lw.impl;

import org.openl.util.Log;

import java.io.FileFilter;
import java.io.File;
import java.io.FilenameFilter;

/**
 * Folder (File System) Helper for Local Workspace.
 * 
 * @author Aleh Bykhavets
 *
 */
public class FolderHelper {
    public static final String PROPERTIES_FOLDER = ".studioProps";
    public static final String FOLDER_PROPERTIES_FOLDER = "folder-props";
    public static final String FOLDER_PROPERTIES_FILE = "folder.props";
    public static final String RESOURCE_PROPERTIES_EXT = ".props";

    private static FileFilter foldersOnly;
    private static FilenameFilter localFilesFilter;

    public static FileFilter getFoldersOnlyFilter() {
        if (foldersOnly == null) {
            foldersOnly = new FoldersOnlyFilter();
        }

        return foldersOnly;
    }
    
    public static FilenameFilter getLocalFilesFilter() {
        if (localFilesFilter == null) {
            localFilesFilter = new LocalFilesFilter();
        }

        return localFilesFilter;
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

    public static File getFolderPropertiesFile(File propFolder) {
        return new File(propFolder, FOLDER_PROPERTIES_FOLDER + File.separator + FOLDER_PROPERTIES_FILE);
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

    private static boolean isSpecialName(String name) {
        if (".svn".equals(name)) return true;
        if ("CVS".equals(name)) return true;

        if (PROPERTIES_FOLDER.equals(name)) return true;

        return false;
    }

    /**
     * Lists folders only.
     */
    private static class FoldersOnlyFilter implements FileFilter {
        public boolean accept(File pathname) {
            if (pathname.isDirectory()) {
                String name = pathname.getName();

                // reject special directories
                if (isSpecialName(name)) return false;

                // accept directory
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * Lists folders and files, excluding special folders
     */
    private static class LocalFilesFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            // reject special directories
            return !(isSpecialName(name));
        }
    };
}
