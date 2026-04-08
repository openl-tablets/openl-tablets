package org.openl.util;

import java.util.Locale;

public final class FileTypeHelper {

    private FileTypeHelper() {
    }

    public static boolean isExcelFile(String fileName) {
        if (fileName == null) {
            return false;
        }
        String lcFileName = fileName.toLowerCase(Locale.ROOT);
        return !lcFileName.startsWith("~$")
                && (lcFileName.endsWith(".xls") || lcFileName.endsWith(".xlsx") || lcFileName.endsWith(".xlsm"));
    }

    public static boolean isZipFile(String fileName) {
        if (fileName == null) {
            return false;
        }
        String lcFileName = fileName.toLowerCase(Locale.ROOT);
        return lcFileName.endsWith(".zip");
    }

    public static boolean isPossibleOpenAPIFile(String fileName) {
        if (fileName == null) {
            return false;
        }
        String lcFileName = fileName.toLowerCase(Locale.ROOT);
        return lcFileName.endsWith(".json") || lcFileName.endsWith(".yml") || lcFileName
                .endsWith(".yaml");
    }
}
