package org.openl.rules.project.abstraction;

import java.io.InputStream;

import org.openl.rules.common.CommonUser;
import org.openl.rules.common.ProjectException;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.util.IOUtils;

public class AProjectResource extends AProjectArtefact {
    private ResourceTransformer resourceTransformer;
    private final ContentHandler contentHandler;

    public AProjectResource(AProject project, Repository repository, FileData fileData) {
        super(project, repository, fileData);
        contentHandler = null;
    }

    public AProjectResource(AProject project, Repository repository, FileData fileData, ContentHandler contentHandler) {
        super(project, repository, fileData);
        this.contentHandler = contentHandler;
    }

    public InputStream getContent() throws ProjectException {
        if (contentHandler != null) {
            return contentHandler.loadContent();
        }
        else {
            if (isHistoric()) {
                return getRepository().readHistory(getFileData().getName(), getFileData().getVersion()).getStream();
            }
            else {
                return getRepository().read(getFileData().getName()).getStream();
            }
        }
    }

    public void setContent(InputStream inputStream) throws ProjectException {
        if (contentHandler != null) {
            throw new UnsupportedOperationException("Can't set content if contentHandler is initialized");
        }
        setFileData(getRepository().save(getFileData(), inputStream));
        IOUtils.closeQuietly(inputStream);
    }

    private void setContent(AProjectResource resource) throws ProjectException {
        setContent(resourceTransformer != null ? resourceTransformer.tranform(resource) : resource.getContent());
    }

    @Override
    public boolean isFolder() {
    	return false;
    }

    @Override
    public void update(AProjectArtefact artefact, CommonUser user) throws ProjectException {
        super.update(artefact, user);
        AProjectResource resource = (AProjectResource)artefact;
        setContent(resource);
    }

    public void setResourceTransformer(ResourceTransformer resourceTransformer) {
        this.resourceTransformer = resourceTransformer;
    }

    @Override
    public String getName() {
        String name = getFileData().getName();

        AProject project = getProject();
        if (project == null) {
            return name.substring(name.lastIndexOf("/") + 1);
        } else {
            String parentPath = getProject().getFolderPath();
            return name.substring(parentPath.length() + 1);
        }
    }
}
