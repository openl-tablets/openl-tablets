package org.openl.util;

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
}
