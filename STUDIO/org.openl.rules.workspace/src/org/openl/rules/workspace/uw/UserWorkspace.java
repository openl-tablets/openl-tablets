package org.openl.rules.workspace.uw;

import org.openl.rules.workspace.abstracts.DeploymentDescriptorProject;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectsContainer;
import org.openl.rules.workspace.dtr.RepositoryException;
import org.openl.rules.workspace.deploy.DeploymentException;

import java.io.File;
import java.util.List;

public interface UserWorkspace extends ProjectsContainer<UserWorkspaceProject> {
    void createProject(String name) throws ProjectException;
    
    void activate() throws ProjectException;
    void passivate();
    void release();

    void refresh() throws ProjectException;

    File getLocalWorkspaceLocation();

    void deploy(DeploymentDescriptorProject deployProject) throws ProjectException;
    DeploymentDescriptorProject getDDProject(String name) throws RepositoryException;
    List<DeploymentDescriptorProject> getDDProjects() throws RepositoryException;
    void createDDProject(String name) throws RepositoryException;
}
