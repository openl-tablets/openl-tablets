package org.openl.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSignatureHelperTest {

    private static final String RESOURCES = "org/openl/util/signatures/";

    private int readSignature(String resourceName) throws IOException {
        try (var is = getClass().getClassLoader().getResourceAsStream(RESOURCES + resourceName)) {
            assertNotNull(is);
            return new DataInputStream(is).readInt();
        }
    }

    @Test
    void testXlsFile_isNotArchive() throws IOException {
        var sign = readSignature("sample.xls");
        assertFalse(FileSignatureHelper.isArchiveSign(sign));
    }

    @Test
    void testXlsxFile_isArchive() throws IOException {
        var sign = readSignature("sample.xlsx");
        assertTrue(FileSignatureHelper.isArchiveSign(sign));
    }

    @Test
    void testZipFile_isArchive() throws IOException {
        var sign = readSignature("sample.zip");
        assertTrue(FileSignatureHelper.isArchiveSign(sign));
        assertFalse(FileSignatureHelper.isEmptyArchive(sign));
    }

    @Test
    void testEmptyZipFile_isEmptyArchive() throws IOException {
        var sign = readSignature("empty.zip");
        assertTrue(FileSignatureHelper.isArchiveSign(sign));
        assertTrue(FileSignatureHelper.isEmptyArchive(sign));
    }

    @Test
    void testPlainTextFile_isNotArchive() throws IOException {
        var sign = readSignature("plain.txt");
        assertFalse(FileSignatureHelper.isArchiveSign(sign));
    }

    @Test
    void readSignatureOfFile(@TempDir Path dir) throws IOException {
        var zip = dir.resolve("sample.zip");
        try (var is = getClass().getClassLoader().getResourceAsStream(RESOURCES + "sample.zip")) {
            Files.copy(is, zip);
        }

        assertTrue(FileSignatureHelper.isArchiveSign(FileSignatureHelper.readSignature(zip)));
    }

    @Test
    void readSignatureOfEntryOfArchive(@TempDir Path dir) throws IOException {
        var outer = dir.resolve("outer.zip");
        try (var fs = FileSystems.newFileSystem(outer, Map.of("create", "true"));
             var is = getClass().getClassLoader().getResourceAsStream(RESOURCES + "sample.zip")) {
            Files.copy(is, fs.getPath("/inner.zip"));
        }
        try (var fs = FileSystems.newFileSystem(outer)) {
            assertTrue(FileSignatureHelper.isArchiveSign(FileSignatureHelper.readSignature(fs.getPath("/inner.zip"))));
        }
    }

    @Test
    void readSignatureOfShortOrAbsentFile(@TempDir Path dir) throws IOException {
        assertEquals(-1, FileSignatureHelper.readSignature(Files.writeString(dir.resolve("short.txt"), "PK")));
        assertEquals(-1, FileSignatureHelper.readSignature(dir.resolve("absent.zip")));
    }
}
