package org.openl.rules.workspace.dtr.impl;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.repository.RFile;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.workspace.abstracts.ArtefactPath;
import org.openl.rules.workspace.abstracts.ProjectArtefact;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectResource;
import org.openl.rules.workspace.dtr.RepositoryProjectArtefact;
import org.openl.rules.workspace.dtr.RepositoryProjectResource;

public class RepositoryProjectResourceImpl extends RepositoryProjectArtefactImpl implements RepositoryProjectResource {
    private static final Log log = LogFactory.getLog(RepositoryProjectResourceImpl.class);

    private RFile rulesFile;
    private String resourceType;

    protected RepositoryProjectResourceImpl(RFile rulesFile, ArtefactPath path) {
        super(rulesFile, path);
        this.rulesFile = rulesFile;
        // TODO fix me
        resourceType = "text/plain";
    }

    public void delete() throws ProjectException {
        try {
            rulesFile.delete();
        } catch (RRepositoryException e) {
            throw new ProjectException("Failed to delete project resource ''{0}''!", e, getArtefactPath()
                    .getStringValue());
        }
    }

    public RepositoryProjectArtefact getArtefact(String name) throws ProjectException {
        throw new ProjectException("Cannot find project artefact ''{0}''!", null, name);
    }

    public InputStream getContent() throws ProjectException {
        try {
            return rulesFile.getContent();
        } catch (RRepositoryException e) {
            throw new ProjectException("Cannot get content!", e);
        }
    }

    public String getResourceType() {
        return resourceType;
    }

    public boolean hasArtefact(String name) {
        return false;
    }

    public boolean isFolder() {
        return false;
    }

    @Override
    public void update(ProjectArtefact srcArtefact) throws ProjectException {
        ProjectResource srcResource = (ProjectResource) srcArtefact;
        super.update(srcArtefact);

        // String resType = srcResource.getResourceType();
        // TODO update resource type

        InputStream is = null;

        try {
            is = srcResource.getContent();

            rulesFile.setContent(is);
        } catch (RRepositoryException e) {
            throw new ProjectException("Failed to update project resource ''{0}''!", e, getArtefactPath()
                    .getStringValue());
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    log.error("Failed to close input stream!", e);
                    // ignore
                }
            }
        }
    }
}
