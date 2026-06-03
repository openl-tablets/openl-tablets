package org.openl.studio.projects.service.files;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import lombok.RequiredArgsConstructor;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.stereotype.Component;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.exception.ConflictException;

/**
 * Reads and writes ZIP archives for the files service.
 *
 * <p>Reads expand an uploaded archive into in-memory entries, bounded by guards against malicious
 * archives (too many entries, oversized entry, oversized total). Writes stream a folder and its
 * readable descendants into a ZIP. Entry paths are validated by the caller before any write.
 *
 * @author Yury Molchan
 */
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
     */
    List<FileEntry> readArchive(String path, InputStream archive) {
        List<FileEntry> entries = new ArrayList<>();
        try (var zis = new ZipInputStream(archive)) {
            ZipEntry entry;
            int count = 0;
            long total = 0;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                if (++count > MAX_ARCHIVE_ENTRIES) {
                    throw new BadRequestException("file.archive.too-many-entries.message");
                }
                String entryName = FilePaths.stripLeadingSlashes(entry.getName().replace('\\', '/'));
                String fullPath = path.isEmpty() ? entryName : path + "/" + entryName;

                byte[] data = zis.readNBytes((int) MAX_ARCHIVE_ENTRY_BYTES + 1);
                if (data.length > MAX_ARCHIVE_ENTRY_BYTES) {
                    throw new BadRequestException("file.archive.entry.too-large.message", new Object[]{entryName});
                }
                total += data.length;
                if (total > MAX_ARCHIVE_TOTAL_BYTES) {
                    throw new BadRequestException("file.archive.too-large.message");
                }
                entries.add(new FileEntry(fullPath, data));
            }
        } catch (IOException e) {
            throw new BadRequestException("file.archive.invalid.message");
        }
        return entries;
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
