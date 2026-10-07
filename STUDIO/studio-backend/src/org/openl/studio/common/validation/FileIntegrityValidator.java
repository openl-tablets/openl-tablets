package org.openl.studio.common.validation;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.CRC32;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

import org.openl.studio.common.exception.BadRequestException;
import org.openl.util.FileTypeHelper;
import org.openl.util.FileUtils;

/**
 * Verifies that uploaded Excel and ZIP content arrived complete.
 *
 * <p>A file signature only tells what the first bytes promise. It says nothing about the rest, so an
 * upload that was cut short still looks like a workbook. This check reads the structure the format
 * records about itself: the directory a ZIP keeps at its end, and the checksum every ZIP entry
 * carries; an OLE2 workbook is walked through its own allocation table. Content that did not arrive
 * in full fails all of them.
 *
 * <p>Only the formats OpenL reads as rules are checked: {@code .xlsx} and {@code .xlsm} as OOXML
 * packages, {@code .xls} as an OLE2 workbook, and {@code .zip} as an archive. Any other file passes
 * unchecked - its format is unknown here, so there is nothing to verify it against.
 *
 * <p>A ZIP is verified against its central directory, which random access requires. Content that
 * arrives as a stream is therefore copied to a temporary file first.
 */
@Slf4j
public final class FileIntegrityValidator {

    /**
     * The part every OOXML package declares its content types in, so a plain ZIP renamed to
     * {@code .xlsx} is refused.
     */
    private static final String OOXML_CONTENT_TYPES = "[Content_Types].xml";

    /**
     * The names an OLE2 workbook stream is stored under, matched ignoring case: BIFF8 writes
     * {@code Workbook}, BIFF5 writes {@code Book}.
     */
    private static final List<String> XLS_WORKBOOK_ENTRIES = List.of("Workbook", "Book");

    /**
     * The total uncompressed size above which the entries of an archive are not read. The structure
     * is still verified; only the per-entry checksums are skipped, so a decompression bomb cannot
     * turn the check itself into the attack.
     */
    private static final long MAX_INFLATED_BYTES = 2L * 1024 * 1024 * 1024;

    /**
     * The largest upload the check copies to disk. Content that arrives as a stream has to be
     * written to a temporary file before it can be read at random, so the copy is bounded: an
     * upload longer than this is refused instead of filling the file system it is copied to. The
     * bound matches the one the other upload paths of OpenL Studio apply.
     */
    private static final long MAX_BUFFERED_BYTES = 1000L * 1024 * 1024;

    /**
     * The largest workbook read into memory to be verified in full - one carried by an archive, and
     * an OLE2 document that would otherwise be mapped from a file. A rule module is orders of
     * magnitude smaller; a bigger one keeps the checks that do not need it in memory.
     */
    private static final long MAX_IN_MEMORY_WORKBOOK_BYTES = 100L * 1024 * 1024;

    /**
     * Opens the content in the form a check needs it, so the caller of a check names the source
     * without opening it: a format that is not verified is never read at all.
     */
    @FunctionalInterface
    private interface Source<T extends Closeable> {
        T open() throws IOException;
    }

    private FileIntegrityValidator() {
    }

    /**
     * Whether the content of a file with this name is verified. A file of any other type is written
     * as it arrives.
     */
    public static boolean isVerified(String fileName) {
        return FileTypeHelper.isExcelFile(fileName) || FileTypeHelper.isZipFile(fileName);
    }

    /**
     * Verifies the content stored in a file, against the format its name promises.
     *
     * @throws IOException when the content is damaged, incomplete, or not in that format
     */
    public static void verify(String fileName, Path content) throws IOException {
        // An OLE2 document is read from memory while it fits: POI maps the file it is given by name,
        // and leaves the mapping behind when the document turns out to be damaged - a file still
        // mapped cannot be deleted on Windows.
        if (isOle2Workbook(fileName) && Files.size(content) <= MAX_IN_MEMORY_WORKBOOK_BYTES) {
            verify(fileName, Files.readAllBytes(content));
            return;
        }
        verify(fileName,
                () -> new POIFSFileSystem(content.toFile(), true),
                () -> Files.newByteChannel(content, StandardOpenOption.READ));
    }

    /**
     * Verifies the content held in memory, against the format its name promises.
     *
     * @throws IOException when the content is damaged, incomplete, or not in that format
     */
    public static void verify(String fileName, byte[] content) throws IOException {
        verify(fileName,
                () -> new POIFSFileSystem(new ByteArrayInputStream(content)),
                () -> new SeekableInMemoryByteChannel(content));
    }

    /**
     * Verifies the content of a stream and returns the same content to write.
     *
     * <p>A verified format is copied to a temporary file, because its structure can only be read by
     * random access. The returned stream reads that copy and deletes it when it is closed, so the
     * caller must close the stream it gets. Content of any other type is returned as it came, and
     * the caller closes it as before.
     *
     * @return a stream of the same content, positioned at the beginning
     * @throws IOException when the content is damaged, incomplete, or not in the promised format
     */
    public static InputStream verify(String fileName, InputStream content) throws IOException {
        if (!isVerified(fileName)) {
            return content;
        }
        Path buffered = null;
        try (content) {
            buffered = FileUtils.copyToPrivateTempFile(content, "openl-verify", null, MAX_BUFFERED_BYTES);
            verify(fileName, buffered);
            return Files.newInputStream(buffered, StandardOpenOption.READ, StandardOpenOption.DELETE_ON_CLOSE);
        } catch (IOException | RuntimeException e) {
            FileUtils.deleteQuietly(buffered);
            // An upload longer than the check may copy is refused as such, not read as damaged content.
            if (e instanceof FileUtils.ContentTooLargeException limit) {
                throw new BadRequestException("file.content.too-large.message",
                        new Object[]{fileName, limit.getMaxBytes()});
            }
            throw e;
        }
    }

    /**
     * Verifies content that is read only to be checked, and never written. Any temporary copy the
     * check needed is removed before returning.
     *
     * @throws IOException when the content is damaged, incomplete, or not in the promised format
     */
    public static void verifyContent(String fileName, InputStream content) throws IOException {
        verify(fileName, content).close();
    }

    /**
     * Verifies an archive that is about to be unpacked, together with the workbooks it carries.
     * Unlike {@link #verify(String, Path)} the name of the archive plays no role: the content is
     * verified as a ZIP archive, whatever it is called.
     *
     * <p>A workbook is checked as a workbook of its own as well. An archive records what each entry
     * held when it was packed, so a module that was already damaged before it was packed travels
     * inside an archive that is intact by every measure of its own.
     *
     * @throws IOException when the archive, or a workbook it carries, is damaged, incomplete, or
     * not in the format it is expected to be
     */
    public static void verifyArchive(Path archive) throws IOException {
        try (var content = openArchive(archive)) {
            verifyEntries(content, false);
            verifyWorkbooks(content);
        } catch (RuntimeException e) {
            throw unreadable(e);
        }
    }

    /**
     * Verifies every workbook an archive carries, reading one at a time. A workbook larger than a
     * rule module can be keeps the checksum of the archive alone.
     *
     * <p>Two kinds of entry are left alone. One the archive records as empty arrived as it was
     * packed, and whether an empty module is of any use is answered by compiling it, not by this
     * check. A resource fork carries the name of the file it belongs to and none of its content.
     */
    private static void verifyWorkbooks(ZipFile archive) throws IOException {
        var entries = archive.getEntries();
        while (entries.hasMoreElements()) {
            var entry = entries.nextElement();
            var name = FileUtils.getName(entry.getName());
            if (!entry.isDirectory() && entry.getSize() > 0
                    && FileTypeHelper.isExcelFile(name) && !isResourceFork(entry.getName(), name)) {
                verifyNestedWorkbook(archive, entry, name);
            }
        }
    }

    /**
     * Whether the entry is the resource fork macOS packs beside the file it belongs to: an
     * {@code __MACOSX} folder holds one per file, named after it with a {@code ._} prefix.
     */
    private static boolean isResourceFork(String path, String name) {
        return name.startsWith("._") || path.startsWith("__MACOSX/") || path.contains("/__MACOSX/");
    }

    private static void verifyNestedWorkbook(ZipFile archive, ZipArchiveEntry entry, String name) throws IOException {
        if (entry.getSize() > MAX_IN_MEMORY_WORKBOOK_BYTES) {
            log.warn("The workbook '{}' is {} bytes, so only its checksum is verified.", entry.getName(),
                    entry.getSize());
            return;
        }
        byte[] content;
        try (var data = archive.getInputStream(entry)) {
            content = data.readAllBytes();
        }
        try {
            verify(name, content);
        } catch (IOException e) {
            throw new IOException("The workbook '%s' the archive carries is damaged: %s"
                    .formatted(entry.getName(), e.getMessage()), e);
        }
    }

    /**
     * Opens an archive through its central directory, which lives at the end of the file, so an
     * archive that did not arrive in full is refused instead of being read up to the cut. The
     * caller reads the entries it needs and verifies each of them with
     * {@link #verifyEntry(ZipArchiveEntry, byte[])}.
     *
     * @throws IOException when the archive is damaged, incomplete, or not an archive at all
     */
    public static ZipFile openArchive(Path archive) throws IOException {
        try {
            return ZipFile.builder().setPath(archive).get();
        } catch (RuntimeException e) {
            throw unreadable(e);
        }
    }

    /**
     * Verifies content read from an archive entry against the size and the checksum the archive
     * records for it.
     *
     * @throws IOException when the content does not match what the archive records
     */
    public static void verifyEntry(ZipArchiveEntry entry, byte[] content) throws IOException {
        if (isUnrecorded(entry)) {
            return;
        }
        var checksum = new CRC32();
        checksum.update(content);
        requireEntryMatches(entry, content.length, checksum.getValue());
    }

    /**
     * Reports content that did not pass the check as a bad request, naming the file and the reason,
     * so a client is told what to send again instead of finding out when the module is opened.
     */
    public static BadRequestException damagedContent(String fileName, IOException cause) {
        log.debug("The uploaded content of '{}' did not pass the integrity check.", fileName, cause);
        return new BadRequestException("file.content.damaged.message", new Object[]{fileName, cause.getMessage()});
    }

    /**
     * Verifies the content against the format the file name promises, reading it through the source
     * that format needs. Unchecked failures of the format libraries are reported as damaged content,
     * because they say the same: what arrived cannot be read to its end.
     */
    private static void verify(String fileName,
                               Source<POIFSFileSystem> workbook,
                               Source<SeekableByteChannel> archive) throws IOException {
        try {
            if (isOle2Workbook(fileName)) {
                try (var document = workbook.open()) {
                    verifyWorkbook(document);
                }
            } else if (isVerified(fileName)) {
                try (var channel = archive.open();
                     var content = ZipFile.builder().setSeekableByteChannel(channel).get()) {
                    verifyEntries(content, isOoxmlPackage(fileName));
                }
            }
        } catch (RuntimeException e) {
            throw unreadable(e);
        }
    }

    /**
     * Reports content the format libraries refused to read as damaged. They signal a structure that
     * makes no sense by unchecked exceptions of their own, which say the same as an {@code IOException}
     * here: what arrived cannot be read to its end.
     */
    private static IOException unreadable(RuntimeException cause) {
        var detail = cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
        return new IOException("The content cannot be read to its end: " + detail, cause);
    }

    /**
     * Reads every entry of an already opened archive to its end and matches it against the size and
     * the checksum the central directory records for it.
     */
    private static void verifyEntries(ZipFile archive, boolean ooxmlPackage) throws IOException {
        var files = new ArrayList<ZipArchiveEntry>();
        var declared = 0L;
        var entries = archive.getEntries();
        while (entries.hasMoreElements()) {
            var entry = entries.nextElement();
            if (!entry.isDirectory()) {
                files.add(entry);
                declared += Math.max(entry.getSize(), 0);
            }
        }
        if (ooxmlPackage && archive.getEntry(OOXML_CONTENT_TYPES) == null) {
            throw new IOException("The archive is not an Excel workbook: '%s' is missing."
                    .formatted(OOXML_CONTENT_TYPES));
        }
        if (declared > MAX_INFLATED_BYTES) {
            log.warn("The archive unpacks to {} bytes, so its entries are not verified.", declared);
            return;
        }
        var buffer = new byte[8 * 1024];
        for (ZipArchiveEntry entry : files) {
            readAndVerifyEntry(archive, entry, buffer);
        }
    }

    /**
     * Reads one entry to the end, comparing what arrives with the size and the checksum recorded for
     * it. The read stops once more than the recorded size has arrived, so an entry that declares
     * less than it unpacks to cannot be read without bound.
     */
    private static void readAndVerifyEntry(ZipFile archive, ZipArchiveEntry entry, byte[] buffer) throws IOException {
        if (isUnrecorded(entry)) {
            return;
        }
        var content = archive.getInputStream(entry);
        if (content == null) {
            throw new IOException("The archive entry '%s' cannot be read.".formatted(entry.getName()));
        }
        var checksum = new CRC32();
        var size = 0L;
        try (content) {
            int read;
            while ((read = content.read(buffer)) != -1 && size <= entry.getSize()) {
                checksum.update(buffer, 0, read);
                size += read;
            }
        } catch (IOException e) {
            throw new IOException("The archive entry '%s' cannot be read: %s"
                    .formatted(entry.getName(), e.getMessage()), e);
        }
        requireEntryMatches(entry, size, checksum.getValue());
    }

    /**
     * Whether the archive records neither the size nor the checksum of the entry, leaving nothing to
     * compare what arrives with.
     */
    private static boolean isUnrecorded(ZipArchiveEntry entry) {
        if (entry.getSize() < 0 || entry.getCrc() < 0) {
            log.debug("The archive entry '{}' records no size or checksum, so it is not verified.", entry.getName());
            return true;
        }
        return false;
    }

    private static void requireEntryMatches(ZipArchiveEntry entry, long size, long checksum) throws IOException {
        if (size != entry.getSize() || checksum != entry.getCrc()) {
            throw new IOException("The archive entry '%s' is damaged or incomplete.".formatted(entry.getName()));
        }
    }

    /**
     * Walks the workbook stream of an OLE2 document to its end, so a document whose allocation table
     * points past the content it kept is refused.
     */
    private static void verifyWorkbook(POIFSFileSystem workbook) throws IOException {
        var root = workbook.getRoot();
        var stream = root.getEntryNames().stream()
                .filter(name -> XLS_WORKBOOK_ENTRIES.stream().anyMatch(name::equalsIgnoreCase))
                .findFirst()
                .orElseThrow(() -> new IOException("The document holds no Excel workbook."));
        try (var content = root.createDocumentInputStream(stream)) {
            content.transferTo(OutputStream.nullOutputStream());
        }
    }

    private static boolean isOle2Workbook(String fileName) {
        return FileTypeHelper.isExcelFile(fileName) && fileName.toLowerCase(Locale.ROOT).endsWith(".xls");
    }

    private static boolean isOoxmlPackage(String fileName) {
        return FileTypeHelper.isExcelFile(fileName) && !isOle2Workbook(fileName);
    }
}
