package org.openl.rules.workspace.uw;

import java.io.InputStream;

import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectResource;

public interface UserWorkspaceProjectResource extends ProjectResource, UserWorkspaceProjectArtefact {
    void setContent(InputStream inputStream) throws ProjectException;
}
