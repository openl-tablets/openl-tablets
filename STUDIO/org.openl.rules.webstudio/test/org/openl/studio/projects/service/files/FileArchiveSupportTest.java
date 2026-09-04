package org.openl.studio.projects.service.files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;

import org.openl.studio.common.exception.BadRequestException;

/**
 * Covers what expanding an uploaded archive refuses: an archive that did not arrive in full, an
 * entry whose content no longer matches what the archive records, and a name the archive stores in
 * an encoding of its own.
 */
class FileArchiveSupportTest {

    /** The signature that starts every central directory record of a ZIP archive. */
    private static final byte[] CENTRAL_DIRECTORY_SIGN = {0x50, 0x4B, 0x01, 0x02};

    private static final String CONTENT = "the content of the entry";

    private final FileArchiveSupport archiveSupport = new FileArchiveSupport(null);

    @Test
    void completeArchiveIsExpanded() throws IOException {
        var entries = readArchive("rules", zip(StandardCharsets.UTF_8, "Rating.xlsx", false));
        assertEquals(1, entries.size());
        assertEquals("rules/Rating.xlsx", entries.getFirst().fullPath());
        assertEquals(CONTENT, new String(entries.getFirst().data(), StandardCharsets.UTF_8));
    }

    @Test
    void archiveThatLostItsDirectoryIsRefused() {
        byte[] tailless = withoutCentralDirectory(zip(StandardCharsets.UTF_8, "Rating.xlsx", false));
        var thrown = assertThrows(BadRequestException.class, () -> readArchive("rules", tailless));
        assertEquals("openl.error.400.file.archive.invalid.message", thrown.getErrorCode());
    }

    @Test
    void entryThatNoLongerMatchesItsChecksumIsRefused() {
        byte[] changed = zip(StandardCharsets.UTF_8, "Rating.xlsx", true);
        var thrown = assertThrows(BadRequestException.class, () -> readArchive("rules", changed));
        assertEquals("openl.error.400.file.archive.invalid.message", thrown.getErrorCode());
    }

    /**
     * The name would be read as unknown characters, so the file would be written under a name that
     * names nothing.
     */
    @Test
    void archiveWithNonUtf8EntryNamesIsRefused() {
        byte[] cyrillic = zip(Charset.forName("CP866"), "Правила.xlsx", false);
        var thrown = assertThrows(BadRequestException.class, () -> readArchive("rules", cyrillic));
        assertEquals("openl.error.400.file.archive.invalid.message", thrown.getErrorCode());
    }

    @Test
    void nonAsciiEntryNamesInUtf8AreKept() throws IOException {
        var entries = readArchive("", zip(StandardCharsets.UTF_8, "Правила.xlsx", false));
        assertEquals("Правила.xlsx", entries.getFirst().fullPath());
    }

    private List<FileEntry> readArchive(String path, byte[] archive) throws IOException {
        return archiveSupport.readArchive(path, new ByteArrayInputStream(archive));
    }

    /**
     * An archive of one uncompressed entry. When {@code changed} is set, one byte of the stored
     * content is flipped after the archive is written, so the entry no longer matches the checksum
     * its directory records.
     */
    private static byte[] zip(Charset names, String name, boolean changed) {
        byte[] content = CONTENT.getBytes(StandardCharsets.UTF_8);
        var out = new ByteArrayOutputStream();
        try (var zos = new ZipOutputStream(out, names)) {
            zos.setMethod(ZipOutputStream.STORED);
            var entry = new ZipEntry(name);
            entry.setSize(content.length);
            entry.setCompressedSize(content.length);
            var checksum = new CRC32();
            checksum.update(content);
            entry.setCrc(checksum.getValue());
            zos.putNextEntry(entry);
            zos.write(content);
            zos.closeEntry();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        byte[] archive = out.toByteArray();
        if (changed) {
            var at = new String(archive, StandardCharsets.ISO_8859_1).indexOf(CONTENT);
            assertTrue(at > 0, "the stored content must be found to be changed");
            archive[at] = 'T';
        }
        return archive;
    }

    /**
     * Cuts the archive where its central directory starts, so every local entry is kept and only the
     * tail that records them is lost.
     */
    private static byte[] withoutCentralDirectory(byte[] archive) {
        for (var i = 0; i < archive.length - CENTRAL_DIRECTORY_SIGN.length; i++) {
            if (Arrays.equals(archive, i, i + CENTRAL_DIRECTORY_SIGN.length,
                    CENTRAL_DIRECTORY_SIGN, 0, CENTRAL_DIRECTORY_SIGN.length)) {
                return Arrays.copyOf(archive, i);
            }
        }
        throw new IllegalStateException("The archive has no central directory to cut.");
    }
}
