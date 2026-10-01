package org.openl.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Created by ymolchan on 12.10.2015.
 */
class FileUtilsTest {

    /**
     * The content of a temporary file must stay unreadable by other local users, so the file the
     * content is written into must be the one that was created with those permissions.
     */
    @Test
    void testCopyToPrivateTempFileKeepsItPrivate() throws IOException {
        byte[] content = "the uploaded content".getBytes(StandardCharsets.UTF_8);
        var file = FileUtils.copyToPrivateTempFile(new ByteArrayInputStream(content), "openl-test", null, 1024);
        try {
            assertEquals(new String(content, StandardCharsets.UTF_8),
                    Files.readString(file, StandardCharsets.UTF_8));
            if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
                assertEquals("rw-------",
                        PosixFilePermissions.toString(Files.getPosixFilePermissions(file)));
            }
        } finally {
            FileUtils.deleteQuietly(file);
        }
        assertFalse(Files.exists(file));
    }

    /**
     * Content of an unknown length must not be able to fill the file system it is copied to, so the
     * copy stops at the bound and leaves nothing behind.
     */
    @Test
    void testCopyToPrivateTempFileStopsAtTheBound() throws IOException {
        var content = new ByteArrayInputStream(new byte[64 * 1024]);
        var thrown = assertThrows(FileUtils.ContentTooLargeException.class,
                () -> FileUtils.copyToPrivateTempFile(content, "openl-test", null, 1024));
        assertEquals(1024, thrown.getMaxBytes());
        try (var files = Files.list(Path.of(FileUtils.getTempDirectoryPath()))) {
            assertTrue(files.noneMatch(file -> file.getFileName().toString().startsWith("openl-test")));
        }
    }

    @Test
    void testCopyDirectory(@TempDir Path tempDir) throws IOException {
        var src = tempDir.resolve("src");
        Files.writeString(Files.createDirectories(src.resolve("nested")).resolve("file.txt"), "content");
        var dest = tempDir.resolve("dest");

        FileUtils.copy(src.toFile(), dest.toFile());

        assertEquals("content", Files.readString(dest.resolve("nested/file.txt")));
    }

    @Test
    void testCopyDirectoryToFile(@TempDir Path tempDir) throws IOException {
        var src = Files.createDirectories(tempDir.resolve("src"));
        var dest = Files.writeString(tempDir.resolve("dest"), "content").toFile();

        var thrown = assertThrows(IOException.class, () -> FileUtils.copy(src.toFile(), dest));
        assertEquals("Destination '" + dest + "' exists but is not a directory", thrown.getMessage());
    }

    @Test
    void testGetBaseName() {
        assertNull(FileUtils.getBaseName(null));

        assertEquals("", FileUtils.getBaseName(""));
        assertEquals("", FileUtils.getBaseName(".txt"));
        assertEquals("a", FileUtils.getBaseName("a.txt"));
        assertEquals("a.b", FileUtils.getBaseName("a.b.txt"));

        assertEquals("", FileUtils.getBaseName("/"));
        assertEquals("c", FileUtils.getBaseName("/c"));
        assertEquals("c", FileUtils.getBaseName("a/b/c.txt"));
        assertEquals("c", FileUtils.getBaseName("a/b/c"));
        assertEquals("", FileUtils.getBaseName("a/b/c/"));

        assertEquals("", FileUtils.getBaseName("\\"));
        assertEquals("c", FileUtils.getBaseName("\\c"));
        assertEquals("c", FileUtils.getBaseName("a\\b\\c.txt"));
        assertEquals("c", FileUtils.getBaseName("a\\b\\c"));
        assertEquals("", FileUtils.getBaseName("a\\b\\c\\"));
    }

    @Test
    void testGetName() {
        assertNull(FileUtils.getName(null));

        assertEquals("", FileUtils.getName(""));
        assertEquals(".txt", FileUtils.getName(".txt"));
        assertEquals("a.txt", FileUtils.getName("a.txt"));
        assertEquals("a.b.txt", FileUtils.getName("a.b.txt"));

        assertEquals("", FileUtils.getName("/"));
        assertEquals("c", FileUtils.getName("/c"));
        assertEquals("c.txt", FileUtils.getName("a/b/c.txt"));
        assertEquals("c", FileUtils.getName("a/b/c"));
        assertEquals("", FileUtils.getName("a/b/c/"));
        assertEquals("c", FileUtils.getName("a/b.txt/c"));
        assertEquals("c:1234567890", FileUtils.getName("a/b/c:1234567890"));

        assertEquals("", FileUtils.getName("\\"));
        assertEquals("c", FileUtils.getName("\\c"));
        assertEquals("c.txt", FileUtils.getName("a\\b\\c.txt"));
        assertEquals("c", FileUtils.getName("a\\b\\c"));
        assertEquals("", FileUtils.getName("a\\b\\c\\"));
        assertEquals("c", FileUtils.getName("a\\b.txt\\c"));
    }

    @Test
    void testGetExtension() {
        assertNull(FileUtils.getExtension(null));

        assertEquals("", FileUtils.getExtension(""));
        assertEquals("txt", FileUtils.getExtension(".txt"));
        assertEquals("txt", FileUtils.getExtension("a.txt"));
        assertEquals("txt", FileUtils.getExtension("a.b.txt"));

        assertEquals("", FileUtils.getExtension("/"));
        assertEquals("", FileUtils.getExtension("/c"));
        assertEquals("txt", FileUtils.getExtension("a/b/c.txt"));
        assertEquals("", FileUtils.getExtension("a/b/c"));
        assertEquals("", FileUtils.getExtension("a/b/c/"));
        assertEquals("", FileUtils.getExtension("a/b.txt/c"));

        assertEquals("", FileUtils.getExtension("\\"));
        assertEquals("", FileUtils.getExtension("\\c"));
        assertEquals("txt", FileUtils.getExtension("a\\b\\c.txt"));
        assertEquals("", FileUtils.getExtension("a\\b\\c"));
        assertEquals("", FileUtils.getExtension("a\\b\\c\\"));
        assertEquals("", FileUtils.getExtension("a\\b.txt\\c"));
    }

    @Test
    void testRemoveExtension() {
        assertNull(FileUtils.removeExtension(null));

        assertEquals("", FileUtils.removeExtension(""));
        assertEquals("", FileUtils.removeExtension(".txt"));
        assertEquals("a", FileUtils.removeExtension("a.txt"));
        assertEquals("a.b", FileUtils.removeExtension("a.b.txt"));

        assertEquals("/", FileUtils.removeExtension("/"));
        assertEquals("/c", FileUtils.removeExtension("/c"));
        assertEquals("a/b/c", FileUtils.removeExtension("a/b/c.txt"));
        assertEquals("a/b/c", FileUtils.removeExtension("a/b/c"));
        assertEquals("a/b/c/", FileUtils.removeExtension("a/b/c/"));
        assertEquals("a/b.txt/c", FileUtils.removeExtension("a/b.txt/c"));

        assertEquals("\\", FileUtils.removeExtension("\\"));
        assertEquals("\\c", FileUtils.removeExtension("\\c"));
        assertEquals("a\\b\\c", FileUtils.removeExtension("a\\b\\c.txt"));
        assertEquals("a\\b\\c", FileUtils.removeExtension("a\\b\\c"));
        assertEquals("a\\b\\c\\", FileUtils.removeExtension("a\\b\\c\\"));
        assertEquals("a\\b.txt\\c", FileUtils.removeExtension("a\\b.txt\\c"));
    }

    @Test
    void testPathMatchesNullArguments() {
        assertThrows(NullPointerException.class, () -> FileUtils.pathMatches(null, "test"));
        assertThrows(NullPointerException.class, () -> FileUtils.pathMatches("test", null));
        assertThrows(NullPointerException.class, () -> FileUtils.pathMatches(null, null));
    }

    @ParameterizedTest(name = "pathMatches({0}, {1}) = {2}")
    @CsvSource(delimiter = '|', textBlock = """
            # Test single character wildcard (?)
            com/t?st.jsp                            | com/test.jsp                                      | true
            com/t?st.jsp                            | com/tast.jsp                                      | true
            com/t?st.jsp                            | com/toast.jsp                                     | false
            com/t?st.jsp                            | com/test.jspx                                     | false
            # Test single asterisk wildcard (*)
            com/*.jsp                               | com/index.jsp                                     | true
            com/*.jsp                               | com/test.jsp                                      | true
            com/*.jsp                               | com/project/index.jsp                             | false
            com/*.jsp                               | com/index.html                                    | false
            # Test double asterisk wildcard (**)
            com/**/storage                          | com/index.jsp                                     | false
            com/**/storage                          | com/project/internal/storage                      | true
            com/**/storage                          | com/project/storage                               | true
            com/**/storage                          | com/storage                                       | true
            com/**/storage                          | com/storage/file.txt                              | false
            # Test mixed patterns
            src/**/*.java                           | src/main/java/com/example/MyClass.java            | true
            src/**/*.java                           | src/test/java/MyTest.java                         | true
            src/**/*.java                           | src/MyTest.java                                   | true
            src/**/*.java                           | test/MyTest.java                                  | false
            src/**/*.java                           | src/MyTest.java/META-INF                          | false
            # Test specific file patterns
            **/*Test.java                           | src/test/java/MyTest.java                         | true
            **/*Test.java                           | test/MyTest.java                                  | true
            **/*Test.java                           | test/MyTest.java/META-INF                         | false
            **/*Test.java                           | src/main/java/MyClass.java                        | false
            # Test directory-specific patterns
            **/config/*.yml                         | src/main/resources/config/application.yml         | true
            **/config/*.yml                         | config/database.yml                               | true
            **/config/*.yml                         | src/main/resources/application.yml                | false
            **/config/*.yml                         | src/config/resources/application.yml              | false
            # Test single character wildcard in specific positions
            src/main/java/com/example/MyClass?.java | src/main/java/com/example/MyClass1.java           | true
            src/main/java/com/example/MyClass?.java | src/main/java/com/example/MyClassA.java           | true
            src/main/java/com/example/MyClass?.java | src/main/java/com/example/MyClass.java            | false
            # Test path separator normalization
            src\\**\\*.java                         | src/main/java/com/example/MyClass.java            | true
            src/**/*.java                           | src\\main\\java\\com\\example\\MyClass.java       | true
            # Test edge cases
            *.java                                  | MyClass.java                                      | true
            *.java                                  | MyClass.class                                     | false
            **/*                                    | any/path/file.txt                                 | true
            **/*                                    | file.txt                                          | true
            **/*                                    | a/b/c/d/e/f.txt                                   | true
            # Test exact matches
            exact/path/file.txt                     | exact/path/file.txt                               | true
            exact/path/file.txt                     | exact/path/file.txtx                              | false
            # Test patterns with dots (should be escaped)
            src/**/*.properties                     | src/main/resources/application.properties         | true
            src/**/*.properties                     | src/main/resources/application_properties         | false
            # Test patterns with regex special characters
            src/**/test[1].java                     | src/test/java/test[1].java                        | true
            src/**/test(1).java                     | src/test/java/test(1).java                        | true
            src/**/test{1}.java                     | src/test/java/test{1}.java                        | true
            # Test complex nested patterns
            src/**/util/**/*.java                   | src/main/java/com/example/util/helper/Helper.java | true
            src/**/util/**/*.java                   | src/main/java/util/Utils.java                     | true
            src/**/util/**/*.java                   | src/util/java/example/Utils.java                  | true
            src/**/util/**/*.java                   | src/main/java/com/example/helper/Helper.java      | false
            # Test patterns with multiple wildcards
            src/**/test/**/*Test.java               | src/test/java/com/example/MyTest.java             | true
            src/**/test/**/*Test.java               | src/test/java/MyTest.java                         | true
            src/**/test/**/*Test.java               | src/main/java/MyClass.java                        | false
            # Test single asterisk in folder paths
            src/*/java/*.java                       | src/main/java/MyClass.java                        | true
            src/*/java/*.java                       | src/test/java/MyTest.java                         | true
            src/*/java/*.java                       | src/main/java/com/example/MyClass.java            | false
            src/*/java/*.java                       | src/main/resources/application.properties         | false
            # Test single asterisk in multiple folder levels
            src/*/java/*/example/*.java             | src/main/java/com/example/MyClass.java            | true
            src/*/java/*/example/*.java             | src/test/java/org/example/MyTest.java             | true
            src/*/java/*/example/*.java             | src/main/java/com/example/util/Helper.java        | false
            # Test single asterisk with specific folder names
            src/*/java/com/*.java                   | src/main/java/com/MyClass.java                    | true
            src/*/java/com/*.java                   | src/test/java/com/MyTest.java                     | true
            src/*/java/com/*.java                   | src/main/java/org/MyClass.java                    | false
            # Test ** preceded by specific symbols/characters
            src/main/**/*.java                      | src/main/java/com/example/MyClass.java            | true
            src/main/**/*.java                      | src/main/resources/config/MyClass.java            | true
            src/main/**/*.java                      | src/test/java/MyTest.java                         | false
            # Test ** preceded by folder name with special characters
            src/main-java/**/*.java                 | src/main-java/com/example/MyClass.java            | true
            src/main-java/**/*.java                 | src/main-java/util/Helper.java                    | true
            src/main-java/**/*.java                 | src/main/java/MyClass.java                        | false
            # Test ** preceded by underscore
            src/main_java/**/*.java                 | src/main_java/com/example/MyClass.java            | true
            src/main_java/**/*.java                 | src/main_java/util/Helper.java                    | true
            src/main_java/**/*.java                 | src/main/java/MyClass.java                        | false
            # Test ** preceded by numbers
            src/1.0/**/*.java                       | src/1.0/com/example/MyClass.java                  | true
            src/1.0/**/*.java                       | src/1.0/util/Helper.java                          | true
            src/1.0/**/*.java                       | src/2.0/com/example/MyClass.java                  | false
            # Test ** preceded by dot
            src/.hidden/**/*.java                   | src/.hidden/com/example/MyClass.java              | true
            src/.hidden/**/*.java                   | src/.hidden/util/Helper.java                      | true
            src/.hidden/**/*.java                   | src/visible/com/example/MyClass.java              | false
            # Test ** preceded by multiple characters
            src/main-java-1.0/**/*.java             | src/main-java-1.0/com/example/MyClass.java        | true
            src/main-java-1.0/**/*.java             | src/main-java-1.0/util/Helper.java                | true
            src/main-java-1.0/**/*.java             | src/main-java-2.0/com/example/MyClass.java        | false
            # Test ** preceded by regex special characters (should be escaped)
            src/test[1]/**/*.java                   | src/test[1]/com/example/MyClass.java              | true
            src/test[1]/**/*.java                   | src/test[1]/util/Helper.java                      | true
            src/test[1]/**/*.java                   | src/test[2]/com/example/MyClass.java              | false
            # Test ** preceded by parentheses
            src/(main)/**/*.java                    | src/(main)/com/example/MyClass.java               | true
            src/(main)/**/*.java                    | src/(main)/util/Helper.java                       | true
            src/(main)/**/*.java                    | src/main/com/example/MyClass.java                 | false
            # Test ** preceded by curly braces
            src/{main}/**/*.java                    | src/{main}/com/example/MyClass.java               | true
            src/{main}/**/*.java                    | src/{main}/util/Helper.java                       | true
            src/{main}/**/*.java                    | src/main/com/example/MyClass.java                 | false
            """)
    void testPathMatches(String pattern, String path, boolean matches) {
        assertEquals(matches, FileUtils.pathMatches(pattern, path));
    }

    @Test
    void testPathMatchesExactMatch() {
        assertTrue(FileUtils.pathMatches("calculate", "calculate"));
        assertFalse(FileUtils.pathMatches("calculate", "calculateAge"));
        assertFalse(FileUtils.pathMatches("calculateAge", "calculate"));
    }

    @Test
    void testPathMatchesStarWildcard() {
        assertTrue(FileUtils.pathMatches("*", "anything"));
        assertTrue(FileUtils.pathMatches("*", ""));
        assertTrue(FileUtils.pathMatches("get*", "getName"));
        assertTrue(FileUtils.pathMatches("get*", "get"));
        assertFalse(FileUtils.pathMatches("get*", "setName"));
        assertTrue(FileUtils.pathMatches("*Name", "getName"));
        assertTrue(FileUtils.pathMatches("*Name", "Name"));
        assertFalse(FileUtils.pathMatches("*Name", "getNames"));
        assertTrue(FileUtils.pathMatches("get*Name", "getName"));
        assertTrue(FileUtils.pathMatches("get*Name", "getFullName"));
        assertFalse(FileUtils.pathMatches("get*Name", "getNames"));
    }

    @Test
    void testPathMatchesQuestionMarkWildcard() {
        assertTrue(FileUtils.pathMatches("?etName", "getName"));
        assertTrue(FileUtils.pathMatches("?etName", "setName"));
        assertFalse(FileUtils.pathMatches("?etName", "etName"));
        assertFalse(FileUtils.pathMatches("?etName", "abetName"));
        assertTrue(FileUtils.pathMatches("calc?late", "calculate"));
        assertFalse(FileUtils.pathMatches("calc?late", "calclate"));
    }

    @Test
    void testPathMatchesCombinedWildcards() {
        assertTrue(FileUtils.pathMatches("get*?", "getName"));
        assertTrue(FileUtils.pathMatches("?et*", "getName"));
        assertTrue(FileUtils.pathMatches("?et*", "set"));
        assertFalse(FileUtils.pathMatches("?et*", "ge"));
    }

    @Test
    void testPathMatchesRegexSpecialCharsAreEscaped() {
        assertTrue(FileUtils.pathMatches("calc.premium", "calc.premium"));
        assertFalse(FileUtils.pathMatches("calc.premium", "calcXpremium"));
        assertTrue(FileUtils.pathMatches("method()", "method()"));
        assertFalse(FileUtils.pathMatches("method()", "methodXY"));
    }
}
