package org.openl.util;

public final class FileTypeHelper {

    private FileTypeHelper() {
    }
    
    public static boolean isExcelFile(String fileName) {
        if (fileName == null) {
            return false;
        }
        String lcFileName = fileName.toLowerCase();
        return lcFileName.endsWith(".xls") || lcFileName.endsWith(".xlsx") || lcFileName.endsWith(".xlsm");
    }

    public static boolean isZipFile(String fileName) {
        if (fileName == null) {
            return false;
        }
        String lcFileName = fileName.toLowerCase();
        return lcFileName.endsWith(".zip");
    }
}
