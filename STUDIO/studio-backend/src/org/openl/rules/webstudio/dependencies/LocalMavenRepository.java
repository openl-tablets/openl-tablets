package org.openl.rules.webstudio.dependencies;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.function.ToLongFunction;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import org.openl.util.FileUtils;

/**
 * The local Maven repository the downloaded dependencies are kept in, and what is removed from it.
 *
 * <p>A version of an artifact is the unit: the folder {@code groupId/artifactId/version} with every file of it, the
 * folders its archives are unpacked into included. A version is used when a project asks for it; the last use is
 * kept in a marker file of the folder, so the downloaded files themselves are never changed.
 *
 * <p>Only the versions holding the marker are removed: those OpenL Studio has downloaded or used. A local repository
 * shared with Maven keeps every other file. Nothing in use is removed: the caller names the files the projects read
 * at the moment.
 *
 * @author Yury Molchan
 */
@Slf4j
final class LocalMavenRepository {

    /** The marker file whose modification time is the last use of a version. */
    static final String LAST_USED = ".openl-last-used";
    /** How long a recorded use stands for the later ones: a resolution reads many files of each version. */
    static final Duration USE_RECORDING_PERIOD = Duration.ofHours(1);

    private final Path root;
    private final Clock clock;

    LocalMavenRepository(Path root, Clock clock) {
        this.root = root;
        this.clock = clock;
    }

    /**
     * Records that a project uses the version the file belongs to: a downloaded file, or an unpacked folder.
     *
     * <p>A use within an hour of the recorded one is not recorded again, so the last use is known to the hour.
     */
    void markUsed(Path file) {
        var version = file.getParent();
        if (version == null || !version.startsWith(root)) {
            return;
        }
        var marker = version.resolve(LAST_USED);
        var now = clock.instant();
        try {
            if (Files.notExists(marker)) {
                Files.createFile(marker);
            } else if (modified(marker).isAfter(now.minus(USE_RECORDING_PERIOD))) {
                return;
            }
            Files.setLastModifiedTime(marker, FileTime.from(now));
        } catch (IOException e) {
            log.debug("Cannot record the use of '{}'.", version, e);
        }
    }

    /** The size of every file of the local repository, in bytes. */
    long size() {
        if (!Files.isDirectory(root)) {
            return 0;
        }
        try {
            return sizeOfTree(root);
        } catch (IOException | UncheckedIOException e) {
            log.warn("Cannot measure the local Maven repository '{}'.", root, e);
            return 0;
        }
    }

    /**
     * Removes the versions no project needs.
     *
     * <p>First the versions unused for longer than the period, then, while the versions OpenL Studio keeps are still
     * larger than the limit, the versions used longest ago. A version holding a file in use stays.
     *
     * @param unusedFor how long a version may stay unused, or {@code null} to keep versions however long unused
     * @param maxBytes  how large the versions OpenL Studio keeps may grow, or {@code 0} for no limit
     * @param inUse     the files the projects compiled at the moment read
     * @return what was removed
     */
    Removal clean(@Nullable Duration unusedFor, long maxBytes, Collection<Path> inUse) {
        var all = versions();
        var versions = all.stream()
                .filter(version -> inUse.stream().noneMatch(file -> file.startsWith(version.folder())))
                .sorted(Comparator.comparing(Version::lastUsed))
                .toList();
        // Each version is measured once, as the removals and the limit need it
        var sizes = new HashMap<Version, Long>();
        ToLongFunction<Version> sizeOf = version -> sizes.computeIfAbsent(version, LocalMavenRepository::bytesOf);
        var removal = Removal.NONE;
        var kept = new ArrayList<Version>();
        var unusedSince = unusedFor == null ? null : clock.instant().minus(unusedFor);
        for (Version version : versions) {
            if (unusedSince != null && version.lastUsed().isBefore(unusedSince)) {
                removal = removal.plus(remove(version, sizeOf.applyAsLong(version)));
            } else {
                kept.add(version);
            }
        }
        if (maxBytes > 0) {
            // What OpenL Studio keeps counts alone: the files of a repository shared with Maven are never removed
            var size = all.stream().mapToLong(sizeOf).sum() - removal.bytes();
            for (int i = 0; i < kept.size() && size > maxBytes; i++) {
                var removed = remove(kept.get(i), sizeOf.applyAsLong(kept.get(i)));
                size -= removed.bytes();
                removal = removal.plus(removed);
            }
        }
        return removal;
    }

    /** Removes every version OpenL Studio has downloaded or used that can be removed. */
    Removal clear() {
        var removal = Removal.NONE;
        for (Version version : versions()) {
            removal = removal.plus(remove(version, bytesOf(version)));
        }
        return removal;
    }

    /** Removes the version, whose files take the given bytes. */
    private Removal remove(Version version, long bytes) {
        FileUtils.deleteQuietly(version.folder());
        if (Files.exists(version.folder())) {
            log.info("Cannot remove '{}' from the local Maven repository; it is kept.", version.folder());
            return Removal.NONE;
        }
        removeEmptyParents(version.folder().getParent());
        log.info("Removed '{}' from the local Maven repository.", root.relativize(version.folder()));
        return new Removal(1, bytes);
    }

    private void removeEmptyParents(@Nullable Path folder) {
        var current = folder;
        while (current != null && !current.equals(root) && current.startsWith(root) && isEmpty(current)) {
            try {
                Files.delete(current);
            } catch (IOException e) {
                return;
            }
            current = current.getParent();
        }
    }

    /** Every version OpenL Studio has downloaded or used: a folder holding the marker. */
    private List<Version> versions() {
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (var files = Files.walk(root)) {
            return files.filter(file -> LAST_USED.equals(String.valueOf(file.getFileName())))
                    .map(marker -> new Version(marker.getParent(), modified(marker)))
                    .toList();
        } catch (IOException | UncheckedIOException e) {
            log.warn("Cannot read the local Maven repository '{}'.", root, e);
            return List.of();
        }
    }

    private static Instant modified(Path file) {
        try {
            return Files.getLastModifiedTime(file).toInstant();
        } catch (IOException e) {
            return Instant.EPOCH;
        }
    }

    private static boolean isEmpty(Path folder) {
        try (var files = Files.list(folder)) {
            return files.findAny().isEmpty();
        } catch (IOException e) {
            return false;
        }
    }

    /** The size of the files of the version, in bytes; nothing when they cannot be read. */
    private static long bytesOf(Version version) {
        try {
            return sizeOfTree(version.folder());
        } catch (IOException | UncheckedIOException e) {
            return 0;
        }
    }

    /** The size of every file under the path, in bytes; a file removed meanwhile counts as empty. */
    private static long sizeOfTree(Path path) throws IOException {
        try (var files = Files.walk(path)) {
            return files.filter(Files::isRegularFile).mapToLong(LocalMavenRepository::sizeOf).sum();
        }
    }

    private static long sizeOf(Path file) {
        try {
            return Files.size(file);
        } catch (NoSuchFileException e) {
            return 0;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private record Version(Path folder, Instant lastUsed) {
    }

    /**
     * What was removed from the local repository.
     *
     * @param versions how many versions of artifacts
     * @param bytes    how many bytes the removed files held
     */
    record Removal(int versions, long bytes) {
        static final Removal NONE = new Removal(0, 0);

        Removal plus(Removal other) {
            return new Removal(versions + other.versions, bytes + other.bytes);
        }
    }
}
