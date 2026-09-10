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
        var scratch = Files.createDirectories(Path.of(openlHome, "tmp", "compare"));
        var file = Files.createTempFile(scratch, "openl-cmp", "." + FileUtils.getExtension(name));
        try (var content = upload.getInputStream()) {
            Files.copy(content, file, StandardCopyOption.REPLACE_EXISTING);
            verifyArrivedInFull(name, file);
            return file;
        } catch (Exception e) {
            Files.deleteIfExists(file);
            throw e;
        }
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
