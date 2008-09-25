package org.openl.rules.workspace.uw;

import java.util.Collection;

import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectFolder;
import org.openl.rules.workspace.abstracts.ProjectResource;

public interface UserWorkspaceProjectFolder extends ProjectFolder, UserWorkspaceProjectArtefact {
    Collection<? extends UserWorkspaceProjectArtefact> getArtefacts();

    UserWorkspaceProjectFolder addFolder(String name) throws ProjectException;
    UserWorkspaceProjectResource addResource(String name, ProjectResource resource) throws ProjectException;
}
