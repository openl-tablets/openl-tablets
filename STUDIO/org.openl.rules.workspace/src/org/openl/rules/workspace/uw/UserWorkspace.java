package org.openl.rules.workspace.uw;

import org.openl.rules.workspace.abstracts.DeploymentDescriptorProject;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectsContainer;
import org.openl.rules.workspace.dtr.RepositoryException;

import java.io.File;
import java.util.List;

public interface UserWorkspace extends ProjectsContainer<UserWorkspaceProject> {
    void createProject(String name) throws ProjectException;
    
    void activate() throws ProjectException;
    void passivate();
    void release();

    void refresh() throws ProjectException;

    File getLocalWorkspaceLocation();



    public DeploymentDescriptorProject getDDProject(String name) throws RepositoryException;
    public List<DeploymentDescriptorProject> getDDProjects() throws RepositoryException;
    public void createDDProject(String name) throws RepositoryException;
}
