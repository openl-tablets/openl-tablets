package org.openl.rules.project.abstraction;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.openl.rules.common.ProjectException;
import org.openl.rules.repository.api.ResourceAPI;

public class AProjectResource extends AProjectArtefact {
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

    public String getResourceType() {
        return getAPI().getResourceType();
    }

    @Override
    public boolean isFolder() {
    	return false;
    }
    
    @Override
    public void update(AProjectArtefact artefact) throws ProjectException {
        super.update(artefact);
        AProjectResource resource = (AProjectResource)artefact;
        setContent(resource.getContent());
    }
}
