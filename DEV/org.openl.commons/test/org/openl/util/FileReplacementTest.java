package org.openl.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** A file is replaced whole or not at all, and nothing of the replacement is left beside it. */
class FileReplacementTest {

    @TempDir
    Path folder;

    @Test
    void theFileIsReplacedAndNothingIsLeftBeside() throws IOException {
        var file = Files.writeString(folder.resolve("Bank Rating.xlsx"), "as it was");

        FileReplacement.replace(file, out -> out.write(bytes("as it is now")), written -> {
            // Nothing to check.
        });

        assertEquals("as it is now", Files.readString(file));
        assertEquals(List.of(file), list());
    }

    @Test
    void aFileThatIsNotThereIsCreated() throws IOException {
        var file = folder.resolve("New.xlsx");

        FileReplacement.replace(file, out -> out.write(bytes("new")), written -> {
            // Nothing to check.
        });

        assertEquals("new", Files.readString(file));
    }

    @Test
    void theCheckSeesTheNewContentAndAFileItRefusesStaysAsItWas() throws IOException {
        var file = Files.writeString(folder.resolve("Bank Rating.xlsx"), "as it was");

        var refused = assertThrows(IOException.class, () -> FileReplacement.replace(file,
                out -> out.write(bytes("as it is now")), written -> {
                    assertEquals("as it is now", Files.readString(written));
                    assertEquals("as it was", Files.readString(file), "the file is untouched while it is written");
                    throw new IOException("changed outside");
                }));

        assertEquals("changed outside", refused.getMessage());
        assertEquals("as it was", Files.readString(file));
        assertEquals(List.of(file), list());
    }

    @Test
    void aTemporaryFileThatCannotBeRemovedHidesNotWhyTheReplacementFailed() throws IOException {
        var file = Files.writeString(folder.resolve("Bank Rating.xlsx"), "as it was");

        var refused = assertThrows(IOException.class, () -> FileReplacement.replace(file,
                out -> out.write(bytes("as it is now")), written -> {
                    // Something takes the place of the temporary file that no delete removes.
                    Files.delete(written);
                    Files.createFile(Files.createDirectories(written).resolve("held"));
                    throw new IOException("changed outside");
                }));

        assertEquals("changed outside", refused.getMessage());
        assertEquals("as it was", Files.readString(file));
    }

    @Test
    void aWriteThatFailsLeavesTheFileAsItWas() throws IOException {
        var file = Files.writeString(folder.resolve("Bank Rating.xlsx"), "as it was");

        assertThrows(OutOfMemoryError.class, () -> FileReplacement.replace(file, out -> {
            out.write(bytes("half"));
            throw new OutOfMemoryError();
        }, written -> {
            // Nothing to check.
        }));

        assertEquals("as it was", Files.readString(file));
        assertEquals(List.of(file), list());
    }

    @Test
    void aReadOnlyFileIsNotReplaced() throws IOException {
        var file = Files.writeString(folder.resolve("Bank Rating.xlsx"), "as it was");
        assertTrue(file.toFile().setWritable(false));
        try {
            // An administrator writes a read-only file all the same: there is nothing to refuse then.
            assumeFalse(Files.isWritable(file));

            assertThrows(AccessDeniedException.class, () -> FileReplacement.replace(file,
                    out -> out.write(bytes("as it is now")), written -> {
                        // Nothing to check.
                    }));

            assertEquals("as it was", Files.readString(file));
        } finally {
            assertTrue(file.toFile().setWritable(true));
        }
    }

    @Test
    void theFileKeepsItsPermissions() throws IOException {
        assumeTrue(FileSystems.getDefault().supportedFileAttributeViews().contains("posix"));
        var file = Files.writeString(folder.resolve("Bank Rating.xlsx"), "as it was");
        var permissions = PosixFilePermissions.fromString("rw-rw----");
        Files.setPosixFilePermissions(file, permissions);

        FileReplacement.replace(file, out -> out.write(bytes("as it is now")), written -> {
            // Nothing to check.
        });

        assertEquals(permissions, Files.getPosixFilePermissions(file));
    }

    @Test
    void aFileReachedThroughALinkIsReplacedWhereTheLinkPoints() throws IOException {
        var file = Files.writeString(Files.createDirectory(folder.resolve("shared")).resolve("Rates.xlsx"), "as it was");
        Path link;
        try {
            link = Files.createSymbolicLink(folder.resolve("Rates.xlsx"), file);
        } catch (UnsupportedOperationException | IOException e) {
            assumeTrue(false, "the file system has no symbolic links for this user");
            return;
        }

        FileReplacement.replace(link, out -> out.write(bytes("as it is now")), written -> {
            // Nothing to check.
        });

        assertTrue(Files.isSymbolicLink(link), "the link stays a link");
        assertEquals("as it is now", Files.readString(file));
    }

    @Test
    void aTemporaryFileLeftBehindIsTakenOverAndOneOfAWriteUnderWayIsLeftAlone() throws IOException {
        var file = Files.writeString(folder.resolve("Bank Rating.xlsx"), "as it was");
        var temporary = folder.resolve(".Bank Rating.xlsx" + FileReplacement.TEMPORARY_SUFFIX);
        Files.writeString(temporary, "of a write under way");

        FileReplacement.replace(file, out -> out.write(bytes("first")), written -> {
            // Nothing to check.
        });

        assertEquals("first", Files.readString(file));
        assertEquals("of a write under way", Files.readString(temporary), "a recent temporary file is not taken");

        Files.setLastModifiedTime(temporary, FileTime.from(Instant.now().minus(Duration.ofDays(1))));
        FileReplacement.replace(file, out -> out.write(bytes("second")), written -> {
            // Nothing to check.
        });

        assertEquals("second", Files.readString(file));
        assertEquals(List.of(file), list(), "a temporary file left behind long ago is taken over and removed");
    }

    @Test
    void temporaryFilesOfAnyNameLeftBehindAreRemoved() throws IOException {
        var file = Files.writeString(folder.resolve("Bank Rating.xlsx"), "as it was");
        for (var name : List.of(".Bank Rating.xlsx", ".Bank Rating.xlsx.0a1b")) {
            var leftover = Files.writeString(folder.resolve(name + FileReplacement.TEMPORARY_SUFFIX),
                    "of a write that stopped");
            Files.setLastModifiedTime(leftover, FileTime.from(Instant.now().minus(Duration.ofDays(1))));
        }

        FileReplacement.replace(file, out -> out.write(bytes("as it is now")), written -> {
            // Nothing to check.
        });

        assertEquals(List.of(file), list());
    }

    @Test
    void aFileInAFolderThatTakesNoNewFileIsWrittenOverInPlace() throws IOException {
        assumeTrue(FileSystems.getDefault().supportedFileAttributeViews().contains("posix"));
        var shared = Files.createDirectory(folder.resolve("shared"));
        var file = Files.writeString(shared.resolve("Bank Rating.xlsx"), "as it was");
        var link = Files.createLink(folder.resolve("Rates.xlsx"), file);
        var permissions = Files.getPosixFilePermissions(shared);
        Files.setPosixFilePermissions(shared, PosixFilePermissions.fromString("r-xr-xr-x"));
        try {
            // An administrator creates files in a read-only folder all the same: there is nothing to fall back from.
            assumeFalse(Files.isWritable(shared));

            FileReplacement.replace(file, out -> out.write(bytes("as it is now")), written -> {
                assertEquals("as it is now", Files.readString(written));
                assertEquals("as it was", Files.readString(file), "the file is untouched while it is written");
            });

            assertEquals("as it is now", Files.readString(file));
            assertEquals("as it is now", Files.readString(link), "the file stays the same file");
        } finally {
            Files.setPosixFilePermissions(shared, permissions);
        }
    }

    /** A file that is neither replaced nor written over stays as it is, and the refusal of the replacement is told. */
    @Test
    void aFileThatCannotBeWrittenOverEitherStaysAsItIs() throws IOException {
        // A folder of files in the place of the file: no file takes its place, and it opens for no writing.
        var file = Files.createDirectory(folder.resolve("Bank Rating.xlsx"));
        Files.writeString(file.resolve("kept.txt"), "kept");

        var refused = assertThrows(FileSystemException.class, () -> FileReplacement.replace(file,
                out -> out.write(bytes("as it is now")), written -> {
                    // Nothing to check.
                }));

        assertEquals(1, refused.getSuppressed().length, "the write over the file failed as well");
        assertEquals("kept", Files.readString(file.resolve("kept.txt")));
        assertEquals(List.of(file), list(), "nothing of the replacement is left beside it");
    }

    @Test
    void aTemporaryFileIsHiddenAndEndsWithItsSuffix() {
        assertTrue(FileReplacement.isTemporary(folder.resolve(".Bank Rating.xlsx" + FileReplacement.TEMPORARY_SUFFIX)));
        assertFalse(FileReplacement.isTemporary(folder.resolve("Bank Rating.xlsx" + FileReplacement.TEMPORARY_SUFFIX)));
        assertFalse(FileReplacement.isTemporary(folder.resolve(".Bank Rating.xlsx")));
    }

    private List<Path> list() throws IOException {
        try (var files = Files.list(folder)) {
            return files.sorted().toList();
        }
    }

    private static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }
}
