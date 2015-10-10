package org.openl.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * A utility class to work with zip files.
 *
 * @author Yury Molchan
 */
public class ZipUtils {

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

        byte[] buffer = new byte[8192];

        ZipInputStream zis = new ZipInputStream(zippedStream);
        try {
            // get the zipped file list entry
            ZipEntry ze = zis.getNextEntry();

            while (ze != null) {

                if (!ze.isDirectory()) {
                    String fileName = ze.getName();
                    File unzipped = new File(outputFolder, fileName);
                    // create all non exists folders
                    new File(unzipped.getParent()).mkdirs();
                    FileOutputStream fos = new FileOutputStream(unzipped);

                    IOUtils.copy(zis, fos, buffer);

                    fos.close();
                }

                ze = zis.getNextEntry();
            }
        } finally {
            IOUtils.closeQuietly(zis);
        }
    }
}
