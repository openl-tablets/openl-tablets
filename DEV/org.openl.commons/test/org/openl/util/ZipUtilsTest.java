package org.openl.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ZipUtilsTest {

    @TempDir
    File tempFolder;

    @Test
    public void testContainsMethodReleasesResources() throws IOException {
        final File file = new File(tempFolder, "test.txt");
        assertTrue(file.createNewFile());

        final File zipFile = new File(tempFolder, "archive.zip");
        ZipUtils.archive(tempFolder, zipFile);

        assertTrue(ZipUtils.contains(zipFile, name -> name.equals("test.txt")));

        assertTrue(zipFile.delete());
        assertFalse(zipFile.exists());
    }
}