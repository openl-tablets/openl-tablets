package org.openl.rules.workspace.dtr.impl;

import org.openl.rules.workspace.abstracts.ArtefactPath;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.dtr.RepositoryProjectArtefact;
import org.openl.rules.workspace.dtr.RepositoryProjectFolder;

import java.util.Collection;
import java.util.LinkedList;

public class RepositoryProjectFolderImpl extends RepositoryProjectArtefactImpl implements RepositoryProjectFolder {
    public RepositoryProjectFolderImpl(String name, ArtefactPath path) {
        super(name, path);
    }

    public RepositoryProjectArtefact getArtefact(String name) throws ProjectException {
        throw new ProjectException("Cannot find project artefact ''{0}''", name);
    }

    public Collection<RepositoryProjectArtefact> getArtefacts() {
        return new LinkedList<RepositoryProjectArtefact>();
    }
}
