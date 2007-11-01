package org.openl.rules.dtr;

import org.openl.rules.commons.projects.ProjectArtefact;
import org.openl.rules.commons.projects.ProjectException;
import org.openl.rules.commons.projects.ProjectVersion;

import java.util.Collection;

public interface RepositoryProjectArtefact extends ProjectArtefact {
    RepositoryProjectArtefact getArtefact(String name) throws ProjectException;

    // all for project, main for content
    Collection<ProjectVersion> getVersions();
}
