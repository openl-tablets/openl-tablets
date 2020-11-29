package org.openl.rules.deploy;

import java.io.InputStream;

import org.openl.rules.project.abstraction.IProjectResource;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;

public class LocalProjectResource extends ALocalProjectArtefact implements IProjectResource {

    private final FileItem file;

    public LocalProjectResource(String name, FileItem file) {
        super(name);
        this.file = file;
    }

    @Override
    public InputStream getContent() {
        return file.getStream();
    }

    public FileData getFileData() {
        return file.getData();
    }
}
