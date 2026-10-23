package org.openl.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** A change of an existing file the file system refuses is told as locked, and any other failure as it is. */
class FileLockedExceptionTest {

    @TempDir
    Path folder;

    @Test
    void aRefusedChangeOfAnExistingFileIsLockedNamingTheFile() throws IOException {
        var file = Files.writeString(folder.resolve("Bank Rating.xlsx"), "held open");
        var refused = new FileSystemException(file.toString());

        var locked = assertInstanceOf(FileLockedException.class, FileLockedException.lockedOr(refused, file));

        assertEquals("Bank Rating.xlsx", locked.getFileName());
        assertSame(refused, locked.getCause());
    }

    @Test
    void anyOtherFailureIsToldAsItIs() throws IOException {
        var missing = folder.resolve("Missing.xlsx");
        var notThere = new FileSystemException(missing.toString());
        var file = Files.writeString(folder.resolve("Bank Rating.xlsx"), "there");
        var alreadyThere = new FileAlreadyExistsException(file.toString());

        assertSame(notThere, FileLockedException.lockedOr(notThere, missing), "no file to be locked");
        assertSame(alreadyThere, FileLockedException.lockedOr(alreadyThere, file), "a file in the way is no lock");
    }
}
