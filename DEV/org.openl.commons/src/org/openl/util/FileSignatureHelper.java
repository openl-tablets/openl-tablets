package org.openl.util;

public final class FileSignatureHelper {

    private static final int REGULAR_ARCHIVE_FILE_SIGN = 0x504B0304;
    private static final int EMPTY_ARCHIVE_FILE_SIGN = 0x504B0506;
    private static final int OLE2_COMPOUND_DOC_SIGN = 0xD0CF11E0;

    private FileSignatureHelper() {
    }

    /**
     * Verifies if first 4 bytes matches archive signature
     *
     * @param sign first 4 bytes
     * @return {@code true} if it's archive, otherwise {@code false}
     * @see <a href="https://en.wikipedia.org/wiki/List_of_file_signatures">List of file signatures</a>
     */
    public static boolean isArchiveSign(int sign) {
        return sign == REGULAR_ARCHIVE_FILE_SIGN || isEmptyArchive(sign);
    }

    public static boolean isEmptyArchive(int sing) {
        return sing == EMPTY_ARCHIVE_FILE_SIGN;
    }

    /**
     * Verifies if first 4 bytes matches OLE2 Compound Document signature (legacy .xls format)
     *
     * @param sign first 4 bytes
     * @return {@code true} if it's OLE2 compound document, otherwise {@code false}
     * @see <a href="https://en.wikipedia.org/wiki/List_of_file_signatures">List of file signatures</a>
     */
    public static boolean isOle2Sign(int sign) {
        return sign == OLE2_COMPOUND_DOC_SIGN;
    }

}
