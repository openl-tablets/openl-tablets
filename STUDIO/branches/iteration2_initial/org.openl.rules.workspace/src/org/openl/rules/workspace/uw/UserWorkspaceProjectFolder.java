package org.openl.rules.workspace.uw;

import java.util.Collection;

import org.acegisecurity.annotation.Secured;
import org.openl.rules.security.Privileges;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectFolder;
import org.openl.rules.workspace.abstracts.ProjectResource;

public interface UserWorkspaceProjectFolder extends ProjectFolder, UserWorkspaceProjectArtefact {
    Collection<? extends UserWorkspaceProjectArtefact> getArtefacts();

	@Secured (Privileges.PRIVILEGE_EDIT)
    UserWorkspaceProjectFolder addFolder(String name) throws ProjectException;
	@Secured (Privileges.PRIVILEGE_EDIT)
    UserWorkspaceProjectResource addResource(String name, ProjectResource resource) throws ProjectException;
}
