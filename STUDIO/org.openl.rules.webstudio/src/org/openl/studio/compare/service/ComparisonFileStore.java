package org.openl.studio.compare.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import jakarta.annotation.PostConstruct;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.validation.FileIntegrityValidator;
import org.openl.util.FileTypeHelper;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;

/**
 * Holds the files a comparison reads.
 *
 * <p>A comparison reads its files while its result is on screen, so an uploaded file is written to
 * disk rather than kept in memory, and is deleted only once the comparison that reads it is done
 * with.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ComparisonFileStore {

    // The compared files live under the Studio home rather than in the world-writable system temp
    // directory, so no other local user can read what was uploaded for comparison.
    @Value("${openl.home}")
    private final String openlHome;

    /**
     * Puts down what an earlier run left behind.
     *
     * <p>A comparison deletes its files when it is released, and a Studio that was killed released
     * nothing. No comparison exists yet when the Studio starts, so everything in the scratch
     * directory belongs to a run that is over.
     */
    @PostConstruct
    public void clearScratch() {
        var scratch = Path.of(openlHome, "tmp", "compare");
        if (!Files.isDirectory(scratch)) {
            return;
        }
        try (var left = Files.list(scratch)) {
            delete(left.toList());
        } catch (IOException e) {
            log.warn("Failed to clear the directory of the compared files '{}'", scratch, e);
        }
    }

    /**
     * Takes uploaded files to compare. Nothing is left behind when one of them is refused.
     *
     * @param uploads the uploaded files
     * @return where each of them was written, in the order they were given
     * @throws IOException when a file cannot be written
     */
    public List<Path> store(MultipartFile... uploads) throws IOException {
        var stored = new ArrayList<Path>(uploads.length);
        try {
            for (MultipartFile upload : uploads) {
                stored.add(storeOne(upload));
            }
        } catch (Exception e) {
            delete(stored);
            throw e;
        }
        return stored;
    }

    /**
     * Takes files that are already on disk, copying each of them. Nothing is left behind when one of
     * them cannot be copied.
     *
     * @param files the files to copy
     * @return where each of them was copied to, in the order they were given
     * @throws IOException when a file cannot be copied
     */
    public List<Path> copy(List<Path> files) throws IOException {
        var stored = new ArrayList<Path>(files.size());
        try {
            for (Path file : files) {
                stored.add(copyOne(file));
            }
        } catch (Exception e) {
            delete(stored);
            throw e;
        }
        return stored;
    }

    /**
     * Takes files that were read from somewhere else - a revision of a project, its working copy.
     * Nothing is left behind when one of them cannot be written, and what is not written is not read.
     *
     * @param files the content to write, in the order it is given
     * @return where each of them was written to
     * @throws IOException when a file cannot be written
     */
    public List<Path> store(List<ComparisonContent> files) throws IOException {
        var stored = new ArrayList<Path>(files.size());
        for (int index = 0; index < files.size(); index++) {
            try {
                stored.add(write(files.get(index)));
            } catch (Exception e) {
                delete(stored);
                // What was not written is not read either, the one that failed included: a file could
                // not be made for it before its content was taken over.
                files.subList(index, files.size()).forEach(rest -> IOUtils.closeQuietly(rest.content()));
                throw e;
            }
        }
        return stored;
    }

    /**
     * Deletes files a comparison no longer reads.
     *
     * @param files the files to delete
     */
    public void delete(Collection<Path> files) {
        for (Path file : files) {
            try {
                Files.deleteIfExists(file);
            } catch (IOException e) {
                log.warn("Failed to delete the compared file '{}'", file, e);
            }
        }
    }

    /**
     * Writes one uploaded file, keeping the extension its format is read by.
     */
    private Path storeOne(MultipartFile upload) throws IOException {
        var originalName = upload.getOriginalFilename();
        var name = FileUtils.getName(originalName == null ? "" : originalName);
        if (!FileTypeHelper.isExcelFile(name)) {
            throw new BadRequestException("compare.file.not-excel.message");
        }
        var file = scratchFile(name);
        try (var content = upload.getInputStream()) {
            Files.copy(content, file, StandardCopyOption.REPLACE_EXISTING);
            verifyArrivedInFull(name, file);
            return file;
        } catch (Exception e) {
            Files.deleteIfExists(file);
            throw e;
        }
    }

    /** Writes content that was read elsewhere, keeping the extension its format is read by. */
    private Path write(ComparisonContent source) throws IOException {
        var file = scratchFile(source.name());
        try (var content = source.content()) {
            Files.copy(content, file, StandardCopyOption.REPLACE_EXISTING);
            return file;
        } catch (Exception e) {
            Files.deleteIfExists(file);
            throw e;
        }
    }

    /** Writes a copy of a file, keeping the extension its format is read by. */
    private Path copyOne(Path source) throws IOException {
        var file = scratchFile(source.getFileName().toString());
        try {
            Files.copy(source, file, StandardCopyOption.REPLACE_EXISTING);
            return file;
        } catch (Exception e) {
            Files.deleteIfExists(file);
            throw e;
        }
    }

    /**
     * An empty file of its own in the scratch directory, named after nothing the user chose.
     *
     * <p>The extension of the given name is kept, and a name without one gives a file without one:
     * a local history version is stored under a name that has no extension at all.
     */
    private Path scratchFile(String name) throws IOException {
        var scratch = Files.createDirectories(Path.of(openlHome, "tmp", "compare"));
        var extension = FileUtils.getExtension(name);
        return Files.createTempFile(scratch, "openl-cmp", StringUtils.isEmpty(extension) ? "" : "." + extension);
    }

    /**
     * Checks that the content arrived complete: a workbook cut short parses as an empty one, which
     * would read as a file that lost all of its tables.
     *
     * <p>Only what the check itself says is told to the user. A file that could not be written is
     * the server failing, not the user sending damaged content.
     *
     * @throws BadRequestException when the content is damaged or incomplete
     */
    private static void verifyArrivedInFull(String name, Path file) {
        try {
            FileIntegrityValidator.verify(name, file);
        } catch (IOException e) {
            throw FileIntegrityValidator.damagedContent(name, e);
        }
    }
}
