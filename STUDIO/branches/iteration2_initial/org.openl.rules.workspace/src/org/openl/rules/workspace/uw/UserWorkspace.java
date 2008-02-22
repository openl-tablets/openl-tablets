package org.openl.rules.workspace.uw;

import java.io.File;
import java.util.List;

import org.acegisecurity.annotation.Secured;
import org.openl.rules.security.Privileges;
import org.openl.rules.workspace.abstracts.DeploymentDescriptorProject;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectsContainer;
import org.openl.rules.workspace.dtr.RepositoryException;
import org.openl.rules.workspace.dtr.DesignTimeRepository;

public interface UserWorkspace extends ProjectsContainer<UserWorkspaceProject> {
	@Secured (Privileges.PRIVILEGE_EDIT)
    void createProject(String name) throws ProjectException;
    
    void activate() throws ProjectException;
    void passivate();
    void release();

    void refresh() throws ProjectException;

    File getLocalWorkspaceLocation();

	@Secured (Privileges.PRIVILEGE_DEPLOY)
    void deploy(DeploymentDescriptorProject deployProject) throws ProjectException;

	UserWorkspaceDeploymentProject getDDProject(String name) throws RepositoryException;
    List<UserWorkspaceDeploymentProject> getDDProjects() throws RepositoryException;

	@Secured (Privileges.PRIVILEGE_EDIT)
    void createDDProject(String name) throws RepositoryException;
	@Secured (Privileges.PRIVILEGE_EDIT)
    void copyProject(UserWorkspaceProject project, String name) throws ProjectException;
	@Secured (Privileges.PRIVILEGE_EDIT)
    void copyDDProject(UserWorkspaceDeploymentProject project, String name) throws ProjectException;
    boolean hasDDProject(String name);

    void addWorkspaceListener(UserWorkspaceListener listener);
    boolean removeWorkspaceListener(UserWorkspaceListener listener);
    DesignTimeRepository getDesignTimeRepository();

	@Secured (Privileges.PRIVILEGE_EDIT)
    void uploadLocalProject(String name) throws ProjectException;
}
