package org.openl.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

class FileSignatureHelperTest {

    private static final String RESOURCES = "org/openl/util/signatures/";

    private int readSignature(String resourceName) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(RESOURCES + resourceName)) {
            assertNotNull(is);
            return new DataInputStream(is).readInt();
        }
    }

    @Test
    void testXlsFile_isOle2() throws IOException {
        int sign = readSignature("sample.xls");
        assertTrue(FileSignatureHelper.isOle2Sign(sign));
        assertFalse(FileSignatureHelper.isArchiveSign(sign));
    }

    @Test
    void testXlsxFile_isArchive() throws IOException {
        int sign = readSignature("sample.xlsx");
        assertTrue(FileSignatureHelper.isArchiveSign(sign));
        assertFalse(FileSignatureHelper.isOle2Sign(sign));
    }

    @Test
    void testZipFile_isArchive() throws IOException {
        int sign = readSignature("sample.zip");
        assertTrue(FileSignatureHelper.isArchiveSign(sign));
        assertFalse(FileSignatureHelper.isEmptyArchive(sign));
        assertFalse(FileSignatureHelper.isOle2Sign(sign));
    }

    @Test
    void testEmptyZipFile_isEmptyArchive() throws IOException {
        int sign = readSignature("empty.zip");
        assertTrue(FileSignatureHelper.isArchiveSign(sign));
        assertTrue(FileSignatureHelper.isEmptyArchive(sign));
        assertFalse(FileSignatureHelper.isOle2Sign(sign));
    }

    @Test
    void testPlainTextFile_isNeitherArchiveNorOle2() throws IOException {
        int sign = readSignature("plain.txt");
        assertFalse(FileSignatureHelper.isArchiveSign(sign));
        assertFalse(FileSignatureHelper.isOle2Sign(sign));
    }
}
