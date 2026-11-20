package org.openl.rules.webstudio.dependencies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.message.OpenLMessage;
import org.openl.message.Severity;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;

class MavenDependencyResolverTest {

    @TempDir
    private Path temp;

    private Path remote;
    private Path local;
    private final MutableClock clock = new MutableClock();
    private MavenDependencyResolver resolver;

    @BeforeEach
    void setUp() throws IOException {
        remote = Files.createDirectories(temp.resolve("remote"));
        local = temp.resolve("local");
        resolver = new MavenDependencyResolver(remote.toUri().toString(), null, null, local, clock);
    }

    @AfterEach
    void tearDown() {
        resolver.destroy();
    }

    @Test
    void downloadsNothingWhileNoRepositoryIsSet() {
        var disabled = new MavenDependencyResolver(" ", null, null, local, clock);

        assertFalse(disabled.isEnabled());
        assertSame(MavenDependencyResolver.Libraries.NONE,
                disabled.librariesOf(project("Rules", library("com.example:lib:jar:1.0.0"))));
        assertSame(MavenDependencyResolver.DownloadedProject.NONE,
                disabled.projectOf(projectDependency("Domain", "com.example:domain:1.0.0")));
        disabled.destroy();
    }

    @Test
    void downloadsNoLibraryForAProjectWithoutOne() {
        assertTrue(resolver.isEnabled());
        assertSame(MavenDependencyResolver.Libraries.NONE, resolver.librariesOf(project("Rules")));
        assertSame(MavenDependencyResolver.Libraries.NONE,
                resolver.librariesOf(project("Rules", projectDependency("Domain", "com.example:domain:1.0.0"))));
    }

    @Test
    void downloadsALibraryWithTheLibrariesItNeedsAtRuntime() throws Exception {
        publish("com.example", "transitive", "1.0.0", "jar", null);
        publish("com.example", "testing", "1.0.0", "jar", null);
        publish("com.example", "lib", "1.0.0", "jar", """
                <dependencies>
                    <dependency>
                        <groupId>com.example</groupId>
                        <artifactId>transitive</artifactId>
                        <version>1.0.0</version>
                        <scope>runtime</scope>
                    </dependency>
                    <dependency>
                        <groupId>com.example</groupId>
                        <artifactId>testing</artifactId>
                        <version>1.0.0</version>
                        <scope>test</scope>
                    </dependency>
                </dependencies>
                """);

        var libraries = resolver.librariesOf(project("Rules",
                library("com.example:lib:jar:1.0.0"),
                projectDependency("Domain", null)));

        assertEquals(List.of(), libraries.messages());
        assertEquals(List.of("lib-1.0.0.jar", "transitive-1.0.0.jar"), fileNames(libraries.files()));
        for (Path file : libraries.files()) {
            assertTrue(file.startsWith(local), "kept in the local repository: " + file);
        }
    }

    @Test
    void reportsALibraryItCannotDownloadAndKeepsTheOthers() throws Exception {
        publish("com.example", "lib", "1.0.0", "jar", null);

        var libraries = resolver.librariesOf(project("Rules",
                library("com.example:lib:jar:1.0.0"),
                library("com.example:missing:jar:2.0.0")));

        assertEquals(List.of("lib-1.0.0.jar"), fileNames(libraries.files()));
        var message = single(libraries.messages());
        assertEquals(Severity.ERROR, message.getSeverity());
        assertTrue(message.getSummary().startsWith("Cannot download a Maven dependency: "), message.getSummary());
        assertTrue(message.getSummary().contains("com.example:missing:jar:2.0.0"), message.getSummary());
    }

    @Test
    void reportsCoordinatesThatAreNotValid() {
        var libraries = resolver.librariesOf(project("Rules", library("not-coordinates")));

        assertEquals(List.of(), libraries.files());
        assertEquals("The Maven coordinates 'not-coordinates' are not valid.",
                single(libraries.messages()).getSummary());
    }

    @Test
    void asksTheRepositoryAgainOnlyAfterAWhile() throws Exception {
        var project = project("Rules", library("com.example:lib:jar:1.0.0"));
        assertEquals(1, resolver.librariesOf(project).messages().size());

        publish("com.example", "lib", "1.0.0", "jar", null);
        clock.advance(MavenDependencyResolver.CACHE_PERIOD.minusSeconds(1));
        assertEquals(1, resolver.librariesOf(project).messages().size(), "the failure is kept for a while");

        clock.advance(MavenDependencyResolver.CACHE_PERIOD);
        var libraries = resolver.librariesOf(project);
        assertEquals(List.of(), libraries.messages());
        assertEquals(List.of("lib-1.0.0.jar"), fileNames(libraries.files()));
    }

    @Test
    void forgetsWhatNoProjectAsksForAnyMore() throws Exception {
        publish("com.example", "old", "1.0.0", "jar", null);
        publish("com.example", "new", "1.0.0", "jar", null);
        resolver.librariesOf(project("Rules", library("com.example:old:jar:1.0.0")));
        assertEquals(1, resolver.cachedAnswers());

        // rules.xml names another library now, and the old answer outlives the period unasked
        clock.advance(MavenDependencyResolver.CACHE_PERIOD.plusSeconds(1));
        resolver.librariesOf(project("Rules", library("com.example:new:jar:1.0.0")));

        assertEquals(1, resolver.cachedAnswers());
    }

    @Test
    void asksForAGroupThePrefixesOfTheRepositoryLeaveOut() throws Exception {
        publish("com.example", "lib", "1.0.0", "jar", null);
        Files.createDirectories(remote.resolve(".meta"));
        Files.writeString(remote.resolve(".meta/prefixes.txt"), "## repository-prefixes/2.0\n/org\n");

        var libraries = resolver.librariesOf(project("Rules", library("com.example:lib:jar:1.0.0")));

        assertEquals(List.of(), libraries.messages(), "a manager may list the prefixes of a proxy alone");
        assertEquals(List.of("lib-1.0.0.jar"), fileNames(libraries.files()));
    }

    @Test
    void recordsTheUseOfWhatItResolves() throws Exception {
        publishParent("com.example", "parent", "3");
        publishWithParent("com.example", "lib", "1.0.0", "parent", "3");
        publishProject("com.example", "domain", "1.0.0", "Domain");

        resolver.librariesOf(project("Rules", library("com.example:lib:jar:1.0.0")));
        resolver.projectOf(projectDependency("Domain", "com.example:domain:1.0.0"));

        // The parent POM is read to work the dependencies out, so it is used as much as the jar
        for (var version : List.of("com/example/lib/1.0.0", "com/example/parent/3", "com/example/domain/1.0.0")) {
            var marker = local.resolve(version).resolve(LocalMavenRepository.LAST_USED);
            assertEquals(clock.instant(), Files.getLastModifiedTime(marker).toInstant(), version);
        }
    }

    @Test
    void emptiesALocalRepositoryGivenByARelativePath() throws Exception {
        publish("com.example", "lib", "1.0.0", "jar", null);
        var relative = Path.of("").toAbsolutePath().relativize(local);
        var relativeResolver = new MavenDependencyResolver(remote.toUri().toString(), null, null, relative, clock);
        try {
            relativeResolver.librariesOf(project("Rules", library("com.example:lib:jar:1.0.0")));

            assertTrue(Files.exists(local.resolve("com/example/lib/1.0.0").resolve(LocalMavenRepository.LAST_USED)));
            assertEquals(1, relativeResolver.clearLocalRepository().versions());
            assertFalse(Files.exists(local.resolve("com/example/lib/1.0.0")));
        } finally {
            relativeResolver.destroy();
        }
    }

    @Test
    void cleansWhatNoAnswerInMemoryNames() throws Exception {
        publish("com.example", "kept", "1.0.0", "jar", null);
        publish("com.example", "dropped", "1.0.0", "jar", null);
        resolver.librariesOf(project("Old", library("com.example:dropped:jar:1.0.0")));
        clock.advance(MavenDependencyResolver.CACHE_PERIOD.plusSeconds(1));
        resolver.librariesOf(project("New", library("com.example:kept:jar:1.0.0")));

        // Any size is too large: only what the projects read now stays
        var removal = resolver.cleanLocalRepository(null, 1);

        assertEquals(1, removal.versions());
        assertFalse(Files.exists(local.resolve("com/example/dropped")));
        assertTrue(Files.isDirectory(local.resolve("com/example/kept/1.0.0")));
        assertTrue(resolver.localRepositorySize() > 0);
    }

    @Test
    void emptiesTheLocalRepositoryAndForgetsEveryAnswer() throws Exception {
        publish("com.example", "lib", "1.0.0", "jar", null);
        var project = project("Rules", library("com.example:lib:jar:1.0.0"));
        var first = resolver.librariesOf(project);

        var removal = resolver.clearLocalRepository();

        assertEquals(1, removal.versions());
        assertEquals(0, resolver.cachedAnswers());
        assertFalse(Files.exists(local.resolve("com/example/lib/1.0.0")));
        var again = resolver.librariesOf(project);
        assertEquals(fileNames(first.files()), fileNames(again.files()), "downloaded again when asked");
    }

    @Test
    void downloadsAndUnpacksAnOpenLProject() throws Exception {
        publishProject("com.example", "domain", "1.0.0", "Domain");

        var downloaded = resolver.projectOf(projectDependency("Domain", "com.example:domain:1.0.0"));

        assertNull(downloaded.problem());
        var project = downloaded.project();
        assertNotNull(project);
        assertEquals("Domain", project.getName());
        assertTrue(project.getProjectFolder().startsWith(local));
        assertTrue(Files.isRegularFile(project.getProjectFolder().resolve("rules.xml")));
        assertSame(downloaded, resolver.projectOf(projectDependency("Domain", "com.example:domain:1.0.0")),
                "the same download is answered while it is kept");
    }

    @Test
    void unpacksADownloadedProjectOnce() throws Exception {
        publishProject("com.example", "domain", "1.0.0", "Domain");
        var first = resolver.projectOf(projectDependency("Domain", "com.example:domain:1.0.0")).project();
        clock.advance(MavenDependencyResolver.CACHE_PERIOD.plusSeconds(1));

        var second = resolver.projectOf(projectDependency("Domain", "com.example:domain:1.0.0")).project();

        assertNotNull(first);
        assertNotNull(second);
        assertEquals(first.getProjectFolder(), second.getProjectFolder());
    }

    @Test
    void refusesAProjectOfAnotherName() throws Exception {
        publishProject("com.example", "domain", "1.0.0", "Other");

        var downloaded = resolver.projectOf(projectDependency("Domain", "com.example:domain:1.0.0"));

        assertNull(downloaded.project());
        assertEquals("'com.example:domain:1.0.0' is the project 'Other', not the dependency 'Domain'.",
                downloaded.problem().getSummary());
    }

    @Test
    void refusesAnArchiveThatIsNoOpenLProject() throws Exception {
        writeZip(artifactPath("com.example", "domain", "1.0.0", null, "zip"), Map.of("readme.txt", "Nothing"));

        var downloaded = resolver.projectOf(projectDependency("Domain", "com.example:domain:1.0.0"));

        assertNull(downloaded.project());
        assertEquals("'com.example:domain:1.0.0' of the dependency 'Domain' is not an OpenL project.",
                downloaded.problem().getSummary());
    }

    @Test
    void reportsAProjectItCannotDownload() {
        var downloaded = resolver.projectOf(projectDependency("Domain", "com.example:domain:1.0.0"));

        assertNull(downloaded.project());
        assertEquals(Severity.ERROR, downloaded.problem().getSeverity());
        assertTrue(downloaded.problem().getSummary().contains("com.example:domain:zip:1.0.0"),
                downloaded.problem().getSummary());
    }

    @Test
    void reportsProjectCoordinatesThatAreNotValid() {
        var downloaded = resolver.projectOf(projectDependency("Domain", "com.example:domain"));

        assertEquals("The Maven coordinates 'com.example:domain' of the dependency 'Domain' are not valid.",
                downloaded.problem().getSummary());
    }

    @Test
    void readsCoordinatesAsTheOpenLMavenPluginDoes() {
        var archive = MavenDependencyResolver.artifactOf("com.example:domain:1.0.0");
        assertEquals("com.example:domain:zip:1.0.0", archive.toString());

        var jar = MavenDependencyResolver.artifactOf(" com.example:lib:jar:2.0.0 ");
        assertEquals("com.example:lib:jar:2.0.0", jar.toString());

        var classified = MavenDependencyResolver.artifactOf("com.example:lib:jar:tests:2.0.0");
        assertEquals("tests", classified.getClassifier());
        assertEquals("jar", classified.getExtension());

        assertNull(MavenDependencyResolver.artifactOf("com.example:lib"));
        assertNull(MavenDependencyResolver.artifactOf("a:b:c:d:e:f"));
        assertNull(MavenDependencyResolver.artifactOf("com.example::1.0.0"));
    }

    @Test
    void refusesCoordinatesThatNameAFileOutsideTheRepository() {
        for (var coordinates : List.of("com.example:..:1.0.0", "com.example:lib:..", "com.example:lib:jar:.:1.0",
                "com/example:lib:1.0.0", "com.example:lib:1.0\\..\\..", ".opt.openl:lib:1.0.0", "..com:lib:1.0.0")) {
            assertNull(MavenDependencyResolver.artifactOf(coordinates), coordinates);
        }
        assertNotNull(MavenDependencyResolver.artifactOf("com.example:lib_x:jar:1.0.0-beta+2"));
    }

    @Test
    void leavesNothingOfAnArchiveItCannotUnpack() throws Exception {
        var archive = artifactPath("com.example", "domain", "1.0.0", null, "zip");
        var content = new byte[64 * 1024];
        new Random(1).nextBytes(content);
        writeZip(archive, Map.of("rules.xml", new String(content, StandardCharsets.ISO_8859_1)));
        // Cut in the middle of the entry
        Files.write(archive, Arrays.copyOf(Files.readAllBytes(archive), (int) Files.size(archive) / 2));

        var downloaded = resolver.projectOf(projectDependency("Domain", "com.example:domain:1.0.0"));

        assertNull(downloaded.project());
        assertTrue(downloaded.problem().getSummary().startsWith("Cannot read the project 'Domain'"),
                downloaded.problem().getSummary());
        try (var files = Files.list(local.resolve("com/example/domain/1.0.0"))) {
            assertEquals(List.of(), files.filter(Files::isDirectory).toList(), "no unpacked or temporary folder");
        }
    }

    @Test
    void resolvesOnceForTheCallersAskingMeanwhileWithoutHoldingUpOthers() throws Exception {
        var started = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var resolutions = new AtomicInteger();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var first = executor.submit(() -> resolver.cached("slow", () -> {
                started.countDown();
                await(release);
                return "answer " + resolutions.incrementAndGet();
            }));
            started.await();
            var second = executor.submit(() -> resolver.cached("slow",
                    () -> "answer " + resolutions.incrementAndGet()));

            assertEquals("other", resolver.cached("other", () -> "other"), "another key is not held up");
            release.countDown();

            assertEquals("answer 1", first.get());
            assertEquals("answer 1", second.get());
            assertEquals(1, resolutions.get());
        }
    }

    @Test
    void resolvesAgainAfterAResolutionFails() {
        assertThrows(IllegalStateException.class, () -> resolver.cached("key", () -> {
            throw new IllegalStateException("failed");
        }));

        assertEquals("found", resolver.cached("key", () -> "found"));
    }

    @Test
    void resolvesAgainAfterAResolutionBreaksWithAnError() {
        assertThrows(StackOverflowError.class, () -> resolver.cached("key", () -> {
            throw new StackOverflowError("deep");
        }));

        assertEquals("found", resolver.cached("key", () -> "found"), "no caller waits for the failed answer");
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private static ProjectDescriptor project(String name, ProjectDependencyDescriptor... dependencies) {
        var project = new ProjectDescriptor();
        project.setName(name);
        project.setDependencies(dependencies.length == 0 ? null : List.of(dependencies));
        return project;
    }

    private static ProjectDependencyDescriptor library(String coordinates) {
        return projectDependency(null, coordinates);
    }

    private static ProjectDependencyDescriptor projectDependency(String name, String coordinates) {
        var dependency = new ProjectDependencyDescriptor();
        dependency.setName(name);
        dependency.setMavenArtifact(coordinates);
        return dependency;
    }

    private static List<String> fileNames(List<Path> files) {
        return files.stream().map(file -> file.getFileName().toString()).sorted().toList();
    }

    private static OpenLMessage single(java.util.Collection<OpenLMessage> messages) {
        assertEquals(1, messages.size(), messages::toString);
        return messages.iterator().next();
    }

    /** Puts an artifact with its POM into the remote repository. */
    private void publish(String groupId, String artifactId, String version, String extension, String dependencies)
            throws IOException {
        writeZip(artifactPath(groupId, artifactId, version, null, extension), Map.of("META-INF/MANIFEST.MF", ""));
        Files.writeString(artifactPath(groupId, artifactId, version, null, "pom"), """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>%s</groupId>
                    <artifactId>%s</artifactId>
                    <version>%s</version>
                    %s
                </project>
                """.formatted(groupId, artifactId, version, dependencies == null ? "" : dependencies));
    }

    /** Puts a parent POM into the remote repository. */
    private void publishParent(String groupId, String artifactId, String version) throws IOException {
        Files.writeString(artifactPath(groupId, artifactId, version, null, "pom"), """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>%s</groupId>
                    <artifactId>%s</artifactId>
                    <version>%s</version>
                    <packaging>pom</packaging>
                </project>
                """.formatted(groupId, artifactId, version));
    }

    /** Puts a jar whose POM inherits the given parent into the remote repository. */
    private void publishWithParent(String groupId, String artifactId, String version, String parent,
                                   String parentVersion) throws IOException {
        writeZip(artifactPath(groupId, artifactId, version, null, "jar"), Map.of("META-INF/MANIFEST.MF", ""));
        Files.writeString(artifactPath(groupId, artifactId, version, null, "pom"), """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                        <groupId>%s</groupId>
                        <artifactId>%s</artifactId>
                        <version>%s</version>
                    </parent>
                    <artifactId>%s</artifactId>
                    <version>%s</version>
                </project>
                """.formatted(groupId, parent, parentVersion, artifactId, version));
    }

    /** Puts an archive of an OpenL project into the remote repository, as the OpenL Maven Plugin packages it. */
    private void publishProject(String groupId, String artifactId, String version, String name) throws IOException {
        writeZip(artifactPath(groupId, artifactId, version, null, "zip"),
                Map.of("rules.xml", "<project><name>%s</name></project>".formatted(name)));
    }

    private Path artifactPath(String groupId, String artifactId, String version, String classifier, String extension)
            throws IOException {
        var folder = remote.resolve(groupId.replace('.', '/')).resolve(artifactId).resolve(version);
        Files.createDirectories(folder);
        var suffix = classifier == null ? "" : "-" + classifier;
        return folder.resolve(artifactId + "-" + version + suffix + "." + extension);
    }

    private static void writeZip(Path file, Map<String, String> entries) throws IOException {
        try (var zip = new ZipOutputStream(Files.newOutputStream(file))) {
            for (var entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue().getBytes());
                zip.closeEntry();
            }
        }
    }

    /** A clock the test moves forward. */
    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-11-19T12:00:00Z");

        void advance(java.time.Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
