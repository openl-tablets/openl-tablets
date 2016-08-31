package org.openl.rules.project.abstraction;

import java.io.InputStream;

import org.openl.rules.common.CommonUser;
import org.openl.rules.common.ProjectException;
import org.openl.rules.repository.api.ResourceAPI;
import org.openl.util.IOUtils;

public class AProjectResource extends AProjectArtefact {
    private ResourceTransformer resourceTransformer;

    public AProjectResource(ResourceAPI api, AProject project) {
        super(api, project);
    }
    
    @Override
    public ResourceAPI getAPI() {
        return (ResourceAPI)super.getAPI();
    }

    public InputStream getContent() throws ProjectException {
        return getAPI().getContent();
    }

    public void setContent(InputStream inputStream) throws ProjectException {
        getAPI().setContent(inputStream);
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
        commit(user);
    }

    @Override
    public void smartUpdate(AProjectArtefact artefact, CommonUser user) throws ProjectException {
        if (artefact.isModified()) {
            super.smartUpdate(artefact, user);
            AProjectResource resource = (AProjectResource) artefact;
            setContent(resource);
            commit(user);
        }
    }

    public void setResourceTransformer(ResourceTransformer resourceTransformer) {
        this.resourceTransformer = resourceTransformer;
    }
}
