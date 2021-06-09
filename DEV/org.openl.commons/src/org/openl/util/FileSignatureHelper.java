package org.openl.util;

public final class FileSignatureHelper {

    private static final int REGULAR_ARCHIVE_FILE_SIGN = 0x504B0304;
    private static final int EMPTY_ARCHIVE_FILE_SIGN = 0x504B0506;

    private FileSignatureHelper() {
    }

    /**
     * Verifies if first 4 bytes matches archive signature
     *
     * @see <a href="https://en.wikipedia.org/wiki/List_of_file_signatures">List of file signatures</a>
     *
     * @param sign first 4 bytes
     * @return {@code true} if it's archive, otherwise {@code false}
     */
    public static boolean isArchiveSign(int sign) {
        return sign == REGULAR_ARCHIVE_FILE_SIGN || isEmptyArchive(sign);
    }

    public static boolean isEmptyArchive(int sing) {
        return sing == EMPTY_ARCHIVE_FILE_SIGN;
    }

}
