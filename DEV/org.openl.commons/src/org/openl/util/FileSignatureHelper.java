package org.openl.util;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FileSignatureHelper {

    private static final int REGULAR_ARCHIVE_FILE_SIGN = 0x504B0304;
    private static final int EMPTY_ARCHIVE_FILE_SIGN = 0x504B0506;

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
     * Reads the signature of a file: its first 4 bytes.
     *
     * <p>The file may be in any file system, such as an entry of a zip archive.
     *
     * @param path the file to read
     * @return the signature, or {@code -1} when the file cannot be read or is shorter than 4 bytes
     */
    public static int readSignature(Path path) {
        try (var in = new DataInputStream(Files.newInputStream(path))) {
            return in.readInt();
        } catch (IOException ignored) {
            return -1;
        }
    }

}
