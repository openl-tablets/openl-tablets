package org.openl.rules.uw;

import org.openl.rules.commons.projects.ProjectsContainer;
import org.openl.rules.commons.projects.ProjectException;

public interface UserWorkspace extends ProjectsContainer<UserWorkspaceProject> {
    void activate() throws ProjectException;
    void passivate();
    void release();

    void refresh() throws ProjectException;
}
