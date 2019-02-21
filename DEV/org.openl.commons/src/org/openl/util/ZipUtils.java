package org.openl.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
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
     * 
     * @param sourceDirectory
     * @param targetFile
     * @throws IOException
     */
    public static void archive(File sourceDirectory, File targetFile) throws IOException {
        if (!sourceDirectory.exists()) {
            throw new FileNotFoundException("File '" + sourceDirectory.getAbsolutePath() + "' is not exist!");
        }
        if (sourceDirectory.isDirectory()) {
            String[] list = sourceDirectory.list();
            if (list == null || list.length == 0) {
                throw new FileNotFoundException("Directory '" + sourceDirectory.getAbsolutePath() + "' is empty!");
            }
        }
        try (OutputStream fos = new FileOutputStream(targetFile)) {
            ZipCompressor.archive(sourceDirectory, fos);
        }
    }
}
