package org.openl.util;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.jspecify.annotations.NonNull;

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

    public void addFile(byte[] data, String path) throws IOException {
        var zipPath = FileNameFormatter.normalizePath(path);
        var entry = new ZipEntry(zipPath);
        zos.putNextEntry(entry);
        zos.write(data);
    }

    public void addFolder(String path) throws IOException {
        String zipPath = FileNameFormatter.normalizePath(path + File.separatorChar);
        ZipEntry entry = new ZipEntry(zipPath);
        zos.putNextEntry(entry);
    }

    /**
     * Copy all entries from the given source ZIP into this archive, prefixing each entry name with {@code prefix}.
     *
     * @param sourceZip path to the source ZIP file
     * @param prefix    folder prefix (without trailing slash) under which the source entries will be placed
     */
    public void addZipEntries(File sourceZip, String prefix) throws IOException {
        addZipEntries(sourceZip, prefix, name -> true);
    }

    /**
     * Copy entries from the given source ZIP into this archive, prefixing each entry name with {@code prefix}.
     * Entries whose original name does not satisfy {@code entryNameFilter} are skipped.
     *
     * @param sourceZip       path to the source ZIP file
     * @param prefix          folder prefix (without trailing slash) under which the source entries will be placed
     * @param entryNameFilter predicate evaluated against the original entry name; entries returning {@code false} are skipped
     */
    public void addZipEntries(@NonNull File sourceZip, @NonNull String prefix, @NonNull Predicate<String> entryNameFilter) throws IOException {
        var normalizedPrefix = prefix.endsWith("/") ? prefix : prefix + "/";
        try (var source = new ZipFile(sourceZip)) {
            var entries = source.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (entry.isDirectory() || !entryNameFilter.test(entry.getName())) {
                    continue;
                }
                var zipPath = FileNameFormatter.normalizePath(normalizedPrefix + entry.getName());
                if (!zipPath.startsWith(normalizedPrefix)) {
                    throw new SecurityException("Zip Slip vulnerability detected! Invalid entry: " + entry.getName());
                }
                zos.putNextEntry(new ZipEntry(zipPath));
                try (InputStream is = source.getInputStream(entry)) {
                    is.transferTo(zos);
                }
            }
        }
    }

    @Override
    public void close() throws IOException {
        zos.close();
    }

    public void finish() throws IOException {
        zos.finish();
    }

}
