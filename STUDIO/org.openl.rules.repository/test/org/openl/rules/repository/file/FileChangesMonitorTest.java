package org.openl.rules.repository.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import org.openl.util.FileReplacement;

class FileChangesMonitorTest {

    private static final FileTime WRITTEN = FileTime.fromMillis(1_700_000_000_000L);

    /** Every folder is watched. */
    private static final Predicate<Path> ALL = folder -> true;

    @TempDir
    Path root;

    @Test
    void doesNotWatchTheIgnoredFolders() throws IOException {
        write("project/rules.xml", "<project/>");
        write(".git/HEAD", "ref: refs/heads/main");
        var repository = new FileSystemRepository();
        repository.setRoot(root);
        repository.setIgnoredPaths("**/target/**");
        var monitor = new FileChangesMonitor(root, repository::isListed);
        var revision = monitor.getRevision();

        write(".git/objects/ab/cdef", "object");
        write("project/target/Rules.zip", "build");

        assertEquals(revision, monitor.getRevision());

        write("project/pom.xml", "<project/>");

        assertNotEquals(revision, monitor.getRevision());
    }

    @Test
    void keepsTheRevisionWhileNothingChanges() throws IOException {
        write("project/rules.xml", "<project/>");
        var monitor = new FileChangesMonitor(root, ALL);

        var revision = monitor.getRevision();

        assertEquals(revision, monitor.getRevision());
        assertEquals(revision, monitor.getRevision());
    }

    @Test
    void tellsAFileAddedInANestedFolder() throws IOException {
        write("project/rules.xml", "<project/>");
        var monitor = new FileChangesMonitor(root, ALL);
        var revision = monitor.getRevision();

        write("project/rules/Bank Rating.xlsx", "workbook");

        assertNotEquals(revision, monitor.getRevision());
    }

    @Test
    void tellsAFileRemoved() throws IOException {
        write("project/rules.xml", "<project/>");
        write("project/rules/Bank Rating.xlsx", "workbook");
        var monitor = new FileChangesMonitor(root, ALL);
        var revision = monitor.getRevision();

        Files.delete(root.resolve("project/rules/Bank Rating.xlsx"));

        assertNotEquals(revision, monitor.getRevision());
    }

    @Test
    void tellsAFileRenamed() throws IOException {
        write("project/rules/Bank Rating.xlsx", "workbook");
        var monitor = new FileChangesMonitor(root, ALL);
        var revision = monitor.getRevision();

        Files.move(root.resolve("project/rules/Bank Rating.xlsx"), root.resolve("project/rules/Rating.xlsx"));

        assertNotEquals(revision, monitor.getRevision());
    }

    @Test
    void tellsAFileWrittenAtTheSameTimeWithAnotherSize() throws IOException {
        write("project/rules.xml", "<project/>");
        var monitor = new FileChangesMonitor(root, ALL);
        var revision = monitor.getRevision();

        // Written again within the same tick of the file system clock: only the size tells.
        write("project/rules.xml", "<project name=\"Bank Rating\"/>");

        assertNotEquals(revision, monitor.getRevision());
    }

    @Test
    void tellsAFileWrittenWithTheSameSizeAtAnotherTime() throws IOException {
        write("project/rules.xml", "<project/>");
        var monitor = new FileChangesMonitor(root, ALL);
        var revision = monitor.getRevision();

        Files.setLastModifiedTime(root.resolve("project/rules.xml"), FileTime.fromMillis(1_800_000_000_000L));

        assertNotEquals(revision, monitor.getRevision());
    }

    @Test
    void countsEachChangeOnce() throws IOException {
        var monitor = new FileChangesMonitor(root, ALL);
        var revision = monitor.getRevision();

        write("project/rules.xml", "<project/>");
        var changed = monitor.getRevision();

        assertNotEquals(revision, changed);
        assertEquals(changed, monitor.getRevision());
    }

    @Test
    void readsEveryFolderOfAWideAndDeepTree() throws IOException {
        for (var project = 0; project < 12; project++) {
            for (var depth = 0; depth < 6; depth++) {
                write("p" + project + "/" + "level/".repeat(depth) + "Rules" + depth + ".xlsx", "workbook");
            }
        }
        var monitor = new FileChangesMonitor(root, ALL);
        var revision = monitor.getRevision();

        // The folders are read at once by several readers: none of the files may be lost, or a pass would differ.
        for (var pass = 0; pass < 20; pass++) {
            assertEquals(revision, monitor.getRevision());
        }

        write("p11/level/level/level/level/level/Rules5.xlsx", "workbook written");

        assertNotEquals(revision, monitor.getRevision());
    }

    @Test
    void leavesFoldersOut() throws IOException {
        write("project/rules.xml", "<project/>");
        var monitor = new FileChangesMonitor(root, ALL);
        var revision = monitor.getRevision();

        Files.createDirectories(root.resolve("project/rules/empty"));

        assertEquals(revision, monitor.getRevision());
    }

    @Test
    void leavesTheTemporaryFileOfAWriteOut() throws IOException {
        write("project/rules.xml", "<project/>");
        var monitor = new FileChangesMonitor(root, ALL);
        var revision = monitor.getRevision();

        write("project/.rules.xml" + FileReplacement.TEMPORARY_SUFFIX, "<project/> half");

        assertEquals(revision, monitor.getRevision());
    }

    @Test
    void answersAMissingFolderAsEmpty() throws IOException {
        var folder = root.resolve("design");
        var monitor = new FileChangesMonitor(folder, ALL);
        var revision = monitor.getRevision();

        assertEquals(revision, monitor.getRevision());

        write("design/project/rules.xml", "<project/>");

        assertNotEquals(revision, monitor.getRevision());
    }

    @Test
    @DisabledOnOs(value = OS.WINDOWS, disabledReason = "Creating a symbolic link needs a privilege on Windows")
    void followsALinkToAFile() throws IOException {
        var target = Files.createDirectories(root.resolve("outside")).resolve("Shared.xlsx");
        Files.writeString(target, "workbook");
        Files.setLastModifiedTime(target, WRITTEN);
        var folder = Files.createDirectories(root.resolve("design/project"));
        Files.createSymbolicLink(folder.resolve("Shared.xlsx"), target);
        var monitor = new FileChangesMonitor(root.resolve("design"), ALL);
        var revision = monitor.getRevision();

        Files.writeString(target, "workbook written");

        assertNotEquals(revision, monitor.getRevision());
    }

    @Test
    @DisabledOnOs(value = OS.WINDOWS, disabledReason = "Creating a symbolic link needs a privilege on Windows")
    void leavesABrokenLinkOut() throws IOException {
        var folder = Files.createDirectories(root.resolve("project"));
        write("project/rules.xml", "<project/>");
        var monitor = new FileChangesMonitor(root, ALL);
        var revision = monitor.getRevision();

        Files.createSymbolicLink(folder.resolve("Gone.xlsx"), root.resolve("nowhere/Gone.xlsx"));

        assertEquals(revision, monitor.getRevision());
    }

    @Test
    @DisabledOnOs(value = OS.WINDOWS, disabledReason = "POSIX permissions")
    void failsOnAFolderItCannotRead() throws IOException {
        assumeFalse("root".equals(System.getProperty("user.name")), "root reads any folder");
        write("project/rules.xml", "<project/>");
        var locked = root.resolve("project");
        Files.setPosixFilePermissions(locked, PosixFilePermissions.fromString("---------"));
        try {
            var monitor = new FileChangesMonitor(root, ALL);

            assertThrows(UncheckedIOException.class, monitor::getRevision);
        } finally {
            Files.setPosixFilePermissions(locked, PosixFilePermissions.fromString("rwx------"));
        }
    }

    /** Writes a file with the same modification time each time, as two writes within one clock tick have. */
    private void write(String path, String content) throws IOException {
        var file = root.resolve(path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
        Files.setLastModifiedTime(file, WRITTEN);
    }
}
