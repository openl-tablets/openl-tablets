package org.openl.rules.workspace.dtr.impl;

import org.openl.rules.workspace.abstracts.ArtefactPath;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.dtr.RepositoryProjectArtefact;
import org.openl.rules.workspace.dtr.RepositoryProjectResource;

import java.io.InputStream;

public class RepositoryProjectResourceImpl extends RepositoryProjectArtefactImpl implements RepositoryProjectResource {
    private String resourceType;

    public RepositoryProjectResourceImpl(String name, ArtefactPath path, String resourceType) {
        super(name, path);
        this.resourceType = resourceType;
    }

    public RepositoryProjectArtefact getArtefact(String name) throws ProjectException {
        throw new ProjectException("Cannot find project artefact ''{0}''", name);
    }

    public InputStream getContent() throws ProjectException {
        throw new ProjectException("TODO");
    }

    public String getResourceType() {
        return resourceType;
    }
}
