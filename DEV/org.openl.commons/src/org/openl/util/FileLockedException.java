package org.openl.util;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;

import lombok.Getter;

/**
 * A file cannot be changed because the file system refuses it.
 *
 * <p>Another program holds the file open, as Excel does on Windows with a workbook it shows, or the file is
 * read-only. The file stays as it is. A change succeeds once the file is closed there.
 */
@Getter
public class FileLockedException extends IOException {

    /** The name of the file, without the folders it is in. */
    private final String fileName;

    public FileLockedException(String fileName, Throwable cause) {
        super("The file '%s' cannot be changed.".formatted(fileName), cause);
        this.fileName = fileName;
    }

    /**
     * Tells a change of an existing file the file system refuses from the other failures of a change.
     *
     * @param refused the failure of the change
     * @param file    the file changed
     * @return the failure told as locked, naming the file, when the file exists; the failure itself otherwise
     */
    public static IOException lockedOr(FileSystemException refused, Path file) {
        var locked = Files.isRegularFile(file) && !(refused instanceof FileAlreadyExistsException);
        return locked ? new FileLockedException(String.valueOf(file.getFileName()), refused) : refused;
    }
}
