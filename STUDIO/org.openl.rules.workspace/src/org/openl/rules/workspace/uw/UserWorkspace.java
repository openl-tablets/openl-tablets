package org.openl.rules.workspace.uw;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.LockEngine;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.abstracts.ProjectsContainer;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.lw.LocalWorkspace;

public interface UserWorkspace extends ProjectsContainer {
    void activate();

    void addWorkspaceListener(UserWorkspaceListener listener);

    DesignTimeRepository getDesignTimeRepository();

    LocalWorkspace getLocalWorkspace();

    void passivate();

    void refresh();

    void syncProjects();

    String getActualName(AProject project) throws ProjectException, IOException;

    void release();

    void removeWorkspaceListener(UserWorkspaceListener listener);

    RulesProject uploadLocalProject(String repositoryId, String name, String projectFolder, String comment) throws ProjectException;

    Optional<RulesProject> getProjectByPath(String repositoryId, String realPath);

    WorkspaceUser getUser();

    @Override
    RulesProject getProject(String repositoryId, String name) throws ProjectException;

    RulesProject getProject(String repositoryId, String name, boolean refreshBefore) throws ProjectException;

    @Override
    Collection<RulesProject> getProjects();

    Collection<RulesProject> getProjects(boolean refreshBefore);

    /**
     * Returns all projects in the workspace whose business name matches the given name (case-insensitive). Since project
     * names are unique only within a single design repository, multiple projects with the same business name may exist
     * across repositories and are all returned.
     * <p>
     * Refreshes the workspace before lookup. Equivalent to {@link #getProjectsByName(String, boolean)} with
     * {@code refreshBefore = true}.
     *
     * @param name project business name to match (case-insensitive)
     * @return matching projects, sorted by business name, repository id, then real path; empty if none found
     */
    Collection<RulesProject> getProjectsByName(String name);

    /**
     * Returns all projects in the workspace whose business name matches the given name (case-insensitive). Since project
     * names are unique only within a single design repository, multiple projects with the same business name may exist
     * across repositories and are all returned.
     *
     * @param name           project business name to match (case-insensitive)
     * @param refreshBefore  if {@code true}, the workspace is refreshed before the lookup; if {@code false}, the cached
     *                       state is used (a refresh still happens when one was previously scheduled)
     * @return matching projects, sorted by business name, repository id, then real path; empty if none found
     */
    Collection<RulesProject> getProjectsByName(String name, boolean refreshBefore);

    LockEngine getProjectsLockEngine();

    boolean isOpenedOtherProject(AProject project);

    @Override
    Collection<RulesProject> getProjects(String repositoryId);
}
