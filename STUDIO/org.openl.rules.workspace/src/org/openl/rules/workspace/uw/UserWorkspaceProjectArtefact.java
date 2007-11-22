package org.openl.rules.workspace.uw;

import java.util.Collection;

import org.openl.rules.workspace.abstracts.ProjectArtefact;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectVersion;
import org.openl.rules.workspace.repository.RulesRepositoryArtefact;

public interface UserWorkspaceProjectArtefact extends ProjectArtefact, RulesRepositoryArtefact {
    UserWorkspaceProjectArtefact getArtefact(String name) throws ProjectException;

    // all for project, main for content
    Collection<ProjectVersion> getVersions();
    
    void delete() throws ProjectException;
    
    boolean isReadOnly();
}
