package org.openl.rules.webstudio.web.repository.project;

import org.openl.util.FileUtils;
import org.richfaces.model.UploadedFile;

import java.io.InputStream;

public class ProjectFile {

    private String name;
    private InputStream input;
    private long size;
    private UploadedFile uploadedFile;

    public ProjectFile(String name, InputStream input) {
        this.name = name;
        this.input = input;
    }

    public ProjectFile(UploadedFile uploadedFile) {
        this.name = FileUtils.getName(uploadedFile.getName());
        this.size = uploadedFile.getSize();
        this.uploadedFile = uploadedFile;
    }

    public String getName() {
        return name;
    }

    public InputStream getInput() {
        if (uploadedFile != null) {
            // returns a new instance for each call
            // In some cases the same input stream is used several times. See ZipWalker implementation.
            return uploadedFile.getInputStream();
        } else {
            return input;
        }
    }

    public long getSize() {
        return size;
    }
}
