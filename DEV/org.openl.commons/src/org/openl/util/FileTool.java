package org.openl.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermissions;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

/**
 * Creates files and directories in the temporary directory that only their owner can use.
 *
 * <p>The temporary directory is shared by every user of the machine. Where the file system tracks POSIX
 * permissions, a file created here can be read and written by its owner alone, and a directory entered by its
 * owner alone. Elsewhere the defaults of the file system apply.
 */
@Slf4j
public final class FileTool {

    private static final FileAttribute<?>[] OWNER_ONLY_FILE = ownerOnly("rw-------");
    private static final FileAttribute<?>[] OWNER_ONLY_DIRECTORY = ownerOnly("rwx------");

    private FileTool() {
        // Utility class
    }

    /**
     * Creates an empty file in the temporary directory.
     *
     * @param prefix the start of the file name
     * @param suffix the end of the file name, {@code .tmp} when {@code null}
     * @return the new file
     * @throws IOException when the file cannot be created
     */
    public static Path createTempFile(String prefix, @Nullable String suffix) throws IOException {
        return Files.createTempFile(prefix, suffix, OWNER_ONLY_FILE);
    }

    /**
     * Creates an empty directory in the temporary directory.
     *
     * @param prefix the start of the directory name
     * @return the new directory
     * @throws IOException when the directory cannot be created
     */
    public static Path createTempDirectory(String prefix) throws IOException {
        return Files.createTempDirectory(prefix, OWNER_ONLY_DIRECTORY);
    }

    /**
     * Copies a stream into a new file in the temporary directory and closes the stream.
     *
     * <p>The failure is logged and {@code null} is returned when the file cannot be created or written.
     *
     * @param source   the content of the file
     * @param fileName the start of the file name
     * @return the new file, or {@code null} when it could not be written
     */
    public static @Nullable File toTempFile(InputStream source, String fileName) {
        try {
            var file = createTempFile(fileName, null);
            IOUtils.copyAndClose(source, Files.newOutputStream(file));
            return file.toFile();
        } catch (IOException e) {
            log.error("Failed to create a file: {}", fileName, e);
            return null;
        }
    }

    private static FileAttribute<?>[] ownerOnly(String permissions) {
        if (!FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
            return new FileAttribute<?>[0];
        }
        return new FileAttribute<?>[]{PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString(permissions))};
    }
}
