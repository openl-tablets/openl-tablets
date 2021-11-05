package org.openl.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

    @Test
    public void testHackZip() throws IOException {
        Path temp = Files.createTempDirectory("openl");
        try {
            Map<String, byte[]> singleDeployment = new HashMap<>();
            singleDeployment.put("rules.xml", "foo".getBytes());
            singleDeployment.put("rules/../../Algorithm.xlsx", "bar".getBytes());
            generateZipFile(temp, "hackedZip.zip", singleDeployment);
            ZipUtils.extractAll(temp.resolve("hackedZip.zip").toFile(), temp.resolve("extracted").toFile());
            fail("An error is expected");
        } catch (InvalidPathException e) {
            assertEquals("Resulted path does not match canonical: rules/../../Algorithm.xlsx", e.getMessage());
        } finally {
            FileUtils.delete(temp);
            assertFalse(Files.exists(temp));
        }
    }

    @Test
    public void testHackZip2() throws IOException {
        Path temp = Files.createTempDirectory("openl");
        try {
            Map<String, byte[]> singleDeployment = new HashMap<>();
            singleDeployment.put("rules.xml", "foo".getBytes());
            singleDeployment.put("/rules/Algorithm.xlsx", "bar".getBytes());
            generateZipFile(temp, "hackedZip.zip", singleDeployment);
            ZipUtils.extractAll(temp.resolve("hackedZip.zip").toFile(), temp.resolve("extracted").toFile());
            fail("An error is expected");
        } catch (InvalidPathException e) {
            assertEquals("Resulted path is outside of parent: /rules/Algorithm.xlsx", e.getMessage());
        } finally {
            FileUtils.delete(temp);
            assertFalse(Files.exists(temp));
        }
    }

    @Test
    public void testExtractZip() throws IOException {
        Path temp = Files.createTempDirectory("openl");
        try {
            Map<String, byte[]> singleDeployment = new HashMap<>();
            singleDeployment.put("rules.xml", "foo".getBytes());
            singleDeployment.put("rules/Algorithm.xlsx", "bar".getBytes());
            singleDeployment.put("rules/subfolder/Algorithm.xlsx", "bar".getBytes());
            generateZipFile(temp, "toExtract.zip", singleDeployment);
            Path dest = temp.resolve("toExtract");
            ZipUtils.extractAll(temp.resolve("toExtract.zip").toFile(), dest.toFile());

            for (Map.Entry<String, byte[]> entry : singleDeployment.entrySet()) {
                Path file = dest.resolve(entry.getKey());
                assertTrue(entry.getKey(), Files.exists(file));
                assertArrayEquals(entry.getValue(), IOUtils.toStringAndClose(Files.newInputStream(file)).getBytes());
            }
        } finally {
            FileUtils.delete(temp);
            assertFalse(Files.exists(temp));
        }
    }

    private void generateZipFile(Path zipFilePath, String name, Map<String, byte[]> entries) throws IOException {
        if (!Files.exists(zipFilePath)) {
            Files.createDirectories(zipFilePath);
        }
        zipFilePath = zipFilePath.resolve(name);
        if (Files.exists(zipFilePath)) {
            throw new IOException("Duplicated file " + name);
        }
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFilePath))) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                String zipPath = entry.getKey();
                ZipEntry zipEntry = new ZipEntry(zipPath);
                zos.putNextEntry(zipEntry);
                byte[] bytes = entry.getValue();
                if (bytes != null) {
                    ByteArrayInputStream baos = new ByteArrayInputStream(bytes);
                    IOUtils.copy(baos, zos);
                }
                zos.closeEntry();
            }
        }
    }
}