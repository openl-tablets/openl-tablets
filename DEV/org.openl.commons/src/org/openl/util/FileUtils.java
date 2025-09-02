package org.openl.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;

import org.slf4j.LoggerFactory;

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
     * @param src  an existing file to copy, must not be {@code null}
     * @param dest the new file, must not be {@code null}
     * @throws NullPointerException if source or destination is {@code null}
     * @throws IOException          if source or destination is invalid
     * @throws IOException          if an IO error occurs during copying
     */
    public static void copy(File src, File dest) throws IOException {
        if (!src.exists()) {
            throw new FileNotFoundException(String.format("Source '%s' does not exist", src));
        }
        final String srcPath = src.getCanonicalPath();
        final String destPath = dest.getCanonicalPath();
        if (srcPath.equals(destPath)) {
            throw new IOException(String.format("Source '%s' and destination '%s' are the same", src, dest));
        }

        if (src.isDirectory()) {
            Collection<String> looped = getLoopedDirectories(src, dest);
            doCopyDirectory(src, dest, looped);
        } else {
            if (destPath.startsWith(srcPath)) {
                throw new IOException(
                        String.format("Destination '%s' has the same path of the source '%s'", dest, src));
            }
            File destFile = dest;
            if (dest.isDirectory()) {
                destFile = new File(dest, src.getName());
            } else {
                File parentFile = dest.getParentFile();
                if (parentFile != null && !parentFile.mkdirs() && !parentFile.isDirectory()) {
                    throw new IOException(String.format("Destination '%s' directory cannot be created", parentFile));
                }
            }
            doCopyFile(src, destFile);
        }
    }

    /**
     * Collects nested directories which should be excluded for copying to prevent an infinity loop of copying.
     *
     * @param src  the source directory
     * @param dest the destination directory
     * @return the list of looped directories
     * @throws IOException if an I/O error occurs
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
     * @param srcDir   the validated source directory, must not be {@code null}
     * @param destDir  the validated destination directory, must not be {@code null}
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
                throw new IOException(String.format("Destination '%s' exists but is not a directory", destDir));
            }
        } else {
            if (!destDir.mkdirs() && !destDir.isDirectory()) {
                throw new IOException(String.format("Destination '%s' directory cannot be created", destDir));
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
        if (!destDir.setLastModified(srcDir.lastModified())) {
            LoggerFactory.getLogger(FileUtils.class).warn("Failed to set modified time to file '{}'.", destDir);
        }
    }

    /**
     * Internal copy file method.
     *
     * @param srcFile  the validated source file, must not be {@code null}
     * @param destFile the validated destination file, must not be {@code null}
     * @throws IOException if an error occurs
     */
    private static void doCopyFile(File srcFile, File destFile) throws IOException {
        if (destFile.exists() && destFile.isDirectory()) {
            throw new IOException(String.format("Destination '%s' exists but is a directory", destFile));
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
            throw new IOException(String.format("Failed to copy full contents from '%s' to '%s'", srcFile, destFile));
        }
        // Try to preserve file date
        if (!destFile.setLastModified(srcFile.lastModified())) {
            LoggerFactory.getLogger(FileUtils.class).warn("Failed to set modified time to file '{}'.", destFile);
        }
    }

    /**
     * Deletes a path. If provided path is a directory, delete it and all sub-directories.
     *
     * @param root path to file or directory to delete, must not be {@code null}
     * @throws NullPointerException  if the directory is {@code null}
     * @throws FileNotFoundException if the file has not been found
     * @throws IOException           in case deletion is unsuccessful
     */
    public static void delete(Path root) throws IOException {
        if (!Files.exists(root)) {
            throw new FileNotFoundException("Path does not exist: " + root);
        }
        var theFirstException = new IOException[1];
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                try {
                    Files.deleteIfExists(file);
                } catch (IOException e) {
                    if (theFirstException[0] == null) {
                        theFirstException[0] = e;
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                try {
                    Files.deleteIfExists(dir);
                } catch (IOException e) {
                    if (theFirstException[0] == null) {
                        theFirstException[0] = e;
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });

        if (theFirstException[0] != null) {
            throw theFirstException[0];
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
        if (file == null) {
            return;
        }
        try {
            delete(file.toPath());
        } catch (Exception ignored) {
            // ignore
        }
    }

    public static void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            delete(path);
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
     * {@code null}.
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
        return Math.max(winSep, unixSep);
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

    /**
     * Checks if a path matches an Ant-style pattern.
     * <p>
     * This method uses the {@link #antPatternToRegex(String)} function to convert the Ant-style pattern
     * to a Java regex pattern and then performs the matching.
     * <p>
     * <p><b>Examples:</b>
     * <ul>
     * <li>{@code pathMatches("com/t?st.jsp", "com/test.jsp")} &rarr; {@code true}</li>
     * <li>{@code pathMatches("com/*.jsp", "com/index.jsp")} &rarr; {@code true}</li>
     * <li>{@code pathMatches("com/** /storage", "com/index.jsp")} &rarr; {@code true}</li>
     * <li>{@code pathMatches("com/** /storage", "com/project/internal/storage")} &rarr; {@code true}</li>
     * <li>{@code pathMatches("com/** /storage", "com/project/storage")} &rarr; {@code true}</li>
     * <li>{@code pathMatches("com/t?st.jsp", "com/tast.jsp")} &rarr; {@code true}</li>
     * <li>{@code pathMatches("com/t?st.jsp", "com/toast.jsp")} &rarr; {@code false}</li>
     * <li>{@code pathMatches("com/*.jsp", "com/project/index.jsp")} &rarr; {@code false}</li>
     * </ul>
     */
    public static boolean pathMatches(String antPattern, String path) {
        if (antPattern == null || path == null) {
            throw new NullPointerException("Ant pattern and path must not be null");
        }

        // Normalize path separators to forward slashes
        var normalizedPath = path.replace('\\', '/');
        var normalizedPattern = antPattern.replace('\\', '/');

        // Convert Ant pattern to regex and match
        var regex = antPatternToRegex(normalizedPattern);
        return normalizedPath.matches(regex);
    }

    /**
     * Converts an Ant-style pattern to a Java regex pattern.
     * <p>
     * This method converts the following Ant-style wildcards to regex equivalents:
     * <ul>
     * <li>{@code ?} - matches exactly one character (except path separators) -&gt; {@code [^/]}</li>
     * <li>{@code *} - matches zero or more characters except path separators -&gt; {@code [^/]*}</li>
     * <li>{@code **} - matches zero or more characters including path separators -&gt; {@code .*}</li>
     * </ul>
     * </p>
     *
     */
    private static String antPatternToRegex(String antPattern) {
        if (antPattern == null) {
            throw new NullPointerException("Ant pattern must not be null");
        }
        
        StringBuilder regex = new StringBuilder();
        regex.append('^'); // Start of string
        
        int length = antPattern.length();
        for (int i = 0; i < length; i++) {
            char c = antPattern.charAt(i);
            
            switch (c) {
                case '?':
                    // ? matches exactly one character except path separator
                    regex.append("[^/]");
                    break;
                    
                case '*':
                    // Check if this is a ** pattern
                    if (i + 1 < length && antPattern.charAt(i + 1) == '*') {
                        // ** matches zero or more characters including path separators
                        regex.append(".*");
                        i++; // skip the second *
                        if (i + 1 < length && antPattern.charAt(i + 1) == '/') {
                            i++; // skip the '/' as it is already included in regexp
                        }
                    } else {
                        // * matches zero or more characters except path separators
                        regex.append("[^/]*");
                    }
                    break;
                    
                case '.':
                case '^':
                case '$':
                case '[':
                case ']':
                case '(':
                case ')':
                case '{':
                case '}':
                case '+':
                case '|':
                case '\\':
                    // Escape regex special characters
                    regex.append('\\');
                    // Fall through to the default
                default:
                    // Regular character
                    regex.append(c);
                    break;
            }
        }
        
        regex.append('$'); // End of string
        return regex.toString();
    }
}
