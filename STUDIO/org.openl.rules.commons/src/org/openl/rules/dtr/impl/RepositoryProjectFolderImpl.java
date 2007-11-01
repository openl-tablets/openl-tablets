package org.openl.rules.dtr.impl;

import org.openl.rules.dtr.RepositoryProjectFolder;
import org.openl.rules.dtr.RepositoryProjectArtefact;
import org.openl.rules.commons.projects.ProjectException;
import org.openl.rules.commons.artefacts.ArtefactPath;

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
