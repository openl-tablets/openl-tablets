package org.openl.rules.workspace.uw;

import java.io.File;
import java.util.List;

import org.openl.rules.workspace.abstracts.DeploymentDescriptorProject;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectsContainer;
import org.openl.rules.workspace.dtr.RepositoryException;
import org.openl.rules.workspace.dtr.DesignTimeRepository;

public interface UserWorkspace extends ProjectsContainer<UserWorkspaceProject> {
    void createProject(String name) throws ProjectException;
    
    void activate() throws ProjectException;
    void passivate();
    void release();

    void refresh() throws ProjectException;

    File getLocalWorkspaceLocation();

    void deploy(DeploymentDescriptorProject deployProject) throws ProjectException;
    UserWorkspaceDeploymentProject getDDProject(String name) throws RepositoryException;
    List<UserWorkspaceDeploymentProject> getDDProjects() throws RepositoryException;
    void createDDProject(String name) throws RepositoryException;
    void copyProject(UserWorkspaceProject project, String name) throws ProjectException;
    void copyDDProject(UserWorkspaceDeploymentProject project, String name) throws ProjectException;
    boolean hasDDProject(String name);

    void addWorkspaceListener(UserWorkspaceListener listener);
    boolean removeWorkspaceListener(UserWorkspaceListener listener);
    DesignTimeRepository getDesignTimeRepository();

    void uploadLocalProject(String name) throws ProjectException;
}
