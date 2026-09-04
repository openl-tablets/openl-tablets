package org.openl.studio.projects.service.files;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.stereotype.Component;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.validation.FileIntegrityValidator;
import org.openl.util.FileUtils;

/**
 * Reads and writes ZIP archives for the files service.
 *
 * <p>Reads expand an uploaded archive into in-memory entries, bounded by guards against malicious
 * archives (too many entries, oversized entry, oversized total). Writes stream a folder and its
 * readable descendants into a ZIP. Entry paths are validated by the caller before any write.
 *
 * @author Yury Molchan
 */
@Slf4j
@Component
@RequiredArgsConstructor
class FileArchiveSupport {

    /**
     * Maximum number of files a single uploaded archive may contain.
     */
    private static final int MAX_ARCHIVE_ENTRIES = 10_000;

    /**
     * Maximum uncompressed size of a single archive entry (guards against zip bombs).
     */
    private static final long MAX_ARCHIVE_ENTRY_BYTES = 100L * 1024 * 1024;

    /**
     * Maximum total uncompressed size of an uploaded archive (guards against zip bombs).
     */
    private static final long MAX_ARCHIVE_TOTAL_BYTES = 200L * 1024 * 1024;

    private final AclProjectsHelper aclProjectsHelper;

    /**
     * Reads every archive entry into memory under the given base path, applying the zip-bomb guards.
     * Entry paths are resolved relative to {@code path} but not validated here; the caller validates
     * them before writing. A malformed archive is reported as a bad request.
     *
     * <p>The upload is copied to a temporary file and read through the directory the archive keeps
     * at its end, so an archive that lost its tail is refused instead of being expanded in part -
     * reading a stream entry by entry yields what arrived before the cut and never notices the rest
     * is missing. Each entry is matched against the size and the checksum recorded for it as it is
     * read, so the archive is walked once.
     */
    List<FileEntry> readArchive(String path, InputStream archive) throws IOException {
        // The copy is made outside the block below, so that a file system that cannot take it is not
        // reported to the client as an archive of theirs that did not arrive.
        Path verified = copyToTempFile(archive);
        try {
            return readEntries(path, verified);
        } catch (IOException e) {
            log.debug("The uploaded archive did not pass the integrity check.", e);
            throw new BadRequestException("file.archive.invalid.message", new Object[]{e.getMessage()});
        } finally {
            FileUtils.deleteQuietly(verified);
        }
    }

    /**
     * Copies the upload to a temporary file, bounded by the size an archive may have: the caps on
     * the entries are applied while the archive is read, which is too late to keep an upload of any
     * length from filling the file system it is copied to.
     */
    private static Path copyToTempFile(InputStream archive) throws IOException {
        try (archive) {
            return FileUtils.copyToPrivateTempFile(archive, "openl-archive", ".zip", MAX_ARCHIVE_TOTAL_BYTES);
        } catch (FileUtils.ContentTooLargeException e) {
            throw new BadRequestException("file.archive.too-large.message");
        }
    }

    private List<FileEntry> readEntries(String path, Path archive) throws IOException {
        var entries = new ArrayList<FileEntry>();
        try (var content = FileIntegrityValidator.openArchive(archive)) {
            var count = 0;
            var total = 0L;
            var items = content.getEntries();
            while (items.hasMoreElements()) {
                var entry = items.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                if (++count > MAX_ARCHIVE_ENTRIES) {
                    throw new BadRequestException("file.archive.too-many-entries.message");
                }
                String entryName = FilePaths.stripLeadingSlashes(entryName(entry).replace('\\', '/'));
                String fullPath = path.isEmpty() ? entryName : path + "/" + entryName;

                var data = readEntry(content, entry);
                if (data.length > MAX_ARCHIVE_ENTRY_BYTES) {
                    throw new BadRequestException("file.archive.entry.too-large.message", new Object[]{entryName});
                }
                total += data.length;
                if (total > MAX_ARCHIVE_TOTAL_BYTES) {
                    throw new BadRequestException("file.archive.too-large.message");
                }
                FileIntegrityValidator.verifyEntry(entry, data);
                entries.add(new FileEntry(fullPath, data));
            }
        }
        return entries;
    }

    /**
     * The name of an entry, refused when the archive stores it in an encoding of its own.
     *
     * <p>A ZIP entry name is UTF-8 only when the archive says so. Bytes of any other encoding are
     * read as unknown characters, and the file would be written under a name that names nothing -
     * so such an archive is refused instead, as it was when it was expanded from a stream.
     */
    private static String entryName(ZipArchiveEntry entry) throws IOException {
        byte[] raw = entry.getRawName();
        if (raw != null && entry.getNameSource() != ZipArchiveEntry.NameSource.UNICODE_EXTRA_FIELD
                && !isUtf8(raw)) {
            throw new IOException("The archive stores the name of an entry in an encoding other than UTF-8.");
        }
        return entry.getName();
    }

    private static boolean isUtf8(byte[] name) {
        try {
            StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(name));
            return true;
        } catch (CharacterCodingException e) {
            return false;
        }
    }

    /**
     * Reads one entry, one byte past the size the archive allows, so an oversized entry is reported
     * rather than held in memory in full.
     */
    private static byte[] readEntry(ZipFile archive, ZipArchiveEntry entry) throws IOException {
        var content = archive.getInputStream(entry);
        if (content == null) {
            // The entry is packed by a method the reader does not support, an encrypted one among them.
            throw new IOException("The archive entry '%s' cannot be read.".formatted(entry.getName()));
        }
        try (content) {
            return content.readNBytes((int) MAX_ARCHIVE_ENTRY_BYTES + 1);
        }
    }

    /**
     * Writes the readable files of a folder to the stream as a ZIP archive. Entry names are relative
     * to the folder. Files the user cannot read are skipped.
     */
    void writeZip(AProjectFolder folder, OutputStream out) throws IOException {
        try (var zos = new ZipOutputStream(out)) {
            zipFolder(folder, "", zos);
        }
    }

    private void zipFolder(AProjectFolder folder, String prefix, ZipOutputStream zos) throws IOException {
        for (AProjectArtefact artefact : folder.getArtefacts()) {
            if (!aclProjectsHelper.hasPermission(artefact, BasePermission.READ)) {
                continue;
            }
            String entryName = prefix.isEmpty() ? artefact.getName() : prefix + "/" + artefact.getName();
            if (artefact.isFolder()) {
                zipFolder((AProjectFolder) artefact, entryName, zos);
            } else {
                zos.putNextEntry(new ZipEntry(entryName));
                try (var in = ((AProjectResource) artefact).getContent()) {
                    in.transferTo(zos);
                } catch (ProjectException e) {
                    throw new ConflictException("file.read.failed.message");
                }
                zos.closeEntry();
            }
        }
    }
}
