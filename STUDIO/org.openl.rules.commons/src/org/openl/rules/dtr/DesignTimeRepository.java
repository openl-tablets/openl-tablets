package org.openl.rules.dtr;

import org.openl.rules.commons.projects.ProjectsContainer;
import org.openl.rules.commons.projects.ProjectVersion;
import org.openl.rules.commons.projects.Project;
import org.openl.rules.WorkspaceUser;

public interface DesignTimeRepository extends ProjectsContainer<RepositoryProject> {
    RepositoryProject getProjectVersion(String name, ProjectVersion version) throws RepositoryException;

    void updateProject(Project project, WorkspaceUser user) throws RepositoryException;
    void copyProject(Project project, String name) throws RepositoryException;
}
