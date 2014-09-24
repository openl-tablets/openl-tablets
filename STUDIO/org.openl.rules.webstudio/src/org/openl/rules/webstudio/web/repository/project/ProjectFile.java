package org.openl.rules.webstudio.web.repository.project;

import org.apache.commons.io.FilenameUtils;
import org.richfaces.model.UploadedFile;

import java.io.InputStream;

public class ProjectFile {

    private String name;
    private InputStream input;
    private long size;

    public ProjectFile(String name, InputStream input) {
        this.name = name;
        this.input = input;
    }

    public ProjectFile(UploadedFile uploadedFile) {
        this.name = FilenameUtils.getName(uploadedFile.getName());
        this.input = uploadedFile.getInputStream();
        this.size = uploadedFile.getSize();
    }

    public String getName() {
        return name;
    }

    public InputStream getInput() {
        return input;
    }

    public long getSize() {
        return size;
    }
}
