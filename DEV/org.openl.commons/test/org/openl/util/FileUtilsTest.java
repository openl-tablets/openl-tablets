package org.openl.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

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
    public void normalizePathTest() {
        assertEquals("/foo/", FileUtils.normalizePath("/foo/"));
        assertEquals("/foo/", FileUtils.normalizePath("\\foo\\"));
        assertEquals("foo", FileUtils.normalizePath("foo"));
        assertEquals("/foo/bar", FileUtils.normalizePath("/foo\\\\bar"));
        assertEquals("", FileUtils.normalizePath(""));
        assertNull(FileUtils.normalizePath(null));
    }

    @Test
    public void getValidPathTest() throws Exception {
        assertEquals(Paths.get("/foo").toString(), FileUtils.getValidPath("/foo").toString());
        assertEquals(Paths.get("foo/bar").toString(), FileUtils.getValidPath("foo", "bar").toString());
        assertEquals(Paths.get("/foo/.openl").toString(), FileUtils.getValidPath("/foo/.openl").toString());

        assertInvalid("../..", () -> FileUtils.getValidPath("../../"));
        assertInvalid("../foo", () -> FileUtils.getValidPath("../", "foo"));
        assertInvalid("foo/../bar", () -> FileUtils.getValidPath("foo", "../bar"));
    }

    @Test
    public void resolveValidPath() throws Exception {
        final Path parent = Paths.get("foo").toAbsolutePath();
        assertInvalid("../bar", () -> FileUtils.resolveValidPath(parent, "../", "bar"));
        assertOutside("/bar", () -> FileUtils.resolveValidPath(parent, "/bar"));
        assertEquals(parent.resolve("bar/bar").toString(), FileUtils.resolveValidPath(parent, "bar/bar").toString());
    }

    public static void assertInvalid(String suffix, Executable exec) throws Exception {
        try {
            exec.execute();
            fail("Expected ValidatorException but nothing was thrown");
        } catch (InvalidPathException e) {
            assertEquals("Resulted path does not match canonical: " + suffix, e.getMessage());
        }
    }

    public static void assertOutside(String suffix, Executable exec) throws Exception {
        try {
            exec.execute();
            fail("Expected ValidatorException but nothing was thrown");
        } catch (InvalidPathException e) {
            assertEquals("Resulted path is outside of parent: " + suffix, e.getMessage());
        }
    }

    public interface Executable {

        void execute() throws Exception;

    }
}