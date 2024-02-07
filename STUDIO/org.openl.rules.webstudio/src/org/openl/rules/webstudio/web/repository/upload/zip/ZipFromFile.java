package org.openl.rules.webstudio.web.repository.upload.zip;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class ZipFromFile implements ZipCharsetDetector.ZipSource {
    private final File uploadedFile;

    public ZipFromFile(File uploadedFile) {
        this.uploadedFile = uploadedFile;
    }

    @Override
    public InputStream createStream() throws FileNotFoundException {
        return new FileInputStream(uploadedFile);
    }
}
