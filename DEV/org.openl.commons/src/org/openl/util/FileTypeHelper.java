package org.openl.util;

import java.util.Locale;

public final class FileTypeHelper {

    private FileTypeHelper() {
    }

    public static boolean isExcelFile(String fileName) {
        if (fileName == null) {
            return false;
        }
        var lcFileName = fileName.toLowerCase(Locale.ROOT);
        return !lcFileName.startsWith("~$")
                && (lcFileName.endsWith(".xls") || lcFileName.endsWith(".xlsx") || lcFileName.endsWith(".xlsm"));
    }

    /**
     * Tells whether a file is a Markdown rules module.
     *
     * <p>The name may be a plain file name, a path or a URI.
     *
     * @param fileName the name of the file, or {@code null}
     * @return {@code true} when the name ends with {@code .md}
     */
    public static boolean isMarkdownFile(String fileName) {
        return fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(".md");
    }

    public static boolean isZipFile(String fileName) {
        if (fileName == null) {
            return false;
        }
        var lcFileName = fileName.toLowerCase(Locale.ROOT);
        return lcFileName.endsWith(".zip");
    }

    public static boolean isPossibleOpenAPIFile(String fileName) {
        if (fileName == null) {
            return false;
        }
        var lcFileName = fileName.toLowerCase(Locale.ROOT);
        return lcFileName.endsWith(".json") || lcFileName.endsWith(".yml") || lcFileName
                .endsWith(".yaml");
    }
}
