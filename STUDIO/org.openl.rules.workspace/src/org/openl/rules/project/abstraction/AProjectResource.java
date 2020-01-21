package org.openl.rules.project.abstraction;

import java.io.IOException;
import java.io.InputStream;

import org.openl.rules.common.CommonUser;
import org.openl.rules.common.ProjectException;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.util.IOUtils;

public class AProjectResource extends AProjectArtefact {
    private ResourceTransformer resourceTransformer;

    public AProjectResource(AProject project, Repository repository, FileData fileData) {
        super(project, repository, fileData);
    }

    public InputStream getContent() throws ProjectException {
        try {
            if (isHistoric()) {
                return getRepository().readHistory(getFileData().getName(), getFileData().getVersion()).getStream();
            } else {
                return getRepository().read(getFileData().getName()).getStream();
            }
        } catch (IOException ex) {
            throw new ProjectException(ex.getMessage(), ex);
        }
    }

    public void setContent(InputStream inputStream) throws ProjectException {
        try {
            getProject().tryLockOrThrow();

            setFileData(getRepository().save(getFileData(), inputStream));
        } catch (IOException ex) {
            throw new ProjectException("Cannot set content", ex);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    @Override
    public boolean isFolder() {
        return false;
    }

    @Override
    public void update(AProjectArtefact artefact, CommonUser user) throws ProjectException {
        super.update(artefact, user);
        AProjectResource resource = (AProjectResource) artefact;
        setContent(resourceTransformer != null ? resourceTransformer.transform(resource) : resource.getContent());
    }

    public void setResourceTransformer(ResourceTransformer resourceTransformer) {
        this.resourceTransformer = resourceTransformer;
    }
}
