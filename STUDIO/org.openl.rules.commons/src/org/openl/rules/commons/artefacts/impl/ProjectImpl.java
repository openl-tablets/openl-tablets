package org.openl.rules.commons.artefacts.impl;

import org.openl.rules.commons.artefacts.*;

public class ProjectImpl extends ArtefactImpl implements Project {

    public ProjectImpl(String name, Workspace workspace) throws ArtefactException {
        super(name, workspace);
    }

    /** {@inheritDoc} */
    public Workspace getWorkspace() throws ArtefactException {
        return (Workspace) getParent();
    }

    /** {@inheritDoc} */
    public ArtefactPath getArtefactPath(Artefact artefact) throws ArtefactException {
        Workspace workspace = getWorkspace();
        return workspace.getArtefactPath(artefact);
    }

    /** {@inheritDoc} */
    public Artefact findArtefactByPath(ArtefactPath artefactPath) throws ArtefactException {
        // check that first segment is legal (this project)
        Workspace workspace = getWorkspace();
        return workspace.findArtefactByPath(artefactPath);
    }
}
