package org.openl.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * A utility class to work with zip files. File names in a zip are in UTF-8.
 *
 * @author Yury Molchan
 */
public final class ZipUtils {
    private static final int BUFFER_SIZE = 64 * 1024;

    private ZipUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Extract all files from a zip file into a directory.
     *
     * @param zipFile      the input zip file
     * @param outputFolder the output folder for extracted files
     */
    public static void extractAll(File zipFile, File outputFolder) throws IOException {
        try (var zippedStream = new FileInputStream(zipFile)) {
            extractAll(zippedStream, outputFolder);
        }
    }

    /**
     * Extract all files from a zipped stream into a directory.
     *
     * <p>Every entry is extracted inside the output folder. An entry naming a location outside of it aborts the
     * extraction.
     *
     * @param zippedStream the zipped input stream
     * @param outputFolder the output folder for extracted files
     * @throws IOException if an entry points outside of the output folder
     */
    public static void extractAll(InputStream zippedStream, File outputFolder) throws IOException {

        byte[] buffer = new byte[BUFFER_SIZE];
        var target = outputFolder.toPath().toAbsolutePath().normalize();

        try (var zis = new ZipInputStream(zippedStream)) {
            // get the zipped file list entry
            var ze = zis.getNextEntry();
            while (ze != null) {

                if (!ze.isDirectory()) {
                    extractOneFile(zis, ze.getName(), target, buffer);
                }
                ze = zis.getNextEntry();
            }
        }
    }

    /**
     * Resolves the name of a zip entry against the output folder.
     *
     * @throws IOException if the entry names a location outside of the output folder
     */
    private static Path resolveEntry(Path outputFolder, String entryName) throws IOException {
        var resolved = outputFolder.resolve(entryName).normalize();
        if (!resolved.startsWith(outputFolder)) {
            throw new IOException("Zip entry '%s' is outside of the target folder.".formatted(entryName));
        }
        return resolved;
    }

    private static void extractOneFile(ZipInputStream zis,
                                       String entryName,
                                       Path outputFolder,
                                       byte[] buffer) throws IOException {
        var targetFile = resolveEntry(outputFolder, entryName);
        // create all non exists folders
        var folder = Files.createDirectories(targetFile.getParent());
        // A name that stays inside the output folder can still be led out of it by a link on the way, which
        // the name alone does not show. The real path does.
        if (!folder.toRealPath().startsWith(outputFolder.toRealPath()) || Files.isSymbolicLink(targetFile)) {
            throw new IOException("Zip entry '%s' is led outside of the target folder by a link."
                    .formatted(entryName));
        }
        try (var fos = Files.newOutputStream(targetFile)) {
            IOUtils.copy(zis, fos, buffer);
        }
    }

    /**
     * Pack all files in a directory to a zip file.
     */
    public static void archive(File sourceDirectory, File targetFile) throws IOException {
        if (!sourceDirectory.exists()) {
            throw new FileNotFoundException(
                    "File '%s' is not exist.".formatted(sourceDirectory.getAbsolutePath()));
        }
        if (sourceDirectory.isDirectory()) {
            var list = sourceDirectory.list();
            if (list == null || list.length == 0) {
                throw new FileNotFoundException(
                        "Directory '%s' is empty.".formatted(sourceDirectory.getAbsolutePath()));
            }
        }
        try (var arch = new ZipArchiver(targetFile.toPath())) {
            ProjectPackager.addOpenLProject(sourceDirectory, arch);
        }
    }

    public static boolean contains(File zipFile, Predicate<String> names) {
        try (var zip = new ZipFile(zipFile)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                var zipEntry = entries.nextElement();
                if (names.test(zipEntry.getName())) {
                    return true;
                }
            }
        } catch (IOException ignored) {
            // skip
        }
        return false;
    }

    public static URI toJarURI(Path pathToZip) {
        var rootURI = pathToZip.toUri();
        try {
            return new URI("jar:" + rootURI.getScheme(), rootURI.getPath(), null);
        } catch (URISyntaxException e) {
            throw RuntimeExceptionWrapper.wrap(e);
        }
    }

}
