package org.openl.rules.workspace.dtr;

import java.util.Collection;

import org.openl.rules.workspace.abstracts.ProjectFolder;

public interface RepositoryProjectFolder extends ProjectFolder, RepositoryProjectArtefact {
    Collection<RepositoryProjectArtefact> getArtefacts();
}
