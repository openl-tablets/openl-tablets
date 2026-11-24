package org.openl.rules.repository.zip;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

import org.openl.rules.repository.api.FileData;

/**
 * Runs with the file system Spring Boot provides for the {@code nested:} URIs of its executable jars.
 *
 * @author Yury Molchan
 */
class ClasspathArchivesTest {

    @TempDir
    private Path tempDir;
    private final ClasspathArchives archives = new ClasspathArchives();
    private byte[] rulesJar;
    private byte[] sampleZip;
    private String appJar;

    @BeforeEach
    void setUp() throws IOException {
        rulesJar = zip(Map.of("rules.xml", bytes("<project/>"), "rules/Main.xlsx", bytes("main")), false);
        sampleZip = zip(Map.of("rules.xml", bytes("<project/>"), "Sample.xlsx", bytes("sample")), false);
        // Spring Boot stores the nested archives of an executable jar without compression
        var app = zip(Map.of("BOOT-INF/classes/rules.xml", bytes("<project/>"),
                "BOOT-INF/classes/openl/sample.zip", sampleZip,
                "BOOT-INF/lib/rules.jar", rulesJar,
                "BOOT-INF/lib/other/rules.jar", zip(Map.of("rules.xml", bytes("<other/>")), false),
                "BOOT-INF/lib/samples.jar", zip(Map.of("openl/sample.zip", sampleZip), true)), true);
        appJar = Files.write(Files.createDirectories(tempDir.resolve("my app")).resolve("app.jar"), app)
                .toUri()
                .getRawPath();
    }

    @Test
    void nestedJarOfSpringBootIsReadInPlace() throws IOException {
        var path = archives.toPath(nested("BOOT-INF/lib/rules.jar!/rules.xml"), true);

        assertEquals("nested", path.toUri().getScheme());
        assertArrayEquals(rulesJar, Files.readAllBytes(path));
        assertEquals(path, archives.toPath(nested("BOOT-INF/lib/rules.jar!/rules.xml"), true));
    }

    @Test
    void archiveInClassesOfSpringBootIsReadInPlace() throws IOException {
        var path = archives.toPath(nested("BOOT-INF/classes/!/openl/sample.zip"), false);

        assertEquals("nested", path.toUri().getScheme());
        assertArrayEquals(sampleZip, Files.readAllBytes(path));
    }

    @Test
    void archiveInNestedJarOfSpringBootIsSkipped() throws IOException {
        assertNull(archives.toPath(nested("BOOT-INF/lib/samples.jar!/openl/sample.zip"), false));
    }

    @Test
    void compressedArchiveOfSpringBootIsSkipped() throws IOException {
        var compressed = zip(Map.of("BOOT-INF/lib/rules.jar", rulesJar), false);
        var app = Files.write(tempDir.resolve("compressed.jar"), compressed);

        assertNull(archives.toPath(URI.create("jar:nested:" + app.toUri().getRawPath()
                + "/!BOOT-INF/lib/rules.jar!/rules.xml"), true));
    }

    @Test
    void absentArchiveOfSpringBootIsSkipped() throws IOException {
        assertNull(archives.toPath(nested("BOOT-INF/lib/absent.jar!/rules.xml"), true));
    }

    @Test
    void repositoryReadsNestedJarOfSpringBoot() throws IOException {
        var path = archives.toPath(nested("BOOT-INF/lib/rules.jar!/rules.xml"), true);
        try (var repository = new AbstractArchiveRepository(Map.of("rules", path)) {
        }) {
            var folders = repository.listFolders("");
            assertEquals(1, folders.size());
            assertEquals("rules", folders.getFirst().getName());
            var files = repository.list("rules").stream().map(FileData::getName).sorted().toList();
            assertEquals(List.of("rules/rules.xml", "rules/rules/Main.xlsx"), files);
            try (var item = repository.read("rules/rules/Main.xlsx")) {
                assertNotNull(item);
                assertArrayEquals(bytes("main"), item.getStream().readAllBytes());
            }
        }
    }

    @Test
    void descriptorInClassesOfSpringBootIsSkipped() throws IOException {
        assertNull(archives.toPath(nested("BOOT-INF/classes/!/rules.xml"), true));
        assertNull(archives.toPath(classic("BOOT-INF/classes!/rules.xml"), true));
    }

    @Test
    void nestedJarOfClassicSpringBootLoaderIsSkipped() throws IOException {
        assertNull(archives.toPath(classic("BOOT-INF/lib/rules.jar!/rules.xml"), true));
        assertNull(archives.toPath(classic("BOOT-INF/classes!/openl/sample.zip"), false));
    }

    @Test
    void nestedJarsOfTheSameNameAreKeptApart() throws IOException {
        var first = archives.toPath(nested("BOOT-INF/lib/rules.jar!/rules.xml"), true);
        var second = archives.toPath(nested("BOOT-INF/lib/other/rules.jar!/rules.xml"), true);

        assertNotNull(first);
        assertNotNull(second);
        assertNotEquals(first, second);
    }

    @Test
    void archiveOfTheFileSystemStandsForItself() throws IOException {
        var jar = Files.write(tempDir.resolve("rules.jar"), rulesJar);
        var zip = Files.write(tempDir.resolve("sample.zip"), sampleZip);

        assertEquals(jar, archives.toPath(URI.create("jar:" + jar.toUri() + "!/rules.xml"), true));
        assertEquals(zip, archives.toPath(zip.toUri(), false));
    }

    @Test
    void descriptorInFolderOfTheClasspathIsSkipped() throws IOException {
        var descriptor = Files.writeString(Files.createDirectories(tempDir.resolve("classes")).resolve("rules.xml"),
                "<project/>");

        assertNull(archives.toPath(descriptor.toUri(), true));
    }

    @Test
    void archiveInJarOfTheFileSystemIsSkipped() throws IOException {
        var jar = Files.write(tempDir.resolve("lib.jar"), zip(Map.of("openl/sample.zip", sampleZip), false));

        assertNull(archives.toPath(URI.create("jar:" + jar.toUri() + "!/openl/sample.zip"), false));
    }

    @Test
    void unsupportedScheme() {
        assertThrows(IllegalArgumentException.class,
                () -> archives.toPath(URI.create("http://localhost/rules.jar"), false));
    }

    @Test
    void deploysJarsOfTheClasspath() throws Exception {
        var names = listProjects(jarIn("a", "first.jar"), jarIn("b", "second.jar")).stream()
                .map(FileData::getName)
                .sorted()
                .toList();

        assertEquals(List.of("first", "second"), names);
    }

    @Test
    void jarsOfTheSameNameConflict() throws Exception {
        var first = jarIn("a", "rules.jar");
        var second = jarIn("b", "rules.jar");

        var e = assertThrows(IllegalStateException.class, () -> listProjects(first, second));
        assertTrue(e.getMessage().contains("conflict for the same 'rules' name"), e.getMessage());
    }

    @Test
    void jarsOfNamesDifferingInCaseConflict() throws Exception {
        var first = jarIn("a", "Rules.jar");
        var second = jarIn("b", "rules.jar");

        var e = assertThrows(IllegalStateException.class, () -> listProjects(first, second));
        assertTrue(e.getMessage().contains("conflict for the same"), e.getMessage());
    }

    @Test
    void namesTheRepositoryKeepsApartAreDeployed() throws Exception {
        // "I" and the dotless "\u0131" lowercase differently, so the repository looks them up apart
        var names = listProjects(jarIn("a", "I.jar"), jarIn("b", "\u0131.jar")).stream()
                .map(FileData::getName)
                .sorted()
                .toList();

        assertEquals(List.of("I", "\u0131"), names);
    }

    @Test
    void namesTheRepositoryTakesAsOneConflict() throws Exception {
        // "\u0130" lowercases to "i" with a combining dot, the same as the second name
        var first = jarIn("a", "\u0130.jar");
        var second = jarIn("b", "i\u0307.jar");

        var e = assertThrows(IllegalStateException.class, () -> listProjects(first, second));
        assertTrue(e.getMessage().contains("conflict for the same"), e.getMessage());
    }

    @Test
    void folderNamedWithPercentSequenceIsKept() throws IOException {
        var jar = Files.write(Files.createDirectories(tempDir.resolve("app%20v1")).resolve("rules.jar"), rulesJar);

        assertEquals(jar, archives.toPath(URI.create("jar:" + jar.toUri() + "!/rules.xml"), true));
    }

    @Test
    void uriEncodedTwiceIsDecodedTwice() throws IOException {
        var jar = Files.write(Files.createDirectories(tempDir.resolve("my app")).resolve("rules.jar"), rulesJar);
        var encodedTwice = jar.toUri().toString().replace("%20", "%2520");

        assertEquals(jar, archives.toPath(URI.create("jar:" + encodedTwice + "!/rules.xml"), true));
    }

    @Test
    void archivesAreListedOnceAndUnreadableOnesSkipped() throws Exception {
        var jar = Files.write(tempDir.resolve("rules.jar"), rulesJar);
        var descriptor = resource(URI.create("jar:" + jar.toUri() + "!/rules.xml"));
        var unknown = unknownResource();
        var resolver = mock(ResourcePatternResolver.class);
        when(resolver.getResources(any(String.class))).thenReturn(new Resource[]{descriptor, unknown},
                new Resource[]{descriptor},
                new Resource[0]);

        assertEquals(List.of(jar), new ClasspathArchives(resolver).archives());
    }

    @Test
    void repositoryFailsOnUnreadableResource() throws Exception {
        var unknown = unknownResource();
        var resolver = mock(ResourcePatternResolver.class);
        when(resolver.getResources(any(String.class))).thenReturn(new Resource[]{unknown});

        assertThrows(IllegalArgumentException.class, new ClasspathArchives(resolver)::find);
    }

    @Test
    void failedLookupSkipsOnlyItsOwnResources() throws Exception {
        var jar = Files.write(tempDir.resolve("rules.jar"), rulesJar);
        var descriptor = resource(URI.create("jar:" + jar.toUri() + "!/rules.xml"));
        var resolver = mock(ResourcePatternResolver.class);
        when(resolver.getResources(any(String.class))).thenReturn(new Resource[]{descriptor})
                .thenThrow(new IllegalStateException("A class loader failed"))
                .thenReturn(new Resource[0]);

        assertEquals(List.of(jar), new ClasspathArchives(resolver).archives());
    }

    @Test
    void repositoryFailsOnFailedLookup() throws Exception {
        var resolver = mock(ResourcePatternResolver.class);
        when(resolver.getResources(any(String.class))).thenThrow(new IllegalStateException("A class loader failed"));

        assertThrows(IllegalStateException.class, new ClasspathArchives(resolver)::find);
    }

    @Test
    void unreadableClasspathListsNothing() throws Exception {
        var resolver = mock(ResourcePatternResolver.class);
        when(resolver.getResources(any(String.class))).thenThrow(new IOException("The classpath cannot be read"));

        assertEquals(List.of(), new ClasspathArchives(resolver).archives());
    }

    /**
     * A resource of a class loader handing out URIs of a scheme no file system reads.
     */
    private static Resource unknownResource() throws IOException {
        return resource(URI.create("vfs:/content/rules.jar/rules.xml"));
    }

    private static Resource resource(URI uri) throws IOException {
        var resource = mock(Resource.class);
        when(resource.getURI()).thenReturn(uri);
        return resource;
    }

    /**
     * A URI of Spring Boot 3.2+ to the entries of the executable jar.
     */
    private URI nested(String entries) {
        return URI.create("jar:nested:" + appJar + "/!" + entries);
    }

    /**
     * A URI of the classic loader of Spring Boot to the entries of the executable jar.
     */
    private URI classic(String entries) {
        return URI.create("jar:file:" + appJar + "!/" + entries);
    }

    private Path jarIn(String folder, String name) throws IOException {
        return Files.write(Files.createDirectories(tempDir.resolve(folder)).resolve(name), rulesJar);
    }

    private List<FileData> listProjects(Path... jars) throws Exception {
        var urls = new URL[jars.length];
        for (int i = 0; i < jars.length; i++) {
            urls[i] = jars[i].toUri().toURL();
        }
        var thread = Thread.currentThread();
        var contextClassLoader = thread.getContextClassLoader();
        try (var classLoader = new URLClassLoader(urls, null)) {
            thread.setContextClassLoader(classLoader);
            try (var jarRepository = new JarLocalRepository()) {
                return jarRepository.listFolders("");
            }
        } finally {
            thread.setContextClassLoader(contextClassLoader);
        }
    }

    private static byte[] zip(Map<String, byte[]> entries, boolean stored) throws IOException {
        var out = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(out)) {
            for (var entry : entries.entrySet()) {
                var zipEntry = new ZipEntry(entry.getKey());
                var content = entry.getValue();
                if (stored) {
                    var crc = new CRC32();
                    crc.update(content);
                    zipEntry.setMethod(ZipEntry.STORED);
                    zipEntry.setSize(content.length);
                    zipEntry.setCrc(crc.getValue());
                }
                zip.putNextEntry(zipEntry);
                zip.write(content);
                zip.closeEntry();
            }
        }
        return out.toByteArray();
    }

    private static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }
}
