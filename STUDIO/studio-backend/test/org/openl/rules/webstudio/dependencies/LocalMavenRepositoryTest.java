package org.openl.rules.webstudio.dependencies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalMavenRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-11-20T12:00:00Z");
    private static final Duration MONTH = Duration.ofDays(30);

    @TempDir
    private Path root;

    private LocalMavenRepository repository;

    @BeforeEach
    void setUp() {
        repository = new LocalMavenRepository(root, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void keepsWhatWasUsedWithinThePeriodAndRemovesTheRest() throws IOException {
        var fresh = version("com/example/fresh/1.0.0", 100, NOW.minus(Duration.ofDays(1)));
        var stale = version("com/example/stale/1.0.0", 200, NOW.minus(Duration.ofDays(31)));

        var removal = repository.clean(MONTH, 0, List.of());

        assertEquals(new LocalMavenRepository.Removal(1, 200), removal);
        assertTrue(Files.isDirectory(fresh));
        assertFalse(Files.exists(stale));
        assertFalse(Files.exists(root.resolve("com/example/stale")), "an emptied artifact folder goes too");
        assertTrue(Files.isDirectory(root.resolve("com/example")), "a folder still holding a version stays");
    }

    @Test
    void aUseRecordedLaterThanTheDownloadKeepsAVersion() throws IOException {
        var used = version("com/example/used/1.0.0", 100, NOW.minus(Duration.ofDays(90)));
        var withUse = new LocalMavenRepository(root, Clock.fixed(NOW.minus(Duration.ofDays(2)), ZoneOffset.UTC));
        withUse.markUsed(used.resolve("used-1.0.0.jar"));

        repository.clean(MONTH, 0, List.of());

        assertTrue(Files.isDirectory(used));
        assertEquals(NOW.minus(Duration.ofDays(2)),
                Files.getLastModifiedTime(used.resolve(LocalMavenRepository.LAST_USED)).toInstant());
        assertEquals(NOW.minus(Duration.ofDays(90)),
                Files.getLastModifiedTime(used.resolve("used-1.0.0.jar")).toInstant(),
                "the downloaded file itself is never changed");
    }

    @Test
    void recordsAUseAgainOnlyOnceTheLastOneIsAnHourOld() throws IOException {
        var recent = version("com/example/recent/1.0.0", 100, NOW.minus(Duration.ofMinutes(30)));
        var older = version("com/example/older/1.0.0", 100, NOW.minus(Duration.ofMinutes(90)));

        repository.markUsed(recent.resolve("recent-1.0.0.jar"));
        repository.markUsed(older.resolve("older-1.0.0.jar"));

        assertEquals(NOW.minus(Duration.ofMinutes(30)),
                Files.getLastModifiedTime(recent.resolve(LocalMavenRepository.LAST_USED)).toInstant());
        assertEquals(NOW, Files.getLastModifiedTime(older.resolve(LocalMavenRepository.LAST_USED)).toInstant());
    }

    @Test
    void aUnpackedProjectCountsForItsVersion() throws IOException {
        var version = version("com/example/domain/1.0.0", 100, NOW.minus(Duration.ofDays(90)));
        var unpacked = Files.createDirectories(version.resolve("domain-1.0.0-123"));
        Files.writeString(unpacked.resolve("rules.xml"), "<project/>");

        repository.markUsed(unpacked);

        assertEquals(0, repository.clean(MONTH, 0, List.of()).versions());
        assertTrue(Files.isRegularFile(unpacked.resolve("rules.xml")));
    }

    @Test
    void removesWhatWasUsedLongestAgoWhileTheRepositoryIsTooLarge() throws IOException {
        var oldest = version("com/example/a/1.0.0", 400, NOW.minus(Duration.ofDays(3)));
        var older = version("com/example/b/1.0.0", 400, NOW.minus(Duration.ofDays(2)));
        var newest = version("com/example/c/1.0.0", 400, NOW.minus(Duration.ofDays(1)));

        var removal = repository.clean(null, 900, List.of());

        assertEquals(new LocalMavenRepository.Removal(1, 400), removal);
        assertFalse(Files.exists(oldest));
        assertTrue(Files.isDirectory(older));
        assertTrue(Files.isDirectory(newest));
        assertEquals(800, repository.size());
    }

    @Test
    void neverRemovesWhatTheProjectsReadNow() throws IOException {
        var inUse = version("com/example/lib/1.0.0", 400, NOW.minus(Duration.ofDays(90)));
        var unpacked = Files.createDirectories(inUse.resolve("lib-1.0.0-1"));

        var removal = repository.clean(MONTH, 1, List.of(unpacked));

        assertEquals(LocalMavenRepository.Removal.NONE, removal);
        assertTrue(Files.isDirectory(inUse));
    }

    @Test
    void keepsEverythingWithoutAPeriodAndALimit() throws IOException {
        var version = version("com/example/lib/1.0.0", 400, Instant.EPOCH);

        assertEquals(LocalMavenRepository.Removal.NONE, repository.clean(null, 0, List.of()));
        assertTrue(Files.isDirectory(version));
    }

    @Test
    void emptiesWhatOpenLStudioDownloadedAndKeepsTheRest() throws IOException {
        version("com/example/a/1.0.0", 100, NOW);
        version("org/example/b/2.0", 50, NOW);
        var metadata = root.resolve("com/example/a/maven-metadata-central.xml");
        Files.writeString(metadata, "<metadata/>");
        var foreign = foreignVersion("org/apache/maven/3.9.0");

        var removal = repository.clear();

        assertEquals(new LocalMavenRepository.Removal(2, 150), removal);
        assertFalse(Files.exists(root.resolve("com/example/a/1.0.0")));
        assertFalse(Files.exists(root.resolve("org/example")));
        assertTrue(Files.isRegularFile(metadata), "a file outside a version stays");
        assertTrue(Files.isDirectory(foreign), "a version Maven downloaded stays");
    }

    @Test
    void neverCleansAVersionOpenLStudioHasNotUsed() throws IOException {
        var foreign = foreignVersion("org/apache/maven/3.9.0");

        assertEquals(LocalMavenRepository.Removal.NONE, repository.clean(MONTH, 1, List.of()));
        assertTrue(Files.isDirectory(foreign));
    }

    @Test
    void aMissingRepositoryIsEmpty() {
        var missing = new LocalMavenRepository(root.resolve("missing"), Clock.systemUTC());

        assertEquals(0, missing.size());
        assertEquals(LocalMavenRepository.Removal.NONE, missing.clean(MONTH, 1, List.of()));
        assertEquals(LocalMavenRepository.Removal.NONE, missing.clear());
    }

    @Test
    void recordsNoUseOutsideTheRepository() throws IOException {
        var outside = Files.createTempFile(root.getParent(), "outside", ".jar");
        try {
            repository.markUsed(outside);

            assertFalse(Files.exists(outside.resolveSibling(LocalMavenRepository.LAST_USED)));
        } finally {
            Files.delete(outside);
        }
    }

    /** A version OpenL Studio downloaded at the given time: a jar of the given size, and the mark of its use. */
    private Path version(String path, int bytes, Instant downloaded) throws IOException {
        var folder = foreignVersion(path);
        var jar = folder.resolve(jarName(path));
        Files.write(jar, new byte[bytes]);
        Files.setLastModifiedTime(jar, FileTime.from(downloaded));
        new LocalMavenRepository(root, Clock.fixed(downloaded, ZoneOffset.UTC)).markUsed(jar);
        return folder;
    }

    /** A version some other tool, as Maven, downloaded to the repository: a jar long unused, and no mark. */
    private Path foreignVersion(String path) throws IOException {
        var folder = Files.createDirectories(root.resolve(path));
        var jar = folder.resolve(jarName(path));
        Files.write(jar, new byte[10]);
        Files.setLastModifiedTime(jar, FileTime.from(Instant.EPOCH));
        return folder;
    }

    private static String jarName(String path) {
        var parts = path.split("/");
        return parts[parts.length - 2] + "-" + parts[parts.length - 1] + ".jar";
    }
}
