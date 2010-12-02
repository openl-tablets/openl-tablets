package org.openl.rules.workspace.uw;

import java.util.List;

import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectsContainer;
import org.openl.rules.workspace.deploy.DeployID;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.RepositoryException;
import org.openl.rules.workspace.lw.LocalWorkspace;

public interface UserWorkspace extends ProjectsContainer {
    void activate() throws ProjectException;

    void addWorkspaceListener(UserWorkspaceListener listener);

    void copyDDProject(ADeploymentProject project, String name) throws ProjectException;

    void copyProject(AProject project, String name) throws ProjectException;

    void createDDProject(String name) throws RepositoryException;

    void createProject(String name) throws ProjectException;

    DeployID deploy(ADeploymentProject deploymentProject) throws ProjectException;

    ADeploymentProject getDDProject(String name) throws ProjectException;

    List<ADeploymentProject> getDDProjects() throws ProjectException;

    DesignTimeRepository getDesignTimeRepository();

    LocalWorkspace getLocalWorkspace();

    boolean hasDDProject(String name);

    void passivate();

    void refresh() throws ProjectException;

    void release();

    boolean removeWorkspaceListener(UserWorkspaceListener listener);

    void uploadLocalProject(String name) throws ProjectException;
    
    WorkspaceUser getUser();
}
