package org.openl.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FileTool {

    public static File toTempFile(InputStream source, String fileName) {
        File file = null;
        try {
            file = File.createTempFile(fileName, null);
            IOUtils.copyAndClose(source, new FileOutputStream(file));
        } catch (IOException e) {
            final Logger log = LoggerFactory.getLogger(FileTool.class);
            log.error("Failed to create a file: {}", fileName, e);
        }
        return file;
    }
}
