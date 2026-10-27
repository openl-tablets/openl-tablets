package org.openl.rules.repository.file;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.function.Predicate;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import org.openl.rules.repository.IgnoredPaths;
import org.openl.rules.repository.common.RevisionGetter;
import org.openl.util.FileReplacement;

/**
 * Tells whether the files under a folder changed since it was last asked.
 *
 * <p>A file added, removed, or written so that its size or its modification time differs is a change. A link to a
 * file counts as the file it points to; folders are not followed through links. The temporary file of a write in
 * progress is no change: the file it replaces is, once it is written.
 *
 * <p>Each question reads the whole folder, asking the file system about each file one time. Several folders are read
 * at once: a file system that answers from another machine, such as a network share or the folder of the host a
 * container is given, answers several questions at a time about as fast as one. A file removed while the folder is
 * read is left out, and the next question sees what is there.
 *
 * <p>A folder that is not watched, such as one the {@link IgnoredPaths ignored paths} name, is not read, so a change in
 * it, such as a fetch into the {@code .git} folder or a build into its output folder, is no change.
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class FileChangesMonitor implements RevisionGetter {
    /**
     * Reads the folders of every monitor, four at a time. More readers gain nothing more and take the processor from
     * the rest. Its threads end when no folder is read for a while.
     */
    private static final ForkJoinPool READERS = new ForkJoinPool(4);

    private final Path baseDir;
    /** Tells the folders inside the base folder that are read. */
    private final Predicate<Path> watched;
    private Map<Path, FileStamp> stamps = Map.of();
    private int revision;

    @Override
    public Object getRevision() {
        var current = scan();
        if (!current.equals(stamps)) {
            stamps = current;
            revision++;
        }
        return revision;
    }

    private Map<Path, FileStamp> scan() {
        var current = new ConcurrentHashMap<Path, FileStamp>(stamps.size() + 10);
        if (Files.isDirectory(baseDir)) {
            READERS.invoke(new FolderScan(watched, baseDir, current));
        }
        return current;
    }

    /** Reads the files of a folder and hands each watched folder inside it to a scan of its own. */
    @RequiredArgsConstructor
    private static final class FolderScan extends RecursiveAction {
        // A scan runs in the pool that started it, and is never serialized.
        private final transient Predicate<Path> watched;
        private final transient Path folder;
        private final transient Map<Path, FileStamp> stamps;

        @Override
        protected void compute() {
            var subfolders = new ArrayList<FolderScan>();
            try (var entries = Files.newDirectoryStream(folder)) {
                for (var entry : entries) {
                    read(entry, subfolders);
                }
            } catch (NoSuchFileException e) {
                // Removed while its parent was read: the next scan sees what is there.
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } catch (DirectoryIteratorException e) {
                throw new UncheckedIOException(e.getCause());
            }
            invokeAll(subfolders);
        }

        private void read(Path entry, List<FolderScan> subfolders) throws IOException {
            BasicFileAttributes attributes;
            try {
                attributes = Files.readAttributes(entry, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            } catch (NoSuchFileException e) {
                return;
            }
            if (attributes.isDirectory()) {
                if (watched.test(entry)) {
                    subfolders.add(new FolderScan(watched, entry, stamps));
                }
            } else if (!FileReplacement.isTemporary(entry)) {
                var stamp = stampOf(entry, attributes);
                if (stamp != null) {
                    stamps.put(entry, stamp);
                }
            }
        }

        /** The stamp of a file, of the file a link points to, or {@code null} for anything else. */
        private static @Nullable FileStamp stampOf(Path file, BasicFileAttributes attributes) throws IOException {
            var target = attributes;
            if (attributes.isSymbolicLink()) {
                try {
                    target = Files.readAttributes(file, BasicFileAttributes.class);
                } catch (NoSuchFileException e) {
                    return null;
                }
            }
            return target.isRegularFile() ? new FileStamp(target.size(), target.lastModifiedTime()) : null;
        }
    }

    private record FileStamp(long size, FileTime modified) {
    }
}
