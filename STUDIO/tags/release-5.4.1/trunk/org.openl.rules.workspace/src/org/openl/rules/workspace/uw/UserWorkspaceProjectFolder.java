package org.openl.rules.workspace.uw;

import java.util.Collection;

import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectFolder;
import org.openl.rules.workspace.abstracts.ProjectResource;

public interface UserWorkspaceProjectFolder extends ProjectFolder, UserWorkspaceProjectArtefact {
    UserWorkspaceProjectFolder addFolder(String name) throws ProjectException;

    UserWorkspaceProjectResource addResource(String name, ProjectResource resource) throws ProjectException;

    Collection<? extends UserWorkspaceProjectArtefact> getArtefacts();
}
