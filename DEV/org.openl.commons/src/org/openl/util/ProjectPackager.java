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

/**
 * Pack an OpenL project using {@linkplain ZipArchiver}.
 *
 * @author Yury Molchan
 */
public final class ProjectPackager implements FileVisitor<Path> {
    private static final List<String> systems = Arrays.asList("rules.xml", "rules-deploy.xml");

    private final Path dir;
    private final ZipArchiver arch;
    private boolean emptyDir;

    private ProjectPackager(Path dir, ZipArchiver arch) {
        this.dir = dir;
        this.arch = arch;
    }

    /**
     * Pack all files in a source directory.
     *
     * @param sourceDir a file or a not empty directory for compressing
     * @param arch a zip archiver.
     */
    public static void addOpenLProject(File sourceDir, ZipArchiver arch) throws IOException {
        Path path = sourceDir.toPath();
        for (String item : systems) {
            // Add system files in the beginning of the archive
            arch.addEntry(path.resolve(item), item);
        }

        Files.walkFileTree(path, new ProjectPackager(path, arch));
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        emptyDir = false;
        // A directory was defined for compression, so make inner files relative to a base directory
        String relativePath = toRelative(file);
        if (!systems.contains(relativePath)) {
            arch.addEntry(file, relativePath);
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path file, BasicFileAttributes attrs) throws IOException {
        emptyDir = true;
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path file, IOException exc) throws IOException {
        if (emptyDir) {
            emptyDir = false;
            String relativePath = toRelative(file);
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
