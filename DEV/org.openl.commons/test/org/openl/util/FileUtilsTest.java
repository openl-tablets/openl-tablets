package org.openl.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Created by ymolchan on 12.10.2015.
 */
public class FileUtilsTest {

    @Test
    public void testGetBaseName() throws Exception {
        assertEquals(null, FileUtils.getBaseName(null));

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
        assertEquals(null, FileUtils.getName(null));

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
        assertEquals(null, FileUtils.getExtension(null));

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
        assertEquals(null, FileUtils.removeExtension(null));

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
}