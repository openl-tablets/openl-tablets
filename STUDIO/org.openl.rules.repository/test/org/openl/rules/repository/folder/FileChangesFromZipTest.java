package org.openl.rules.repository.folder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;

class FileChangesFromZipTest {

    @Test
    void walksTheFilesOfTheArchiveAndSkipsItsFolders() throws IOException {
        try (var stream = new ZipInputStream(new ByteArrayInputStream(archive()))) {
            var names = new ArrayList<String>();
            for (var file : new FileChangesFromZip(stream, "/folder")) {
                names.add(file.getData().getName());
            }

            assertEquals(List.of("/folder/first.txt", "/folder/nested/second.txt"), names);
        }
    }

    @Test
    void answersWithoutBeingAskedWhetherThereIsMore() throws IOException {
        try (var stream = new ZipInputStream(new ByteArrayInputStream(archive()))) {
            var it = new FileChangesFromZip(stream, "/folder").iterator();

            assertEquals("/folder/first.txt", it.next().getData().getName());
            assertEquals("/folder/nested/second.txt", it.next().getData().getName());
        }
    }

    @Test
    void askingTwiceWhetherThereIsMoreDoesNotPassAFileBy() throws IOException {
        try (var stream = new ZipInputStream(new ByteArrayInputStream(archive()))) {
            var it = new FileChangesFromZip(stream, "/folder").iterator();

            assertTrue(it.hasNext());
            assertTrue(it.hasNext());
            assertEquals("/folder/first.txt", it.next().getData().getName());
        }
    }

    @Test
    void anExhaustedIterationHasNothingToAnswerWith() throws IOException {
        try (var stream = new ZipInputStream(new ByteArrayInputStream(archive()))) {
            var it = new FileChangesFromZip(stream, "/folder").iterator();
            while (it.hasNext()) {
                it.next();
            }

            assertThrows(NoSuchElementException.class, it::next);
        }
    }

    /** An archive holding one file, one file in a subfolder, and the subfolder entry itself. */
    private static byte[] archive() throws IOException {
        var bytes = new ByteArrayOutputStream();
        try (var zos = new ZipOutputStream(bytes)) {
            zos.putNextEntry(new ZipEntry("first.txt"));
            zos.write("first".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry("nested/"));
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry("nested/second.txt"));
            zos.write("second".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        return bytes.toByteArray();
    }
}
