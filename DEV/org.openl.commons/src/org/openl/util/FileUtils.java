package org.openl.util;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collection;

/**
 * A set of methods to work with a file system.
 *
 * @author Yury Molchan
 */
public class FileUtils {

    private static final int DEFAULT_BUFFER_SIZE = 8 * 1024 * 1024;

    /**
     * Returns the path to the system temporary directory.
     *
     * @return the path to the system temporary directory.
     */
    public static String getTempDirectoryPath() {
        return System.getProperty("java.io.tmpdir");
    }

    /**
     * Copies a file to a new location preserving the file date.
     * <p>
     * This method copies the contents of the specified source file to the specified destination file. The directory
     * holding the destination file is created if it does not exist. If the destination file exists, then this method
     * will overwrite it.
     * <p>
     * <strong>Note:</strong> This method tries to preserve the file's last modified date/times using
     * {@link File#setLastModified(long)}, however it is not guaranteed that the operation will succeed. If the
     * modification operation fails, no indication is provided.
     *
     * @param src an existing file to copy, must not be {@code null}
     * @param dest the new file, must not be {@code null}
     *
     * @throws NullPointerException if source or destination is {@code null}
     * @throws IOException if source or destination is invalid
     * @throws IOException if an IO error occurs during copying
     */
    public static void copy(File src, File dest) throws IOException {
        if (!src.exists()) {
            throw new FileNotFoundException("Source '" + src + "' does not exist");
        }
        final String srcPath = src.getCanonicalPath();
        final String destPath = dest.getCanonicalPath();
        if (srcPath.equals(destPath)) {
            throw new IOException("Source '" + src + "' and destination '" + dest + "' are the same");
        }

        if (src.isDirectory()) {
            Collection<String> looped = getLoopedDirectories(src, dest);
            doCopyDirectory(src, dest, looped);
        } else {
            if (destPath.startsWith(srcPath)) {
                throw new IOException("Destination '" + dest + "' has the same path of the source '" + src + "'");
            }
            File destFile = dest;
            if (dest.isDirectory()) {
                destFile = new File(dest, src.getName());
            } else {
                File parentFile = dest.getParentFile();
                if (parentFile != null && !parentFile.mkdirs() && !parentFile.isDirectory()) {
                    throw new IOException("Destination '" + parentFile + "' directory cannot be created");
                }
            }
            doCopyFile(src, destFile);
        }
    }

    /**
     * Collects nested directories which should be excluded for copying to prevent an infinity loop of copying.
     *
     * @param src the source directory
     * @param dest the destination directory
     * @return the list of looped directories
     * @throws IOException
     */
    private static Collection<String> getLoopedDirectories(File src, File dest) throws IOException {
        if (!dest.getCanonicalPath().startsWith(src.getCanonicalPath())) {
            return null;
        }
        Collection<String> looped = null;
        File[] srcFiles = src.listFiles();
        if (srcFiles != null && srcFiles.length > 0) {
            looped = new ArrayList<>(srcFiles.length + 1);
            for (File srcFile : srcFiles) {
                File copiedFile = new File(dest, srcFile.getName());
                if (srcFile.isDirectory()) {
                    looped.add(copiedFile.getCanonicalPath());
                }
            }
            if (!dest.exists()) {
                looped.add(dest.getCanonicalPath());
            }
        }
        return looped;
    }

    /**
     * Internal copy directory method.
     *
     * @param srcDir the validated source directory, must not be {@code null}
     * @param destDir the validated destination directory, must not be {@code null}
     * @param excluded the list of directories or files to exclude from the copy, may be null
     * @throws IOException if an error occurs
     */
    private static void doCopyDirectory(File srcDir, File destDir, Collection<String> excluded) throws IOException {
        File[] srcFiles = srcDir.listFiles();
        if (srcFiles == null) { // null if security restricted
            throw new IOException("Failed to list contents of " + srcDir);
        }
        if (destDir.exists()) {
            if (!destDir.isDirectory()) {
                throw new IOException("Destination '" + destDir + "' exists but is not a directory");
            }
        } else {
            if (!destDir.mkdirs() && !destDir.isDirectory()) {
                throw new IOException("Destination '" + destDir + "' directory cannot be created");
            }
        }

        // recurse copying
        for (File srcFile : srcFiles) {
            File dstFile = new File(destDir, srcFile.getName());
            if (excluded == null || !excluded.contains(srcFile.getCanonicalPath())) {
                if (srcFile.isDirectory()) {
                    doCopyDirectory(srcFile, dstFile, excluded);
                } else {
                    doCopyFile(srcFile, dstFile);
                }
            }
        }

        // Try to preserve file date
        destDir.setLastModified(srcDir.lastModified());
    }

    /**
     * Internal copy file method.
     *
     * @param srcFile the validated source file, must not be {@code null}
     * @param destFile the validated destination file, must not be {@code null}
     * @throws IOException if an error occurs
     */
    private static void doCopyFile(File srcFile, File destFile) throws IOException {
        if (destFile.exists() && destFile.isDirectory()) {
            throw new IOException("Destination '" + destFile + "' exists but is a directory");
        }

        FileInputStream fis = null;
        FileOutputStream fos = null;
        FileChannel input = null;
        FileChannel output = null;
        try {
            fis = new FileInputStream(srcFile);
            fos = new FileOutputStream(destFile);
            input = fis.getChannel();
            output = fos.getChannel();
            long size = input.size();
            long pos = 0;
            while (pos < size) {
                pos += output.transferFrom(input, pos, DEFAULT_BUFFER_SIZE);
            }
        } finally {
            IOUtils.closeQuietly(output);
            IOUtils.closeQuietly(fos);
            IOUtils.closeQuietly(input);
            IOUtils.closeQuietly(fis);
        }

        if (srcFile.length() != destFile.length()) {
            throw new IOException("Failed to copy full contents from '" + srcFile + "' to '" + destFile + "'");
        }
        // Try to preserve file date
        destFile.setLastModified(srcFile.lastModified());
    }

    /**
     * Moves a directory or a file.
     * <p>
     * When the destination directory or file is on another file system, do a "copy and delete".
     *
     * @param src the directory or the file to be moved
     * @param dest the destination directory or file
     * @throws NullPointerException if source or destination is {@code null}
     * @throws IOException if source or destination is invalid
     * @throws IOException if an IO error occurs moving the file
     */
    public static void move(File src, File dest) throws IOException {
        if (!src.exists()) {
            throw new FileNotFoundException("Source '" + src + "' does not exist");
        }
        if (dest.exists()) {
            throw new IOException("Destination '" + dest + "' already exists");
        }
        boolean rename = src.renameTo(dest);
        if (!rename) {
            if (src.isDirectory() && dest.getCanonicalPath().startsWith(src.getCanonicalPath())) {
                throw new IOException("Cannot move directory: " + src + " to a subdirectory of itself: " + dest);
            }
            copy(src, dest);
            delete(src);
            if (src.exists()) {
                throw new IOException(
                    "Failed to delete original directory or file '" + src + "' after copy to '" + dest + "'");
            }
        }
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
     * @throws NullPointerException if the directory is {@code null}
     * @throws FileNotFoundException if the file has not been found
     * @throws IOException in case deletion is unsuccessful
     */
    public static void delete(File file) throws IOException {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files == null) { // null if security restricted
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

    /**
     * Gets the name minus the path from a full filename.
     * <p>
     * This method will handle a file in either Unix or Windows format. The text after the last forward or backslash is
     * returned.
     *
     * <pre>
     * a/b/c.txt --> c.txt
     * a.txt     --> a.txt
     * a/b/c     --> c
     * a/b/c/    --> ""
     * </pre>
     * <p>
     *
     * @param filename the filename to query, null returns null
     * @return the name of the file without the path, or an empty string if none exists
     */
    public static String getName(String filename) {
        if (filename == null) {
            return null;
        }
        int sep = getSeparatorIndex(filename);
        return filename.substring(sep + 1);
    }

    /**
     * Gets the base name, minus the full path and extension, from a full filename.
     * <p/>
     * This method will handle a file in either Unix or Windows format. The text after the last forward or backslash and
     * before the last dot is returned.
     *
     * <pre>
     * a/b/c.txt --> c
     * a.b.txt   --> a.b
     * a/b/c     --> c
     * a/b/c/    --> ""
     * </pre>
     * <p/>
     *
     * @param filename the filename to query, null returns null
     * @return the name of the file without the path, or an empty string if none exists
     */
    public static String getBaseName(String filename) {
        if (filename == null) {
            return null;
        }

        int dot = filename.lastIndexOf('.');
        int sep = getSeparatorIndex(filename);
        if (dot > sep) {
            return filename.substring(sep + 1, dot);
        } else {
            return filename.substring(sep + 1);
        }
    }

    /**
     * Gets the extension of a filename.
     * <p>
     * This method returns the textual part of the filename after the last dot. There must be no directory separator
     * after the dot.
     *
     * <pre>
     * a/b/c.txt    --> txt
     * a.b.txt      --> txt
     * a/b.txt/c    --> ""
     * a/b/c        --> ""
     * </pre>
     * <p>
     *
     * @param filename the filename to retrieve the extension of.
     * @return the extension of the file or an empty string if none exists or {@code null} if the filename is
     *         {@code null}.
     */
    public static String getExtension(String filename) {
        if (filename == null) {
            return null;
        }

        int dot = getExtensionIndex(filename);
        if (dot == -1) {
            return StringUtils.EMPTY;
        } else {
            return filename.substring(dot + 1);
        }
    }

    /**
     * Removes the extension from a filename.
     * <p>
     * This method returns the textual part of the filename before the last dot. There must be no directory separator
     * after the dot.
     *
     * <pre>
     * foo.txt    --> foo
     * a\b\c.jpg  --> a\b\c
     * a\b\c      --> a\b\c
     * a.b\c      --> a.b\c
     * </pre>
     * <p>
     *
     * @param filename the filename to query, null returns null
     * @return the filename minus the extension
     */
    public static String removeExtension(String filename) {
        if (filename == null) {
            return null;
        }

        int dot = getExtensionIndex(filename);
        if (dot == -1) {
            return filename;
        } else {
            return filename.substring(0, dot);
        }
    }

    private static int getSeparatorIndex(String filename) {
        int winSep = filename.lastIndexOf('\\');
        int unixSep = filename.lastIndexOf('/');
        return winSep > unixSep ? winSep : unixSep;
    }

    private static int getExtensionIndex(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot == -1) {
            return -1;
        }
        int sep = getSeparatorIndex(filename);
        if (dot > sep) {
            return dot;
        }
        return -1;
    }
}
