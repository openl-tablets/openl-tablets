package org.openl.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
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

import org.apache.commons.lang3.SystemUtils;

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
     * Pack all files in a directory to output stream.
     * 
     * @param sourceDirectory
     * @param outputStream
     * @throws IOException
     */
    public static void archive(File sourceDirectory, File targetFile) throws IOException {
        boolean isEntry = false;
        Queue<File> directoryList = new LinkedList<File>();
        if (sourceDirectory.exists()) {
            FileOutputStream fos = new FileOutputStream(targetFile);
            ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos));
            byte data[] = new byte[BUFFER];
            final String sourceDirAbsolutePath = sourceDirectory.getAbsolutePath() + SystemUtils.FILE_SEPARATOR;
            if (sourceDirectory.isDirectory()) {
                // This is directory
                do {
                    File directory = null;
                    if (!directoryList.isEmpty()) {
                        directory = directoryList.poll();
                    }else{
                        directory = sourceDirectory;
                    }
                    
                    File[] files = directory.listFiles();

                    for (File file : files) {
                        String entryName = file.getAbsolutePath().substring(sourceDirAbsolutePath.length());
                        entryName = entryName.replaceAll("\\\\", "/");
                        if (file.isDirectory()) {
                            if (file.listFiles().length == 0){
                                ZipEntry entry = new ZipEntry(entryName + "/");
                                zos.putNextEntry(entry);
                                isEntry = true;
                            }else{
                                directoryList.add(file);
                            }
                        } else {
                            ZipEntry entry = new ZipEntry(entryName);
                            zos.putNextEntry(entry);
                            isEntry = true;
                            FileInputStream fileInputStream = new FileInputStream(file);
                            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream, BUFFER);
                            int size = -1;
                            while ((size = bufferedInputStream.read(data, 0, BUFFER)) != -1) {
                                zos.write(data, 0, size);
                            }
                            bufferedInputStream.close();
                        }
                    }
                } while (!directoryList.isEmpty());
            } else {
                // This is File
                FileInputStream fis = new FileInputStream(sourceDirectory);
                BufferedInputStream bufferedInputStream = new BufferedInputStream(fis, BUFFER);
                ZipEntry entry = new ZipEntry(sourceDirectory.getName());
                zos.putNextEntry(entry);
                isEntry = true;
                int size = -1;
                while ((size = bufferedInputStream.read(data, 0, BUFFER)) != -1) {
                    zos.write(data, 0, size);
                }
                bufferedInputStream.close();
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
