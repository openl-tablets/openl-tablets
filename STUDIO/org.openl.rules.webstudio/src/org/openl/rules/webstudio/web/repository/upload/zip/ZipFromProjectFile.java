package org.openl.rules.webstudio.web.repository.upload.zip;

import java.io.IOException;
import java.io.InputStream;

import org.openl.rules.webstudio.web.repository.project.ProjectFile;

public class ZipFromProjectFile implements ZipCharsetDetector.ZipSource {
    private final ProjectFile uploadedFile;

    public ZipFromProjectFile(ProjectFile uploadedFile) {
        this.uploadedFile = uploadedFile;
    }

    @Override
    public InputStream createStream() throws IOException {
        return uploadedFile.getInput();
    }
}
