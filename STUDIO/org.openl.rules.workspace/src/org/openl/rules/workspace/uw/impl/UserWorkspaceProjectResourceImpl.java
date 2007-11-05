package org.openl.rules.workspace.uw.impl;

import java.io.InputStream;

import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectResource;
import org.openl.rules.workspace.dtr.RepositoryProjectResource;
import org.openl.rules.workspace.lw.LocalProjectResource;
import org.openl.rules.workspace.uw.UserWorkspaceProjectArtefact;
import org.openl.rules.workspace.uw.UserWorkspaceProjectResource;

public class UserWorkspaceProjectResourceImpl extends UserWorkspaceProjectArtefactImpl implements UserWorkspaceProjectResource{
    private LocalProjectResource localResource;
    private RepositoryProjectResource dtrResource;
    
    protected UserWorkspaceProjectResourceImpl(UserWorkspaceProjectImpl project, LocalProjectResource localResource, RepositoryProjectResource dtrResource) {
        super(project, localResource, dtrResource);
        
        this.localResource = localResource;
        this.dtrResource = dtrResource;
    }

    public void setContent(InputStream inputStream) throws ProjectException {
        if (!isLocal()) {
            throw new ProjectException("Can modify local resource only!", null);
        }
        
        //TODO -- check that project is in RW mode
        
        localResource.setContent(inputStream);
    }

    public InputStream getContent() throws ProjectException {
        return getResource().getContent();
    }

    public String getResourceType() {
        return getResource().getResourceType();
    }

    public UserWorkspaceProjectArtefact getArtefact(String name) throws ProjectException {
        throw new ProjectException("Cannot find project artefact ''{0}''", null, name);
    }
    
    // --- protected
    
    protected ProjectResource getResource() {
        return (isLocal() ? localResource : dtrResource);
    }
}
