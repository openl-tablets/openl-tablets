package org.openl.studio.repositories.validator;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import jakarta.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.util.SystemReader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.rules.webstudio.util.NameChecker;
import org.openl.rules.webstudio.web.repository.upload.zip.ZipCharsetDetector;
import org.openl.rules.workspace.filter.PathFilter;
import org.openl.studio.common.validation.FileIntegrityValidator;
import org.openl.util.FileSignatureHelper;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.util.StringUtils;
import org.openl.util.ZipUtils;

@Slf4j
@Component
public class ZipArchiveValidator implements Validator {

    private final PathFilter zipFilter;
    private final ZipCharsetDetector zipCharsetDetector;

    @Inject
    public ZipArchiveValidator(@Qualifier("zipFilter") PathFilter zipFilter, ZipCharsetDetector zipCharsetDetector) {
        this.zipFilter = zipFilter;
        this.zipCharsetDetector = zipCharsetDetector;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return Path.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        var archive = (Path) target;
        if (!validateSignature(archive, errors) || !validateIntegrity(archive, errors)) {
            return;
        }
        var charset = zipCharsetDetector.detectCharset(() -> Files.newInputStream(archive));
        if (charset == null) {
            errors.reject("zip-archive.unknown.charset.message");
            return;
        }

        try (FileSystem fs = FileSystems.newFileSystem(ZipUtils.toJarURI(archive),
                Map.of("encoding", charset.name()))) {

            var walkRoot = fs.getPath("/");
            if (ProjectResolver.getInstance().isRulesProject(walkRoot) == null) {
                errors.reject("zip-archive.unknown.project.structure.message");
                return;
            }

            var rejectedPaths = new HashSet<Path>();
            try (var stream = Files.walk(walkRoot)
                    .filter(p -> !walkRoot.equals(p))
                    .filter(p -> zipFilter.accept(p.toString()))) {

                stream.forEach(path -> {
                    if (rejectedPaths.stream().noneMatch(r -> r.startsWith(path) || path.startsWith(r))) {
                        for (var i = 0; i < path.getNameCount(); i++) {
                            try {
                                NameChecker.validatePath(path.getName(i).toString());
                            } catch (IOException e) {
                                errors.reject("zip-archive.unknown.archive.path.message",
                                        new String[]{e.getMessage()},
                                        e.getMessage());
                                rejectedPaths.add(path);
                                return;
                            }
                        }
                        try {
                            var p = path.toString().replace('\\', '/');
                            if (p.charAt(0) == '/') {
                                p = p.substring(1);
                            }
                            if (p.charAt(p.length() - 1) == '/') {
                                p = p.substring(0, p.length() - 1);
                            }
                            SystemReader.getInstance().checkPath(p);
                        } catch (CorruptObjectException e) {
                            String defaultMessage = StringUtils.capitalize(e.getMessage());
                            errors.reject("zip-archive.invalid.path.message",
                                    new String[]{defaultMessage},
                                    defaultMessage);
                            rejectedPaths.add(path);
                        }
                    }
                });
            }
        } catch (IOException e) {
            throw RuntimeExceptionWrapper.wrap(e);
        }
    }

    private boolean validateSignature(Path archive, Errors errors) {
        var isValid = true;
        if (!Files.isRegularFile(archive)) {
            errors.reject("zip-archive.invalid.archive.message");
            isValid = false;
        } else {
            var sign = readSignature(archive);
            if (!FileSignatureHelper.isArchiveSign(sign)) {
                errors.reject("zip-archive.invalid.archive.message");
                isValid = false;
            } else if (FileSignatureHelper.isEmptyArchive(sign)) {
                errors.reject("zip-archive.empty.archive.message");
                isValid = false;
            }
        }
        return isValid;
    }

    /**
     * Verifies that the archive arrived complete: its own directory is read, and every entry is
     * matched against the size and the checksum recorded for it. An upload that was cut short keeps
     * a valid signature, so the signature alone does not tell a whole archive from a part of one.
     */
    private static boolean validateIntegrity(Path archive, Errors errors) {
        try {
            FileIntegrityValidator.verifyArchive(archive);
            return true;
        } catch (IOException e) {
            log.debug("The uploaded archive did not pass the integrity check.", e);
            errors.reject("zip-archive.damaged.archive.message", new String[]{e.getMessage()}, e.getMessage());
            return false;
        }
    }

    private static int readSignature(Path path) {
        try (var raf = new RandomAccessFile(path.toFile(), "r")) {
            return raf.readInt();
        } catch (IOException ignored) {
            return -1;
        }
    }

}
