package org.openl.rules.webstudio.web.repository.upload.zip;

import java.io.IOException;
import java.io.InputStream;

/**
 * Used by {@link ZipWalker} while iterating zip entries.
 */
public interface ZipEntryCommand {
    /**
     * Execute action on file entry
     *
     * @param filePath    path to the current file
     * @param inputStream input stream with content of current file
     * @return true if continue entries iteration, false to stop iteration
     * @throws IOException if an I/O error occurs
     */
    boolean execute(String filePath, InputStream inputStream) throws IOException;

    /**
     * Execute action on directory entry
     *
     * @param directoryPath path to the current directory
     * @return true if continue entries iteration, false to stop iteration
     * @throws IOException if an I/O error occurs
     */
    boolean execute(String directoryPath) throws IOException;
}
