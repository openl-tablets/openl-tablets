package org.openl.util;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.openl.util.formatters.FileNameFormatter;

/**
 * Pack a file to an output stream.
 *
 * @author Yury Molchan
 */
public final class ZipArchiver implements Closeable {
    private static final int BUFFER_SIZE = 64 * 1024;

    private final ZipOutputStream zos;
    private final byte[] buffer = new byte[BUFFER_SIZE];

    public ZipArchiver(Path file) throws IOException {
        Path dir = file.getParent();
        if (dir != null) {
            Files.createDirectories(dir);
        }
        OutputStream os = Files.newOutputStream(file);
        this.zos = new ZipOutputStream(os);
    }

    public void addEntry(Path file, String path) throws IOException {
        if (Files.isDirectory(file)) {
            addFolder(path);
        } else if (Files.isRegularFile(file)) {
            addFile(file, path);
        }
    }

    public void addFile(File file, String path) throws IOException {
        addFile(file.toPath(), path);
    }

    public void addFile(Path file, String path) throws IOException {
        try (InputStream fis = Files.newInputStream(file)) {
            addFile(fis, path);
        }
    }

    public void addFile(InputStream inputStream, String path) throws IOException {
        String zipPath = FileNameFormatter.normalizePath(path);
        ZipEntry entry = new ZipEntry(zipPath);
        zos.putNextEntry(entry);
        IOUtils.copy(inputStream, zos, buffer);
    }

    public void addFolder(String path) throws IOException {
        String zipPath = FileNameFormatter.normalizePath(path + File.separatorChar);
        ZipEntry entry = new ZipEntry(zipPath);
        zos.putNextEntry(entry);
    }

    @Override
    public void close() throws IOException {
        zos.close();
    }

    public void finish() throws IOException {
        zos.finish();
    }

}
