package org.openl.rules.project.abstraction;

import java.io.InputStream;

import org.openl.rules.project.impl.ProjectArtefactAPI;
import org.openl.rules.workspace.abstracts.ProjectException;

//TODO: check isCheckedOut?
public class AProjectResource extends AProjectArtefact {
    public AProjectResource(ProjectArtefactAPI api, AProject project) {
        super(api, project);
    }

    public InputStream getContent() throws ProjectException {
        return impl.getContent();
    }

    public void setContent(InputStream inputStream) throws ProjectException {
        impl.setContent(inputStream);
    }

    public String getResourceType() {
        return impl.getResourceType();
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
