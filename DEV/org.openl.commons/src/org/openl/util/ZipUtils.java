package org.openl.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
        if (!sourceDirectory.exists()) {
            throw new FileNotFoundException("File '" + sourceDirectory.getAbsolutePath() + "' is not exist!");
        }
        if (sourceDirectory.isDirectory()) {
            String[] list = sourceDirectory.list();
            if (list == null || list.length == 0) {
                throw new FileNotFoundException("Directory '" + sourceDirectory.getAbsolutePath() + "' is empty!");
            }
        }
        OutputStream fos = new FileOutputStream(targetFile);
        try {
            archive(sourceDirectory, fos);
        } finally {
            fos.close();
        }
    }

    /**
     * Pack a file or all files in a source directory to a zipped output stream.
     * This method does not close the output stream.
     *
     * @param source a file or a not empty directory for compressing
     * @param output a stream for zipped files. It is not closed by this method.
     * @throws IOException
     */
    private static void archive(File source, OutputStream output) throws IOException {
        ZipOutputStream zos = new ZipOutputStream(output);
        byte data[] = new byte[BUFFER];
        if (source.isDirectory()) {
            // This is a directory
            String sourceDirAbsolutePath = source.getAbsolutePath() + File.separator;
            int prefixLength = sourceDirAbsolutePath.length();
            Queue<File> directoryList = new LinkedList<File>();
            directoryList.add(source);
            while (!directoryList.isEmpty()) {
                File directory = directoryList.poll();

                File[] files = directory.listFiles();

                for (File file : files) {
                    // Remove the absolute part of path
                    String entryName = file.getAbsolutePath().substring(prefixLength);
                    entryName = entryName.replaceAll("\\\\", "/");
                    if (file.isDirectory()) {
                        if (file.listFiles().length == 0) {
                            ZipEntry entry = new ZipEntry(entryName + "/");
                            zos.putNextEntry(entry);
                        } else {
                            directoryList.add(file);
                        }
                    } else {
                        archiveFile(file, entryName, zos, data);
                    }
                }
            }
        } else {
            // This is a single File
            String name = source.getName();
            archiveFile(source, name, zos, data);
        }
        // Do not close the zip output stream, because it cause closing the
        // output stream. So finish() is used instead.
        zos.finish();
    }

    private static void archiveFile(File file, String name, ZipOutputStream zos, byte[] data) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        try {
            ZipEntry entry = new ZipEntry(name);
            zos.putNextEntry(entry);

            IOUtils.copy(fis, zos, data);
        } finally {
            fis.close();
        }
    }
}
