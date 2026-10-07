package org.openl.studio.compare.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import org.openl.studio.common.exception.BadRequestException;

class ComparisonFileStoreTest {

    @TempDir
    Path home;

    private ComparisonFileStore store;

    @BeforeEach
    void setUp() {
        store = new ComparisonFileStore(home.toString());
    }

    @Test
    void keepsTheUploadedFilesUnderTheStudioHome() throws IOException {
        var content = workbook();

        var files = store.store(upload("first.xlsx", content), upload("second.xlsx", content));

        assertEquals(2, files.size());
        for (Path file : files) {
            assertTrue(Files.exists(file));
            assertEquals(home.resolve("tmp").resolve("compare"), file.getParent());
            assertTrue(file.getFileName().toString().endsWith(".xlsx"),
                    "the extension the format is read by is kept: " + file.getFileName());
            assertArrayEquals(content, Files.readAllBytes(file));
        }
    }

    @Test
    void refusesAFileThatIsNotAWorkbook() throws IOException {
        var workbook = upload("first.xlsx", workbook());
        var notes = upload("notes.txt", "text".getBytes());

        assertThrows(BadRequestException.class, () -> store.store(workbook, notes));

        assertTrue(storedFiles().isEmpty(), "nothing is left behind when an upload is refused");
    }

    @Test
    void refusesAWorkbookThatDidNotArriveInFull() throws IOException {
        var truncated = upload("cut.xlsx", Arrays.copyOf(workbook(), 64));

        assertThrows(BadRequestException.class, () -> store.store(truncated));

        assertTrue(storedFiles().isEmpty(), "nothing is left behind when an upload is refused");
    }

    @Test
    void doesNotBlameTheUploadWhenItCannotBeWritten() throws IOException {
        var failing = new MockMultipartFile("file", "first.xlsx", null, workbook()) {
            @Override
            public InputStream getInputStream() {
                return new InputStream() {
                    @Override
                    public int read() throws IOException {
                        throw new IOException("The disk is full");
                    }
                };
            }
        };

        // A file the server could not write is the server failing, not damaged content.
        var failure = assertThrows(IOException.class, () -> store.store(failing));

        assertEquals("The disk is full", failure.getMessage());
        assertTrue(storedFiles().isEmpty(), "nothing is left behind when a file cannot be written");
    }

    @Test
    void copiesFilesThatAreAlreadyOnDisk() throws IOException {
        var content = workbook();
        var source = Files.write(home.resolve("1700000000000"), content);

        var copies = store.copy(List.of(source));

        var copy = copies.getFirst();
        assertEquals(home.resolve("tmp").resolve("compare"), copy.getParent());
        assertNotEquals(source, copy, "the comparison reads a copy, not the file it was given");
        assertArrayEquals(content, Files.readAllBytes(copy));
        assertFalse(copy.getFileName().toString().contains("."),
                "a version stored without an extension is copied without one: " + copy.getFileName());
        assertTrue(Files.exists(source), "the file it was given stays where it is");
    }

    @Test
    void leavesNothingBehindWhenAFileCannotBeCopied() throws IOException {
        var source = Files.write(home.resolve("1700000000000"), workbook());

        assertThrows(IOException.class, () -> store.copy(List.of(source, home.resolve("gone"))));

        assertTrue(storedFiles().isEmpty(), "nothing is left behind when a file cannot be copied");
    }

    @Test
    void clearsWhatAnEarlierRunLeftBehind() throws IOException {
        var abandoned = store.store(upload("first.xlsx", workbook())).getFirst();

        store.clearScratch();

        assertFalse(Files.exists(abandoned), "a file no running comparison reads is not kept");
        assertTrue(storedFiles().isEmpty());
    }

    @Test
    void saysNothingWhenThereIsNothingToClear() {
        assertDoesNotThrow(store::clearScratch, "a scratch directory that was never written is not an error");
    }

    @Test
    void deletesTheFilesItIsAskedTo() throws IOException {
        var files = store.store(upload("first.xlsx", workbook()));

        store.delete(files);

        assertFalse(Files.exists(files.getFirst()));
    }

    @Test
    void saysNothingOfAFileThatIsAlreadyGone() throws IOException {
        var files = store.store(upload("first.xlsx", workbook()));
        store.delete(files);

        store.delete(files);

        assertFalse(Files.exists(files.getFirst()));
    }

    private List<Path> storedFiles() throws IOException {
        var scratch = home.resolve("tmp").resolve("compare");
        if (!Files.isDirectory(scratch)) {
            return List.of();
        }
        try (var files = Files.list(scratch)) {
            return files.toList();
        }
    }

    private static MockMultipartFile upload(String name, byte[] content) {
        return new MockMultipartFile("file", name, null, content);
    }

    /** The smallest workbook POI writes, used as content that arrived in full. */
    private static byte[] workbook() throws IOException {
        var out = new ByteArrayOutputStream();
        try (var workbook = new XSSFWorkbook()) {
            workbook.createSheet("Rules");
            workbook.write(out);
        }
        return out.toByteArray();
    }
}
