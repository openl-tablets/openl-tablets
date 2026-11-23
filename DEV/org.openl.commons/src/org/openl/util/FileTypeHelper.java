package org.openl.util;

import java.util.Locale;

public final class FileTypeHelper {

    private FileTypeHelper() {
    }

    /**
     * Tells whether a file is an Excel workbook: {@code .xls}, {@code .xlsx} or {@code .xlsm}, but not the lock file
     * of one, see {@link #isLockFile(String)}.
     *
     * @param fileName the name of the file or its path, or {@code null}
     */
    public static boolean isExcelFile(String fileName) {
        if (fileName == null || isLockFile(FileUtils.getName(fileName))) {
            return false;
        }
        var lcFileName = fileName.toLowerCase(Locale.ROOT);
        return lcFileName.endsWith(".xls") || lcFileName.endsWith(".xlsx") || lcFileName.endsWith(".xlsm");
    }

    /**
     * Tells whether a file is the lock file Microsoft Office keeps beside a document while it is open, named after
     * the document with a {@code ~$} prefix, such as {@code ~$Rules.xlsx}.
     *
     * <p>This is the one rule the engine applies by itself, where no setting of OpenL Studio is known, such as in the
     * OpenL Maven plugin or in OpenL Rule Services. OpenL Studio ignores such files, along with the other system and
     * temporary files, by its {@code repository.ignored-paths} setting.
     *
     * @param fileName the name of the file
     */
    public static boolean isLockFile(String fileName) {
        return fileName.startsWith("~$");
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
