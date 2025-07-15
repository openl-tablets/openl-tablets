package org.openl.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Created by ymolchan on 12.10.2015.
 */
public class FileUtilsTest {

    @Test
    public void testGetBaseName() throws Exception {
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
    public void testGetName() throws Exception {
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
    public void testGetExtension() throws Exception {
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
    public void testRemoveExtension() throws Exception {
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
    public void testPathMatches() {
        // Test null parameters
        assertThrows(NullPointerException.class, () -> FileUtils.pathMatches(null, "test"));
        assertThrows(NullPointerException.class, () -> FileUtils.pathMatches("test", null));
        assertThrows(NullPointerException.class, () -> FileUtils.pathMatches(null, null));

        // Test single character wildcard (?)
        assertTrue(FileUtils.pathMatches("com/t?st.jsp", "com/test.jsp"));
        assertTrue(FileUtils.pathMatches("com/t?st.jsp", "com/tast.jsp"));
        assertFalse(FileUtils.pathMatches("com/t?st.jsp", "com/toast.jsp"));
        assertFalse(FileUtils.pathMatches("com/t?st.jsp", "com/test.jspx"));

        // Test single asterisk wildcard (*)
        assertTrue(FileUtils.pathMatches("com/*.jsp", "com/index.jsp"));
        assertTrue(FileUtils.pathMatches("com/*.jsp", "com/test.jsp"));
        assertFalse(FileUtils.pathMatches("com/*.jsp", "com/project/index.jsp"));
        assertFalse(FileUtils.pathMatches("com/*.jsp", "com/index.html"));

        // Test double asterisk wildcard (**)
        assertFalse(FileUtils.pathMatches("com/**/storage", "com/index.jsp"));
        assertTrue(FileUtils.pathMatches("com/**/storage", "com/project/internal/storage"));
        assertTrue(FileUtils.pathMatches("com/**/storage", "com/project/storage"));
        assertTrue(FileUtils.pathMatches("com/**/storage", "com/storage"));
        assertFalse(FileUtils.pathMatches("com/**/storage", "com/storage/file.txt"));

        // Test mixed patterns
        assertTrue(FileUtils.pathMatches("src/**/*.java", "src/main/java/com/example/MyClass.java"));
        assertTrue(FileUtils.pathMatches("src/**/*.java", "src/test/java/MyTest.java"));
        assertTrue(FileUtils.pathMatches("src/**/*.java", "src/MyTest.java"));
        assertFalse(FileUtils.pathMatches("src/**/*.java", "test/MyTest.java"));
        assertFalse(FileUtils.pathMatches("src/**/*.java", "src/MyTest.java/META-INF"));

        // Test specific file patterns
        assertTrue(FileUtils.pathMatches("**/*Test.java", "src/test/java/MyTest.java"));
        assertTrue(FileUtils.pathMatches("**/*Test.java", "test/MyTest.java"));
        assertFalse(FileUtils.pathMatches("**/*Test.java", "test/MyTest.java/META-INF"));
        assertFalse(FileUtils.pathMatches("**/*Test.java", "src/main/java/MyClass.java"));

        // Test directory-specific patterns
        assertTrue(FileUtils.pathMatches("**/config/*.yml", "src/main/resources/config/application.yml"));
        assertTrue(FileUtils.pathMatches("**/config/*.yml", "config/database.yml"));
        assertFalse(FileUtils.pathMatches("**/config/*.yml", "src/main/resources/application.yml"));
        assertFalse(FileUtils.pathMatches("**/config/*.yml", "src/config/resources/application.yml"));

        // Test single character wildcard in specific positions
        assertTrue(FileUtils.pathMatches("src/main/java/com/example/MyClass?.java", "src/main/java/com/example/MyClass1.java"));
        assertTrue(FileUtils.pathMatches("src/main/java/com/example/MyClass?.java", "src/main/java/com/example/MyClassA.java"));
        assertFalse(FileUtils.pathMatches("src/main/java/com/example/MyClass?.java", "src/main/java/com/example/MyClass.java"));

        // Test path separator normalization
        assertTrue(FileUtils.pathMatches("src\\**\\*.java", "src/main/java/com/example/MyClass.java"));
        assertTrue(FileUtils.pathMatches("src/**/*.java", "src\\main\\java\\com\\example\\MyClass.java"));

        // Test edge cases
        assertTrue(FileUtils.pathMatches("*.java", "MyClass.java"));
        assertFalse(FileUtils.pathMatches("*.java", "MyClass.class"));
        assertTrue(FileUtils.pathMatches("**/*", "any/path/file.txt"));
        assertTrue(FileUtils.pathMatches("**/*", "file.txt"));
        assertTrue(FileUtils.pathMatches("**/*", "a/b/c/d/e/f.txt"));

        // Test exact matches
        assertTrue(FileUtils.pathMatches("exact/path/file.txt", "exact/path/file.txt"));
        assertFalse(FileUtils.pathMatches("exact/path/file.txt", "exact/path/file.txtx"));

        // Test patterns with dots (should be escaped)
        assertTrue(FileUtils.pathMatches("src/**/*.properties", "src/main/resources/application.properties"));
        assertFalse(FileUtils.pathMatches("src/**/*.properties", "src/main/resources/application_properties"));

        // Test patterns with regex special characters
        assertTrue(FileUtils.pathMatches("src/**/test[1].java", "src/test/java/test[1].java"));
        assertTrue(FileUtils.pathMatches("src/**/test(1).java", "src/test/java/test(1).java"));
        assertTrue(FileUtils.pathMatches("src/**/test{1}.java", "src/test/java/test{1}.java"));

        // Test complex nested patterns
        assertTrue(FileUtils.pathMatches("src/**/util/**/*.java", "src/main/java/com/example/util/helper/Helper.java"));
        assertTrue(FileUtils.pathMatches("src/**/util/**/*.java", "src/main/java/util/Utils.java"));
        assertTrue(FileUtils.pathMatches("src/**/util/**/*.java", "src/util/java/example/Utils.java"));
        assertFalse(FileUtils.pathMatches("src/**/util/**/*.java", "src/main/java/com/example/helper/Helper.java"));

        // Test patterns with multiple wildcards
        assertTrue(FileUtils.pathMatches("src/**/test/**/*Test.java", "src/test/java/com/example/MyTest.java"));
        assertTrue(FileUtils.pathMatches("src/**/test/**/*Test.java", "src/test/java/MyTest.java"));
        assertFalse(FileUtils.pathMatches("src/**/test/**/*Test.java", "src/main/java/MyClass.java"));

        // Test single asterisk in folder paths
        assertTrue(FileUtils.pathMatches("src/*/java/*.java", "src/main/java/MyClass.java"));
        assertTrue(FileUtils.pathMatches("src/*/java/*.java", "src/test/java/MyTest.java"));
        assertFalse(FileUtils.pathMatches("src/*/java/*.java", "src/main/java/com/example/MyClass.java"));
        assertFalse(FileUtils.pathMatches("src/*/java/*.java", "src/main/resources/application.properties"));

        // Test single asterisk in multiple folder levels
        assertTrue(FileUtils.pathMatches("src/*/java/*/example/*.java", "src/main/java/com/example/MyClass.java"));
        assertTrue(FileUtils.pathMatches("src/*/java/*/example/*.java", "src/test/java/org/example/MyTest.java"));
        assertFalse(FileUtils.pathMatches("src/*/java/*/example/*.java", "src/main/java/com/example/util/Helper.java"));

        // Test single asterisk with specific folder names
        assertTrue(FileUtils.pathMatches("src/*/java/com/*.java", "src/main/java/com/MyClass.java"));
        assertTrue(FileUtils.pathMatches("src/*/java/com/*.java", "src/test/java/com/MyTest.java"));
        assertFalse(FileUtils.pathMatches("src/*/java/com/*.java", "src/main/java/org/MyClass.java"));

        // Test ** preceded by specific symbols/characters
        assertTrue(FileUtils.pathMatches("src/main/**/*.java", "src/main/java/com/example/MyClass.java"));
        assertTrue(FileUtils.pathMatches("src/main/**/*.java", "src/main/resources/config/MyClass.java"));
        assertFalse(FileUtils.pathMatches("src/main/**/*.java", "src/test/java/MyTest.java"));

        // Test ** preceded by folder name with special characters
        assertTrue(FileUtils.pathMatches("src/main-java/**/*.java", "src/main-java/com/example/MyClass.java"));
        assertTrue(FileUtils.pathMatches("src/main-java/**/*.java", "src/main-java/util/Helper.java"));
        assertFalse(FileUtils.pathMatches("src/main-java/**/*.java", "src/main/java/MyClass.java"));

        // Test ** preceded by underscore
        assertTrue(FileUtils.pathMatches("src/main_java/**/*.java", "src/main_java/com/example/MyClass.java"));
        assertTrue(FileUtils.pathMatches("src/main_java/**/*.java", "src/main_java/util/Helper.java"));
        assertFalse(FileUtils.pathMatches("src/main_java/**/*.java", "src/main/java/MyClass.java"));

        // Test ** preceded by numbers
        assertTrue(FileUtils.pathMatches("src/1.0/**/*.java", "src/1.0/com/example/MyClass.java"));
        assertTrue(FileUtils.pathMatches("src/1.0/**/*.java", "src/1.0/util/Helper.java"));
        assertFalse(FileUtils.pathMatches("src/1.0/**/*.java", "src/2.0/com/example/MyClass.java"));

        // Test ** preceded by dot
        assertTrue(FileUtils.pathMatches("src/.hidden/**/*.java", "src/.hidden/com/example/MyClass.java"));
        assertTrue(FileUtils.pathMatches("src/.hidden/**/*.java", "src/.hidden/util/Helper.java"));
        assertFalse(FileUtils.pathMatches("src/.hidden/**/*.java", "src/visible/com/example/MyClass.java"));

        // Test ** preceded by multiple characters
        assertTrue(FileUtils.pathMatches("src/main-java-1.0/**/*.java", "src/main-java-1.0/com/example/MyClass.java"));
        assertTrue(FileUtils.pathMatches("src/main-java-1.0/**/*.java", "src/main-java-1.0/util/Helper.java"));
        assertFalse(FileUtils.pathMatches("src/main-java-1.0/**/*.java", "src/main-java-2.0/com/example/MyClass.java"));

        // Test ** preceded by regex special characters (should be escaped)
        assertTrue(FileUtils.pathMatches("src/test[1]/**/*.java", "src/test[1]/com/example/MyClass.java"));
        assertTrue(FileUtils.pathMatches("src/test[1]/**/*.java", "src/test[1]/util/Helper.java"));
        assertFalse(FileUtils.pathMatches("src/test[1]/**/*.java", "src/test[2]/com/example/MyClass.java"));

        // Test ** preceded by parentheses
        assertTrue(FileUtils.pathMatches("src/(main)/**/*.java", "src/(main)/com/example/MyClass.java"));
        assertTrue(FileUtils.pathMatches("src/(main)/**/*.java", "src/(main)/util/Helper.java"));
        assertFalse(FileUtils.pathMatches("src/(main)/**/*.java", "src/main/com/example/MyClass.java"));

        // Test ** preceded by curly braces
        assertTrue(FileUtils.pathMatches("src/{main}/**/*.java", "src/{main}/com/example/MyClass.java"));
        assertTrue(FileUtils.pathMatches("src/{main}/**/*.java", "src/{main}/util/Helper.java"));
        assertFalse(FileUtils.pathMatches("src/{main}/**/*.java", "src/main/com/example/MyClass.java"));
    }
}
