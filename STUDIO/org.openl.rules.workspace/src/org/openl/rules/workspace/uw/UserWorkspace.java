package org.openl.rules.workspace.uw;

import java.util.Collection;
import java.util.List;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.project.abstraction.LockEngine;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.abstracts.ProjectsContainer;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.RepositoryException;
import org.openl.rules.workspace.lw.LocalWorkspace;

public interface UserWorkspace extends ProjectsContainer {
    void activate();

    void addWorkspaceListener(UserWorkspaceListener listener);

    void copyDDProject(ADeploymentProject project, String name, String comment) throws ProjectException;

    ADeploymentProject createDDProject(String name) throws RepositoryException;

    ADeploymentProject getDDProject(String name) throws ProjectException;

    ADeploymentProject getLatestDeploymentConfiguration(String name);

    List<ADeploymentProject> getDDProjects() throws ProjectException;

    DesignTimeRepository getDesignTimeRepository();

    LocalWorkspace getLocalWorkspace();

    boolean hasDDProject(String name);

    void passivate();

    void refresh();

    void release();

    void removeWorkspaceListener(UserWorkspaceListener listener);

    void uploadLocalProject(String repositoryId, String name, String projectFolder, String comment) throws ProjectException;

    WorkspaceUser getUser();

    @Override
    RulesProject getProject(String repositoryId, String name) throws ProjectException;

    RulesProject getProject(String repositoryId, String name, boolean refreshBefore) throws ProjectException;

    @Override
    Collection<RulesProject> getProjects();

    Collection<RulesProject> getProjects(boolean refreshBefore);

    LockEngine getProjectsLockEngine();
}
