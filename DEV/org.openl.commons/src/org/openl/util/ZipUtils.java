package org.openl.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.Queue;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * A utility class to work with zip files.
 *
 * @author Yury Molchan
 */
public class ZipUtils {

    private static final int BUFFER = 2048;

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

        byte[] buffer = new byte[BUFFER];

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

    /**
     * Pack all files in a directory to a zip file.
     * 
     * @param sourceDirectory
     * @param targetFile
     * @throws IOException
     */
    public static void archive(File sourceDirectory, File targetFile) throws IOException {
        boolean isEntry = false;
        Queue<File> directoryList = new LinkedList<File>();
        if (sourceDirectory.exists()) {
            FileOutputStream fos = new FileOutputStream(targetFile);
            ZipOutputStream zos = new ZipOutputStream(fos);
            byte data[] = new byte[BUFFER];
            final String sourceDirAbsolutePath = sourceDirectory.getAbsolutePath() + File.separator;
            if (sourceDirectory.isDirectory()) {
                // This is directory
                do {
                    File directory = null;
                    if (!directoryList.isEmpty()) {
                        directory = directoryList.poll();
                    } else {
                        directory = sourceDirectory;
                    }

                    File[] files = directory.listFiles();

                    for (File file : files) {
                        String entryName = file.getAbsolutePath().substring(sourceDirAbsolutePath.length());
                        entryName = entryName.replaceAll("\\\\", "/");
                        if (file.isDirectory()) {
                            if (file.listFiles().length == 0) {
                                ZipEntry entry = new ZipEntry(entryName + "/");
                                zos.putNextEntry(entry);
                                isEntry = true;
                            } else {
                                directoryList.add(file);
                            }
                        } else {
                            ZipEntry entry = new ZipEntry(entryName);
                            zos.putNextEntry(entry);
                            isEntry = true;
                            FileInputStream fis = new FileInputStream(file);

                            IOUtils.copy(fis, zos, data);
                            fis.close();
                        }
                    }
                } while (!directoryList.isEmpty());
            } else {
                // This is File
                FileInputStream fis = new FileInputStream(sourceDirectory);
                ZipEntry entry = new ZipEntry(sourceDirectory.getName());
                zos.putNextEntry(entry);
                isEntry = true;

                IOUtils.copy(fis, zos, data);
                fis.close();
            }
            if (isEntry) {
                zos.close();
            } else {
                zos = null;
            }
        } else {
            throw new FileNotFoundException("File '" + sourceDirectory.getAbsolutePath() + "'not found!");
        }
    }
}
