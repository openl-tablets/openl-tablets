package org.openl.rules.workspace.dtr;

import org.openl.rules.workspace.abstracts.ProjectArtefact;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectVersion;
import org.openl.rules.workspace.repository.RulesRepositoryArtefact;

import java.util.Collection;

public interface RepositoryProjectArtefact extends ProjectArtefact, RulesRepositoryArtefact {
    void delete() throws ProjectException;

    RepositoryProjectArtefact getArtefact(String name) throws ProjectException;

    // all for project, main for content
    Collection<ProjectVersion> getVersions();

    void update(ProjectArtefact srcArtefact) throws ProjectException;
}
