package org.openl.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Internal. Pack a file or all files in a source directory to a zipped output stream.
 *
 * @author Yury Molchan
 */
final class ZipCompressor implements FileVisitor<Path> {
    private static final int BUFFER_SIZE = 64 * 1024;
    private static final Pattern BACK_SLASH = Pattern.compile("\\\\");

    private final Path dir;
    private final ZipOutputStream zos;
    private final byte[] buffer = new byte[BUFFER_SIZE];
    private boolean emptyDir;

    private ZipCompressor(Path dir, ZipOutputStream zos) {
        this.dir = dir;
        this.zos = zos;
    }

    /**
     * Pack a file or all files in a source directory to a zipped output stream. This method does not close the output
     * stream.
     *
     * @param source a file or a not empty directory for compressing
     * @param output a stream for zipped files. It is not closed by this method.
     * @throws IOException
     */
    static void archive(File source, OutputStream output) throws IOException {
        Path path = source.toPath();
        ZipOutputStream zos = new ZipOutputStream(output);

        Files.walkFileTree(path, new ZipCompressor(path, zos));
        // Do not close the zip output stream, because it cause closing the
        // output stream. So finish() is used instead.
        zos.finish();
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        emptyDir = false;
        String zipPath;
        if (Files.isDirectory(dir)) {
            // A directory was defined for compression, so make inner files relative to a base directory
            String relativePath = dir.relativize(file).toString();
            zipPath = BACK_SLASH.matcher(relativePath).replaceAll("/");
        } else {
            // Otherwise it is a file, so ignore a 'dir' field.
            zipPath = file.getFileName().toString();
        }
        try (InputStream fis = Files.newInputStream(file)) {
            ZipEntry entry = new ZipEntry(zipPath);
            zos.putNextEntry(entry);
            IOUtils.copy(fis, zos, buffer);
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
            String relativePath = dir.relativize(file).toString() + File.separatorChar;
            String zipPath = BACK_SLASH.matcher(relativePath).replaceAll("/");
            ZipEntry entry = new ZipEntry(zipPath);
            zos.putNextEntry(entry);
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        throw exc;
    }
}
