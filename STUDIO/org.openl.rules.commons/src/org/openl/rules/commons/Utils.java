package org.openl.rules.commons;

import org.openl.rules.commons.util.FoldersOnlyFilter;
import org.openl.rules.commons.logs.CLog;

import java.io.FileFilter;
import java.io.File;

public class Utils {
    private static FileFilter foldersOnly;

    public static FileFilter getFoldersOnlyFilter() {
        if (foldersOnly == null) {
            foldersOnly = new FoldersOnlyFilter();
        }

        return foldersOnly;
    }

    public static void clearFolder(File folder) {
        CLog.log(CLog.DEBUG, "Clearing folder ''{0}''", folder);

        File[] files = folder.listFiles();

        // delete sub elements one by one
        for (File file : files) {
            if (file.isFile()) {
                if (!file.delete()) {
                    CLog.log(CLog.DEBUG, "Failed to delete file ''{0}''", file.getAbsolutePath());
                }
            } else {
                deleteFolder(file);
            }
        }
    }

    public static void deleteFolder(File folder) {
        CLog.log(CLog.DEBUG, "Deleting folder ''{0}''", folder);

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
                        CLog.log(CLog.DEBUG, "Failed to delete file ''{0}''", file.getAbsolutePath());
                    }
                }
            }

            if (!folder.delete()) {
                CLog.log(CLog.DEBUG, "Failed to delete folder ''{0}''", folder.getAbsolutePath());
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
}
