package org.openl.rules.workspace.dtr;

import org.openl.rules.workspace.abstracts.ProjectArtefact;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectVersion;

import java.util.Collection;

public interface RepositoryProjectArtefact extends ProjectArtefact {
    RepositoryProjectArtefact getArtefact(String name) throws ProjectException;

    // all for project, main for content
    Collection<ProjectVersion> getVersions();
}
