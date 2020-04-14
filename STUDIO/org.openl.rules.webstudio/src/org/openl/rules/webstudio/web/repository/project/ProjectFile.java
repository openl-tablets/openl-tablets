package org.openl.rules.webstudio.web.repository.project;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.openl.util.FileUtils;
import org.richfaces.model.UploadedFile;

public class ProjectFile {

    private final String name;
    private InputStream input;
    private long size;
    private File tempFile;

    public ProjectFile(String name, InputStream input) {
        this.name = name;
        this.input = input;
    }

    public ProjectFile(UploadedFile uploadedFile) throws IOException {
        this.name = FileUtils.getName(uploadedFile.getName());
        this.size = uploadedFile.getSize();

        this.tempFile = File.createTempFile("openl-upload", null);
        uploadedFile.write(tempFile.getPath());
    }

    public String getName() {
        return name;
    }

    public InputStream getInput() throws IOException {
        if (tempFile != null) {
            return new FileInputStream(tempFile);
        } else {
            return input;
        }
    }

    public long getSize() {
        return size;
    }

    public void destroy() {
        if (tempFile != null) {
            FileUtils.deleteQuietly(tempFile);
            tempFile = null;
        }
    }
}
