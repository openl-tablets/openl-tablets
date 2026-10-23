package org.openl.util;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

/**
 * Replaces the content of a file as a whole.
 *
 * <p>The new content is written into a temporary file in the same folder, which then takes the place of the file in
 * one step. A reader meets the file as it was before or as it is after, never half written. A write that fails, and
 * a write the check before the replacement refuses, leave the file as it was.
 *
 * <p>A read-only file is not replaced. Neither is a file another program holds open, as Excel does on Windows; a
 * program that opens the file for a moment, such as an indexer or an antivirus, is waited out. Both failures are a
 * {@link FileSystemException}. A file reached through a symbolic link is replaced where the link points. The file
 * keeps its permissions where the file system has POSIX permissions, but it belongs to the user that replaced it.
 *
 * <p>A file that cannot be replaced by another file is written over in place instead, as it was before: a file in a
 * folder that takes no new file, and a file mounted into a container on its own. A reader may meet such a file half
 * written. It keeps its owner, its links and its permissions.
 *
 * <p>The temporary file is hidden: its name starts with a dot and ends with {@value #TEMPORARY_SUFFIX}
 * ({@link #isTemporary(Path)}). The ones left behind by a process that stopped while writing are removed by a later
 * replacement of the same file that finds one in its way.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FileReplacement {

    /** The end of the name of a temporary file. */
    public static final String TEMPORARY_SUFFIX = ".openl-write";

    /** How old a temporary file is before it is taken for one left behind rather than for a write under way. */
    private static final Duration LEFTOVER_AGE = Duration.ofHours(1);

    private static final int ATTEMPTS = 5;

    private static final Duration PAUSE = Duration.ofMillis(100);

    /** The content of the file, written whole. */
    @FunctionalInterface
    public interface Content {
        void writeTo(OutputStream out) throws IOException;
    }

    /** What is checked once the content is written and before it replaces the file. */
    @FunctionalInterface
    public interface Check {

        /**
         * @param written the temporary file that holds the new content
         * @throws IOException to leave the file as it is
         */
        void verify(Path written) throws IOException;
    }

    /**
     * Replaces the content of the file, or creates the file when there is none, with nothing to check before.
     *
     * @see #replace(Path, Content, Check)
     */
    public static void replace(Path file, Content content) throws IOException {
        replace(file, content, written -> {
            // Nothing to check.
        });
    }

    /**
     * Replaces the content of the file, or creates the file when there is none.
     *
     * @param file            the file to replace; its folder must exist
     * @param content         the new content
     * @param beforeReplacing checked right before the replacement, so that it sees the file as it is replaced
     * @throws AccessDeniedException when the file is read-only
     * @throws FileSystemException   when another program holds the file open, or the folder cannot be written
     */
    public static void replace(Path file, Content content, Check beforeReplacing) throws IOException {
        var target = Files.exists(file) ? file.toRealPath() : file.toAbsolutePath();
        if (Files.exists(target) && !Files.isWritable(target)) {
            throw new AccessDeniedException(target.toString(), null, "The file is read-only.");
        }
        var beside = createTemporaryBeside(target);
        var temporary = beside == null ? FileUtils.createPrivateTempFile("openl-write", TEMPORARY_SUFFIX) : beside;
        try {
            try (var out = Files.newOutputStream(temporary)) {
                content.writeTo(out);
            }
            if (beside != null) {
                keepPermissions(target, temporary);
            }
            beforeReplacing.verify(temporary);
            if (beside == null) {
                overwrite(temporary, target);
            } else {
                move(temporary, target);
            }
        } finally {
            removeTemporary(temporary);
        }
    }

    /**
     * Removes the temporary file of a replacement that is over.
     *
     * <p>A file that cannot be removed is only logged: the replacement has succeeded or failed already, and its
     * outcome stands. A later replacement of the same file removes it once it is old enough.
     */
    private static void removeTemporary(Path temporary) {
        try {
            Files.deleteIfExists(temporary);
        } catch (IOException e) {
            log.warn("Cannot remove the temporary file '{}'.", temporary, e);
        }
    }

    /**
     * Whether the file is the temporary file of a replacement: no file of its own, it is not listed or read.
     */
    public static boolean isTemporary(Path file) {
        var name = String.valueOf(file.getFileName());
        return name.startsWith(".") && name.endsWith(TEMPORARY_SUFFIX);
    }

    /**
     * Creates the temporary file the content is written into, beside the file.
     *
     * <p>It is named after the file. A temporary file of that name in the way is either left behind long ago, and
     * then removed with every other temporary file of the file left behind, or one of a write of the same file under
     * way, and then left alone: this write takes a name of its own.
     *
     * @return the temporary file, or null when the folder takes no new file
     */
    private static @Nullable Path createTemporaryBeside(Path target) throws IOException {
        var hidden = "." + target.getFileName();
        var temporary = target.resolveSibling(hidden + TEMPORARY_SUFFIX);
        try {
            if (Files.exists(temporary)) {
                removeLeftovers(target, hidden + ".");
            }
            return createUnlessTaken(temporary, target.resolveSibling(hidden + "." + UUID.randomUUID()
                    + TEMPORARY_SUFFIX));
        } catch (AccessDeniedException e) {
            return null;
        }
    }

    /** Creates the temporary file, or another one when a write of the same file under way holds its name. */
    private static Path createUnlessTaken(Path temporary, Path another) throws IOException {
        try {
            return Files.createFile(temporary);
        } catch (FileAlreadyExistsException e) {
            return Files.createFile(another);
        }
    }

    private static void removeLeftovers(Path target, String prefix) throws IOException {
        try (var temporaries = Files.newDirectoryStream(target.getParent(),
                file -> isTemporary(file) && file.getFileName().toString().startsWith(prefix))) {
            for (var temporary : temporaries) {
                if (isLeftover(temporary)) {
                    Files.deleteIfExists(temporary);
                }
            }
        }
    }

    private static boolean isLeftover(Path temporary) throws IOException {
        try {
            var modified = Files.getLastModifiedTime(temporary).toInstant();
            return modified.isBefore(Instant.now().minus(LEFTOVER_AGE));
        } catch (NoSuchFileException e) {
            // Its write has just ended.
            return false;
        }
    }

    private static void keepPermissions(Path target, Path temporary) throws IOException {
        if (Files.exists(target) && Files.getFileStore(target).supportsFileAttributeView(PosixFileAttributeView.class)) {
            Files.setPosixFilePermissions(temporary, Files.getPosixFilePermissions(target));
        }
    }

    /**
     * Moves the temporary file over the file.
     *
     * <p>Windows refuses to replace a file that another program has open. A program that opens it for a moment is
     * waited out. A file that still is not replaced is written over in place, as a file mounted into a container on
     * its own must be; a program that holds the file open makes that fail as well.
     */
    private static void move(Path temporary, Path target) throws IOException {
        for (var attempt = 1; ; attempt++) {
            try {
                moveOnce(temporary, target);
                return;
            } catch (FileSystemException e) {
                if (e instanceof NoSuchFileException) {
                    throw e;
                }
                if (attempt == ATTEMPTS) {
                    overwriteOr(e, temporary, target);
                    return;
                }
                pause();
            }
        }
    }

    private static void overwriteOr(FileSystemException refused, Path temporary, Path target) throws IOException {
        try {
            overwrite(temporary, target);
        } catch (IOException e) {
            refused.addSuppressed(e);
            throw refused;
        }
    }

    /** Writes the content over the file in place, so the file stays the same file. */
    private static void overwrite(Path temporary, Path target) throws IOException {
        try (var out = Files.newOutputStream(target)) {
            Files.copy(temporary, out);
        }
    }

    private static void moveOnce(Path temporary, Path target) throws IOException {
        try {
            Files.move(temporary, target, ATOMIC_MOVE, REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temporary, target, REPLACE_EXISTING);
        }
    }

    private static void pause() throws InterruptedIOException {
        try {
            Thread.sleep(PAUSE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterruptedIOException("Interrupted while waiting to replace a file.");
        }
    }
}
