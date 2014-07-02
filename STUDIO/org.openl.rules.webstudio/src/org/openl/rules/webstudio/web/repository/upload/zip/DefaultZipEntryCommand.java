package org.openl.rules.webstudio.web.repository.upload.zip;

import java.io.IOException;
import java.io.InputStream;

/**
 * Default implementation that does nothing.
 */
public class DefaultZipEntryCommand implements ZipEntryCommand {
    @Override
    public boolean execute(String directoryPath) throws IOException {
        return true;
    }

    @Override
    public boolean execute(String filePath, InputStream inputStream) throws IOException {
        return true;
    }
}
