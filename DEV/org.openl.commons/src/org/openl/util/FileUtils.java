package org.openl.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * A set of methods to work with a file system.
 *
 * @author Yury Molchan
 */
public class FileUtils {
    /**
     * Returns the path to the system temporary directory.
     *
     * @return the path to the system temporary directory.
     */
    public static String getTempDirectoryPath() {
        return System.getProperty("java.io.tmpdir");
    }

    /**
     * Deletes a file. If file is a directory, delete it and all sub-directories.
     * <p/>
     * The difference between File.delete() and this method are:
     * <ul>
     * <li>A directory to be deleted does not have to be empty.</li>
     * <li>You get exceptions when a file or directory cannot be deleted.</li>
     * </ul>
     *
     * @param file file or directory to delete, must not be {@code null}
     * @throws NullPointerException  if the directory is {@code null}
     * @throws FileNotFoundException if the file was not found
     * @throws IOException           in case deletion is unsuccessful
     */
    public static void delete(File file) throws IOException {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files == null) {  // null if security restricted
                throw new IOException("Failed to list contents of direcory: " + file);
            }

            IOException exception = null;
            for (File fl : files) {
                try {
                    delete(fl);
                } catch (IOException ioe) {
                    exception = ioe;
                }
            }

            if (null != exception) {
                throw exception;
            }

            if (!file.delete()) {
                throw new IOException("Unable to delete directory: " + file);
            }
        } else {
            boolean filePresent = file.exists();
            if (!file.delete()) {
                if (!filePresent) {
                    throw new FileNotFoundException("File does not exist: " + file);
                }
                throw new IOException("Unable to delete file: " + file);
            }
        }
    }

    /**
     * Deletes a file, never throwing an exception. If file is a directory, delete it and all sub-directories.
     * <p/>
     * The difference between File.delete() and this method are:
     * <ul>
     * <li>A directory to be deleted does not have to be empty.</li>
     * <li>No exceptions are thrown when a file or directory cannot be deleted.</li>
     * </ul>
     *
     * @param file file or directory to delete, can be {@code null}
     */
    public static void deleteQuietly(File file) {
        try {
            delete(file);
        } catch (Exception ignored) {
            // ignore
        }
    }
}
