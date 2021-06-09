package org.openl.util;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
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
     * @param zipFile the input zip file
     * @param outputFolder the output folder for extracted files
     */
    public static void extractAll(File zipFile, File outputFolder) throws IOException {
        final FileInputStream zippedStream = new FileInputStream(zipFile);
        extractAll(zippedStream, outputFolder);
    }

    /**
     * Extract all files from a zipped stream into a directory.
     *
     * @param zippedStream the zipped input stream
     * @param outputFolder the output folder for extracted files
     */
    public static void extractAll(InputStream zippedStream, File outputFolder) throws IOException {

        byte[] buffer = new byte[BUFFER_SIZE];

        try (ZipInputStream zis = new ZipInputStream(zippedStream)) {
            // get the zipped file list entry
            ZipEntry ze = zis.getNextEntry();
            while (ze != null) {

                if (!ze.isDirectory()) {
                    String fileName = ze.getName();
                    File unzipped = new File(outputFolder, fileName);
                    extractOneFile(zis, unzipped, buffer);
                }
                ze = zis.getNextEntry();
            }
        }
    }

    private static void extractOneFile(ZipInputStream zis, File targetFile, byte[] buffer) throws IOException {
        // create all non exists folders
        new File(targetFile.getParent()).mkdirs();
        try (FileOutputStream fos = new FileOutputStream(targetFile)) {
            IOUtils.copy(zis, fos, buffer);
        }
    }

    /**
     * Pack all files in a directory to a zip file.
     */
    public static void archive(File sourceDirectory, File targetFile) throws IOException {
        if (!sourceDirectory.exists()) {
            throw new FileNotFoundException(
                String.format("File '%s' is not exist.", sourceDirectory.getAbsolutePath()));
        }
        if (sourceDirectory.isDirectory()) {
            String[] list = sourceDirectory.list();
            if (list == null || list.length == 0) {
                throw new FileNotFoundException(
                    String.format("Directory '%s' is empty.", sourceDirectory.getAbsolutePath()));
            }
        }
        try (ZipArchiver arch = new ZipArchiver(targetFile.toPath())) {
            ProjectPackager.addOpenLProject(sourceDirectory, arch);
        }
    }

    public static boolean contains(File zipFile, Predicate<String> names) {
        try (ZipFile zip = new ZipFile(zipFile)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry zipEntry = entries.nextElement();
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
        URI rootURI = pathToZip.toUri();
        try {
            return new URI("jar:" + rootURI.getScheme(), rootURI.getPath(), null);
        } catch (URISyntaxException e) {
            throw RuntimeExceptionWrapper.wrap(e);
        }
    }

    public static Path toPath(URI uri) {
        if ("jar".equals(uri.getScheme())) {
            String path = uri.getRawSchemeSpecificPart();
            int sep = path.indexOf("!/");
            if (sep > -1) {
                path = path.substring(0, sep);
            }
            try {
                URI uriToZip = new URI(path);
                if (uriToZip.getSchemeSpecificPart().contains("%")) {
                    //FIXME workaround to fix double URI encoding for URIs from ZipPath
                    try {
                        uriToZip = new URI(uriToZip.getScheme() + ":" + uriToZip.getSchemeSpecificPart());
                    } catch (URISyntaxException ignored) {
                        //it's ok
                    }
                }
                return Paths.get(uriToZip);
            } catch (URISyntaxException e) {
                throw RuntimeExceptionWrapper.wrap(e);
            }
        } else if ("file".equals(uri.getScheme())) {
            return Paths.get(uri);
        }
        throw new IllegalArgumentException("Invalid URI scheme.");
    }
}
