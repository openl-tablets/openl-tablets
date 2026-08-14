package org.openl.studio.common.validation;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Verifies that content which did not arrive in full is refused, whichever shape the damage takes:
 * a stream cut in the middle of an entry, a stream that lost only the directory a ZIP keeps at its
 * end, and content whose bytes no longer match the checksum recorded for them.
 */
class FileIntegrityValidatorTest {

    /** The signature that starts every central directory record of a ZIP archive. */
    private static final byte[] CENTRAL_DIRECTORY_SIGN = {0x50, 0x4B, 0x01, 0x02};

    @TempDir
    private Path folder;

    @Test
    void onlyExcelAndArchivesAreVerified() {
        assertTrue(FileIntegrityValidator.isVerified("Rating.xlsx"));
        assertTrue(FileIntegrityValidator.isVerified("Rating.XLSM"));
        assertTrue(FileIntegrityValidator.isVerified("Rating.xls"));
        assertTrue(FileIntegrityValidator.isVerified("project.zip"));
        assertFalse(FileIntegrityValidator.isVerified("rules.xml"));
        assertFalse(FileIntegrityValidator.isVerified("notes.txt"));
        assertFalse(FileIntegrityValidator.isVerified("~$Rating.xlsx"));
    }

    @Test
    void completeWorkbookIsAccepted() throws IOException {
        byte[] ooxml = xlsx();
        byte[] ole2 = xls();
        byte[] archive = zip(false);
        assertDoesNotThrow(() -> FileIntegrityValidator.verify("Rating.xlsx", ooxml));
        assertDoesNotThrow(() -> FileIntegrityValidator.verify("Rating.xlsm", ooxml));
        assertDoesNotThrow(() -> FileIntegrityValidator.verify("Rating.xls", ole2));
        assertDoesNotThrow(() -> FileIntegrityValidator.verify("project.zip", archive));
    }

    @Test
    void workbookCutInTheMiddleIsRefused() throws IOException {
        byte[] cut = Arrays.copyOf(xlsx(), xlsx().length / 3);
        assertThrows(IOException.class, () -> FileIntegrityValidator.verify("Rating.xlsx", cut));
    }

    /**
     * The shape neither a signature check nor a streaming reader can see: everything but the tail
     * arrived, so the file still starts as a workbook and reading it entry by entry yields every
     * entry it ever had.
     */
    @Test
    void workbookThatLostItsDirectoryIsRefused() throws IOException {
        byte[] tailless = withoutCentralDirectory(xlsx());
        assertEquals(entryCount(xlsx()), entryCount(tailless), "reading entry by entry sees no damage");
        assertThrows(IOException.class, () -> FileIntegrityValidator.verify("Rating.xlsx", tailless));
    }

    @Test
    void archiveThatLostItsDirectoryIsRefused() throws IOException {
        byte[] tailless = withoutCentralDirectory(zip(false));
        assertThrows(IOException.class, () -> FileIntegrityValidator.verify("project.zip", tailless));
    }

    @Test
    void archiveWithChangedContentIsRefused() throws IOException {
        byte[] changed = zip(true);
        assertThrows(IOException.class, () -> FileIntegrityValidator.verify("project.zip", changed));
    }

    @Test
    void archiveOfAnotherFormatIsRefusedAsWorkbook() throws IOException {
        byte[] archive = zip(false);
        assertThrows(IOException.class, () -> FileIntegrityValidator.verify("Rating.xlsx", archive));
    }

    @Test
    void contentOfAnotherFormatIsRefused() {
        byte[] text = "This is plain text, not a workbook.".getBytes(StandardCharsets.UTF_8);
        assertThrows(IOException.class, () -> FileIntegrityValidator.verify("Rating.xlsx", text));
        assertThrows(IOException.class, () -> FileIntegrityValidator.verify("Rating.xls", text));
        assertThrows(IOException.class, () -> FileIntegrityValidator.verify("project.zip", text));
    }

    @Test
    void legacyWorkbookCutInTheMiddleIsRefused() throws IOException {
        byte[] cut = Arrays.copyOf(xls(), xls().length / 3);
        assertThrows(IOException.class, () -> FileIntegrityValidator.verify("Rating.xls", cut));
    }

    @Test
    void contentStoredInAFileIsVerified() throws IOException {
        Path complete = write("complete.xlsx", xlsx());
        Path damaged = write("damaged.xlsx", withoutCentralDirectory(xlsx()));
        assertDoesNotThrow(() -> FileIntegrityValidator.verify("complete.xlsx", complete));
        assertThrows(IOException.class, () -> FileIntegrityValidator.verify("damaged.xlsx", damaged));
        assertThrows(IOException.class, () -> FileIntegrityValidator.verifyArchive(damaged));
    }

    /**
     * An archive records what each entry held when it was packed, so a module that was damaged
     * before it was packed travels inside an archive that is intact by every measure of its own.
     */
    @Test
    void archiveCarryingADamagedWorkbookIsRefused() throws IOException {
        Path carriesDamaged = write("damaged-module.zip",
                archiveOf("rules/Rating.xlsx", withoutCentralDirectory(xlsx())));
        var thrown = assertThrows(IOException.class, () -> FileIntegrityValidator.verifyArchive(carriesDamaged));
        assertTrue(thrown.getMessage().contains("rules/Rating.xlsx"), thrown.getMessage());
    }

    @Test
    void archiveCarryingACompleteWorkbookIsAccepted() throws IOException {
        Path carriesComplete = write("complete-module.zip", archiveOf("rules/Rating.xlsx", xlsx()));
        assertDoesNotThrow(() -> FileIntegrityValidator.verifyArchive(carriesComplete));
    }

    /**
     * What Excel and the desktop leave beside a module carries the name of that module and none of
     * its content, so an archive is not judged by it.
     */
    @Test
    void companionsOfAModuleAreNotReadAsWorkbooks() throws IOException {
        var entries = new LinkedHashMap<String, byte[]>();
        entries.put("rules/Rating.xlsx", xlsx());
        entries.put("rules/~$Rating.xlsx", bytes("the owner file Excel keeps while the module is open"));
        entries.put("__MACOSX/rules/._Rating.xlsx", bytes("the resource fork macOS packs beside a file"));
        entries.put("rules/Placeholder.xls", new byte[0]);
        Path withCompanions = write("with-companions.zip", archiveOf(entries));
        assertDoesNotThrow(() -> FileIntegrityValidator.verifyArchive(withCompanions));
    }

    /**
     * The check a caller that expands an archive applies to the entries it reads, so the archive is
     * walked once instead of being verified and then read again.
     */
    @Test
    void entryReadFromAnArchiveIsMatchedAgainstWhatItRecords() throws IOException {
        Path complete = write("complete.zip", zip(false));
        Path changed = write("changed.zip", zip(true));
        assertDoesNotThrow(() -> verifyFirstEntry(complete));
        assertThrows(IOException.class, () -> verifyFirstEntry(changed));
    }

    private void verifyFirstEntry(Path archive) throws IOException {
        try (var content = FileIntegrityValidator.openArchive(archive)) {
            var entry = content.getEntries().nextElement();
            FileIntegrityValidator.verifyEntry(entry, content.getInputStream(entry).readAllBytes());
        }
    }

    /**
     * The stream a caller gets back carries the content it handed in, whatever the check did to read
     * it. That the temporary copy behind that stream is removed is not asserted here: it is deleted
     * by {@code DELETE_ON_CLOSE}, and a POSIX system unlinks it the moment it is opened, so no test
     * running on one can see it at all - while looking for it in the shared temporary directory
     * would answer for every other process writing there.
     */
    @Test
    void verifiedStreamKeepsTheContent() throws IOException {
        byte[] workbook = xlsx();
        try (var verified = FileIntegrityValidator.verify("Rating.xlsx", new ByteArrayInputStream(workbook))) {
            assertArrayEquals(workbook, verified.readAllBytes());
        }
    }

    @Test
    void streamOfDamagedContentIsRefused() throws IOException {
        var damaged = new ByteArrayInputStream(withoutCentralDirectory(xlsx()));
        assertThrows(IOException.class, () -> FileIntegrityValidator.verify("Rating.xlsx", damaged));
    }

    @Test
    void streamOfAnUnverifiedTypeIsPassedThrough() throws IOException {
        var content = new ByteArrayInputStream("anything".getBytes(StandardCharsets.UTF_8));
        assertSame(content, FileIntegrityValidator.verify("notes.txt", content));
    }

    private Path write(String name, byte[] content) throws IOException {
        return Files.write(folder.resolve(name), content);
    }

    /**
     * The number of entries a reader that walks the archive from its start finds - the reading that
     * notices nothing when only the tail of an archive is missing.
     */
    private static int entryCount(byte[] archive) throws IOException {
        var count = 0;
        try (var entries = new ZipInputStream(new ByteArrayInputStream(archive))) {
            while (entries.getNextEntry() != null) {
                count++;
            }
        }
        return count;
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

    private static byte[] archiveOf(String name, byte[] content) throws IOException {
        return archiveOf(new LinkedHashMap<>(Map.of(name, content)));
    }

    /**
     * An archive of stored entries, so every entry keeps exactly the bytes it is given and the
     * archive itself stays intact whatever those bytes are.
     */
    private static byte[] archiveOf(Map<String, byte[]> entries) throws IOException {
        var out = new ByteArrayOutputStream();
        try (var zos = new ZipOutputStream(out)) {
            zos.setMethod(ZipOutputStream.STORED);
            for (var file : entries.entrySet()) {
                var entry = new ZipEntry(file.getKey());
                entry.setSize(file.getValue().length);
                entry.setCompressedSize(file.getValue().length);
                entry.setCrc(crc(file.getValue()));
                zos.putNextEntry(entry);
                zos.write(file.getValue());
                zos.closeEntry();
            }
        }
        return out.toByteArray();
    }

    private static byte[] bytes(String content) {
        return content.getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] xlsx() throws IOException {
        var out = new ByteArrayOutputStream();
        try (var workbook = new XSSFWorkbook()) {
            workbook.createSheet("Rules").createRow(0).createCell(0).setCellValue("Rating");
            workbook.write(out);
        }
        return out.toByteArray();
    }

    private static byte[] xls() throws IOException {
        var out = new ByteArrayOutputStream();
        try (var workbook = new HSSFWorkbook()) {
            workbook.createSheet("Rules").createRow(0).createCell(0).setCellValue("Rating");
            workbook.write(out);
        }
        return out.toByteArray();
    }

    /**
     * A ZIP archive of one uncompressed entry. When {@code changed} is set, one byte of the stored
     * content is flipped after the archive is written, so the entry no longer matches the checksum
     * its directory records - the damage a length check cannot see.
     */
    private static byte[] zip(boolean changed) throws IOException {
        byte[] content = "the content of the entry".repeat(8).getBytes(StandardCharsets.UTF_8);
        var out = new ByteArrayOutputStream();
        try (var zos = new ZipOutputStream(out)) {
            zos.setMethod(ZipOutputStream.STORED);
            var entry = new ZipEntry("rules/Rating.txt");
            entry.setSize(content.length);
            entry.setCompressedSize(content.length);
            entry.setCrc(crc(content));
            zos.putNextEntry(entry);
            zos.write(content);
            zos.closeEntry();
        }
        byte[] archive = out.toByteArray();
        if (changed) {
            var at = new String(archive, StandardCharsets.ISO_8859_1).indexOf("the content");
            archive[at] = 'T';
        }
        return archive;
    }

    private static long crc(byte[] content) {
        var checksum = new CRC32();
        checksum.update(content);
        return checksum.getValue();
    }
}
