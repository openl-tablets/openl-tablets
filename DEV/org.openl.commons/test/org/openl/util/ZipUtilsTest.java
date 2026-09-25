package org.openl.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.abort;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ZipUtilsTest {

    @TempDir
    File tempFolder;

    @Test
    void testContainsMethodReleasesResources() throws IOException {
        final var file = new File(tempFolder, "test.txt");
        assertTrue(file.createNewFile());

        final var zipFile = new File(tempFolder, "archive.zip");
        ZipUtils.archive(tempFolder, zipFile);

        assertTrue(ZipUtils.contains(zipFile, name -> name.equals("test.txt")));

        assertTrue(zipFile.delete());
        assertFalse(zipFile.exists());
    }

    @Test
    void extractAllWritesEntriesIntoTheOutputFolder() throws IOException {
        var outputFolder = new File(tempFolder, "out");

        ZipUtils.extractAll(zipped("folder/test.txt"), outputFolder);

        assertEquals("content", Files.readString(outputFolder.toPath().resolve("folder/test.txt")));
    }

    @Test
    void extractAllRejectsAnEntryEscapingTheOutputFolder() throws IOException {
        var outputFolder = new File(tempFolder, "out");
        var archive = zipped("../escaped.txt");

        var error = assertThrows(IOException.class, () -> ZipUtils.extractAll(archive, outputFolder));

        assertEquals("Zip entry '../escaped.txt' is outside of the target folder.", error.getMessage());
        assertFalse(new File(tempFolder, "escaped.txt").exists());
    }

    @Test
    void extractAllRejectsAnAbsoluteEntryName() throws IOException {
        var outputFolder = new File(tempFolder, "out");
        var absoluteName = new File(tempFolder, "absolute.txt").getAbsolutePath();
        var archive = zipped(absoluteName);

        var error = assertThrows(IOException.class, () -> ZipUtils.extractAll(archive, outputFolder));

        assertEquals("Zip entry '%s' is outside of the target folder.".formatted(absoluteName), error.getMessage());
        assertFalse(new File(tempFolder, "absolute.txt").exists());
    }

    @ParameterizedTest
    @ValueSource(strings = {"away/escaped.txt", "./away/escaped.txt"})
    void extractAllRejectsAnEntryLedOutOfTheOutputFolderByALink(String entryName) throws IOException {
        var outputFolder = new File(tempFolder, "out");
        var outside = new File(tempFolder, "outside");
        assertTrue(outside.mkdirs());
        assertTrue(outputFolder.mkdirs());
        assumeSymbolicLink(outputFolder.toPath().resolve("away"), outside.toPath());
        var archive = zipped(entryName);

        var error = assertThrows(IOException.class, () -> ZipUtils.extractAll(archive, outputFolder));

        assertEquals("Zip entry '%s' is led outside of the target folder by a link.".formatted(entryName),
                error.getMessage());
        assertFalse(new File(outside, "escaped.txt").exists());
    }

    /** Creates the link, or skips the test where the file system does not let us. */
    private static void assumeSymbolicLink(Path link, Path target) {
        try {
            Files.createSymbolicLink(link, target);
        } catch (IOException | UnsupportedOperationException e) {
            abort("Symbolic links are not available here: " + e.getMessage());
        }
    }

    private static ByteArrayInputStream zipped(String entryName) throws IOException {
        var bytes = new ByteArrayOutputStream();
        try (var zos = new ZipOutputStream(bytes)) {
            zos.putNextEntry(new ZipEntry(entryName));
            zos.write("content".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        return new ByteArrayInputStream(bytes.toByteArray());
    }
}
