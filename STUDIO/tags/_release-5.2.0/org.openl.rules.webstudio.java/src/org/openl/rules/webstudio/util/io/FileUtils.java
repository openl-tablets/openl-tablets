package org.openl.rules.webstudio.util.io;

import java.io.File;
import java.io.IOException;


/**
 * Helper methods related to Files.
 *
 * @author Andrey Naumenko
 */
public class FileUtils {
    /**
     * This function create parent directories for file
     *
     * @param file
     *
     * @throws IOException if directories can not be created
     */
    public static void createParentDirs(File file) throws IOException {
        if ((file != null) && !file.exists() && !file.getParentFile().exists()) {
            if (!file.getParentFile().mkdirs()) {
                throw new IOException("Can't create dirs for file  " + file);
            }
        }
    }
}
