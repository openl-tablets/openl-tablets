package org.openl.util;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.Test;

public class ZipUtilsTest {
    @Test
    public void testContainsMethodReleasesResources() throws IOException {
        File tempFolder = Files.createTempDirectory("openl").toFile();
        try {
            final File file = new File(tempFolder, "test.txt");
            assertTrue(file.createNewFile());

            final File zipFile = new File(tempFolder, "archive.zip");
            ZipUtils.archive(tempFolder, zipFile);

            assertTrue(ZipUtils.contains(zipFile, name -> name.equals("test.txt")));

            assertTrue(zipFile.delete());
            assertFalse(zipFile.exists());
        } finally {
            // If can't delete the folder, we must fail, because folder is locked.
            FileUtils.delete(tempFolder);
            assertFalse(tempFolder.exists());
        }
    }
}