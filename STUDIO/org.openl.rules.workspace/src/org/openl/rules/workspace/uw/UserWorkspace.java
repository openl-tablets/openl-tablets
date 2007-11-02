package org.openl.rules.workspace.uw;

import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectsContainer;

import java.io.File;

public interface UserWorkspace extends ProjectsContainer<UserWorkspaceProject> {
    void activate() throws ProjectException;
    void passivate();
    void release();

    void refresh() throws ProjectException;

    File getLocalWorkspaceLocation();
}
