package org.openl.rules.deploy;

import java.io.IOException;
import java.io.InputStream;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.IProjectResource;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Repository;

public class LocalProjectResource extends ALocalProjectArtefact implements IProjectResource {

    private final Repository repository;
    private final FileData data;

    public LocalProjectResource(String name, Repository repository, FileData data) {
        super(name);
        this.repository = repository;
        this.data = data;
    }

    /**
     * Opens a fresh stream on demand. The repository is kept open for the whole deployment lifetime, so the stream must
     * not be retained: the caller owns it and is responsible for closing it.
     */
    @Override
    public InputStream getContent() throws ProjectException {
        try {
            FileItem item = repository.read(data.getName());
            if (item == null) {
                throw new ProjectException("Resource ''{0}'' is not found.", null, getName());
            }
            return item.getStream();
        } catch (IOException e) {
            throw new ProjectException("Failed to read resource ''{0}''.", e, getName());
        }
    }

    public FileData getFileData() {
        return data;
    }
}
