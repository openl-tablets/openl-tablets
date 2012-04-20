package org.openl.rules.workspace.lw.impl;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.util.MsgHelper;

/**
 * Folder (File System) Helper for Local Workspace.
 *
 * @author Aleh Bykhavets
 */
public class FolderHelper {

    public static final String PROPERTIES_FOLDER = ".studioProps";
    public static final String FOLDER_PROPERTIES_FILE = "..studioProps.folder";
    public static final String RESOURCE_PROPERTIES_EXT = ".props";

    public static boolean checkOrCreateFolder(File location) {
        if (location.exists()) {
            // ok
            return true;
        } else {
            return location.mkdirs();
        }
    }

    /**
     * Clears the folder's content.
     * The folder itself will not be deleted.
     * If you want to delete a folder itself, use {@link #deleteFolder(File)} instead
     * 
     * @param folder folder, which content will be deleted
     * @return true if all content is deleted and false if at least one file or sub-folder cannot be deleted.
     */
    public static boolean clearFolder(File folder) {
    	final Log log = LogFactory.getLog(FolderHelper.class);
        if (log.isDebugEnabled()) {
            log.debug(MsgHelper.format("Clearing folder ''{0}''", folder));
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
                    if (log.isDebugEnabled()) {
                        log.debug(MsgHelper.format("Failed to delete file ''{0}''!", file.getAbsolutePath()));
                    }
                }
            } else {
                if (!deleteFolder(file)) {
                    failures = true;
                }
            }
        }

        return !failures;
    }

    /**
     * Deletes the folder and it's content recursively.
     * If you do not want to delete a folder itself, use {@link #clearFolder(File)} instead
     * 
     * @param folder the folder, that will be deleted
     * @return true if the folder is deleted and false if the folder cannot be deleted.
     */
    public static boolean deleteFolder(File folder) {
    	final Log log = LogFactory.getLog(FolderHelper.class);
        if (log.isDebugEnabled()) {
            log.debug(MsgHelper.format("Deleting folder ''{0}''", folder));
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
                        if (log.isDebugEnabled()) {
                            log.debug(MsgHelper.format("Failed to delete file ''{0}''!", f.getAbsolutePath()));
                        }
                    }
                }
            }

            if (!folder.delete()) {
                 failures = true;
                 if (log.isDebugEnabled()) {
                    log.debug(MsgHelper.format("Failed to delete folder ''{0}''!", folder.getAbsolutePath()));
                }
            }
        }

        return !failures;
    }

    public static File generateSubLocation(File location, String name) {
        return new File(location, name);
    }

    public static boolean isParent(File parent, File child) {
        return (child != null) && (child.equals(parent) || isParent(parent, child.getParentFile()));
    }
}
