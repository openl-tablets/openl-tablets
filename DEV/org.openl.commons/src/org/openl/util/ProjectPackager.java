package org.openl.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

/**
 * Pack an OpenL project using {@linkplain ZipArchiver}.
 *
 * @author Yury Molchan
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class ProjectPackager implements FileVisitor<Path> {
    private static final String DESCRIPTOR = "rules.xml";
    private static final List<String> systems = Arrays.asList(DESCRIPTOR, "rules-deploy.xml");

    private final Path dir;
    private final ZipArchiver arch;
    private boolean emptyDir;

    /**
     * Pack provided files in a source directory.
     * <p>
     * The given descriptor is packed as {@code rules.xml} in place of the file of the directory. Without it, the
     * file is packed as it is.
     *
     * @param sourceDir  a file or a not empty directory for compressing
     * @param includes   the array of files to pack
     * @param descriptor the content of {@code rules.xml} to pack, or {@code null} to pack the file of the directory
     * @param arch       a zip archiver.
     */
    public static void addOpenLProject(File sourceDir,
                                       String[] includes,
                                       byte @Nullable [] descriptor,
                                       ZipArchiver arch) throws IOException {
        var path = sourceDir.toPath();
        resolveSystemFiles(path, descriptor, arch);
        for (String file : includes) {
            if (!systems.contains(file)) {
                arch.addEntry(path.resolve(file), file);
            }
        }
    }

    /**
     * Pack all files in a source directory.
     *
     * @param sourceDir a file or a not empty directory for compressing
     * @param arch      a zip archiver.
     */
    public static void addOpenLProject(File sourceDir, ZipArchiver arch) throws IOException {
        var path = sourceDir.toPath();
        resolveSystemFiles(path, null, arch);

        Files.walkFileTree(path, new ProjectPackager(path, arch));
    }

    /**
     * Pack system files in the beginning of the archive
     *
     * @param sourceDir  a file or a not empty directory for compressing
     * @param descriptor the content of {@code rules.xml} to pack, or {@code null} to pack the file of the directory
     * @param arch       a zip archiver.
     */
    private static void resolveSystemFiles(Path sourceDir,
                                           byte @Nullable [] descriptor,
                                           ZipArchiver arch) throws IOException {
        for (String item : systems) {
            // Add system files in the beginning of the archive
            if (descriptor != null && DESCRIPTOR.equals(item)) {
                arch.addFile(descriptor, item);
            } else {
                arch.addEntry(sourceDir.resolve(item), item);
            }
        }
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        emptyDir = false;
        // A directory was defined for compression, so make inner files relative to a base directory
        var relativePath = toRelative(file);
        if (!systems.contains(relativePath)) {
            arch.addEntry(file, relativePath);
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path file, BasicFileAttributes attrs) {
        emptyDir = true;
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path file, IOException exc) throws IOException {
        if (emptyDir) {
            emptyDir = false;
            var relativePath = toRelative(file);
            arch.addEntry(file, relativePath);
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        throw exc;
    }

    private String toRelative(Path file) {
        return dir.relativize(file).toString();
    }
}
