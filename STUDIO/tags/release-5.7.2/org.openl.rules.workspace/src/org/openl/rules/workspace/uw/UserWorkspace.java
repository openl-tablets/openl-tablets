package org.openl.rules.workspace.uw;

import java.io.File;
import java.util.List;

import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectsContainer;
import org.openl.rules.workspace.deploy.DeployID;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.RepositoryException;

public interface UserWorkspace extends ProjectsContainer<UserWorkspaceProject> {
    void activate() throws ProjectException;

    void addWorkspaceListener(UserWorkspaceListener listener);

    void copyDDProject(UserWorkspaceDeploymentProject project, String name) throws ProjectException;

    void copyProject(UserWorkspaceProject project, String name) throws ProjectException;

    void createDDProject(String name) throws RepositoryException;

    void createProject(String name) throws ProjectException;

    DeployID deploy(UserWorkspaceDeploymentProject deploymentProject) throws ProjectException;

    UserWorkspaceDeploymentProject getDDProject(String name) throws RepositoryException;

    List<UserWorkspaceDeploymentProject> getDDProjects() throws RepositoryException;

    DesignTimeRepository getDesignTimeRepository();

    File getLocalWorkspaceLocation();

    boolean hasDDProject(String name);

    void passivate();

    void refresh() throws ProjectException;

    void release();

    boolean removeWorkspaceListener(UserWorkspaceListener listener);

    void uploadLocalProject(String name) throws ProjectException;
}
