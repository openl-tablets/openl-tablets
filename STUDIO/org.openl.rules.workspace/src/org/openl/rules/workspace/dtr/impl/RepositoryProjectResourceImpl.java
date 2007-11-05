package org.openl.rules.workspace.dtr.impl;

import org.openl.rules.repository.RFile;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.workspace.abstracts.ArtefactPath;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.dtr.RepositoryProjectArtefact;
import org.openl.rules.workspace.dtr.RepositoryProjectResource;

import java.io.InputStream;

public class RepositoryProjectResourceImpl extends RepositoryProjectArtefactImpl implements RepositoryProjectResource {
    private RFile rulesFile;
    private String resourceType;

    protected RepositoryProjectResourceImpl(RFile rulesFile, ArtefactPath path) {
        super(rulesFile, path);
        this.rulesFile = rulesFile;
        this.resourceType = "some-file";
    }

    /** @deprecated */
    public RepositoryProjectResourceImpl(String name, ArtefactPath path, String resourceType) {
        super(name, path);
        this.resourceType = resourceType;
    }

    public RepositoryProjectArtefact getArtefact(String name) throws ProjectException {
        throw new ProjectException("Cannot find project artefact ''{0}''", null, name);
    }

    public InputStream getContent() throws ProjectException {
        try {
            return rulesFile.getContent();
        } catch (RRepositoryException e) {
            throw new ProjectException("Cannot get content", e);
        }        
    }

    public String getResourceType() {
        return resourceType;
    }
}
