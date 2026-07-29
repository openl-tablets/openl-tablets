package org.openl.security.acl.workspace;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.acls.domain.BasePermission;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.LockEngine;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.lw.LocalWorkspace;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.rules.workspace.uw.UserWorkspaceListener;
import org.openl.security.acl.repository.RepositoryAclService;

@RequiredArgsConstructor
@Slf4j
public class SecureUserWorkspaceImpl implements UserWorkspace {

    private final UserWorkspace userWorkspace;
    private final RepositoryAclService designRepositoryAclService;
    private final boolean allowProjectCreateDelete;

    @Override
    public boolean hasProject(String repositoryId, String name) {
        try {
            var project = userWorkspace.getProject(repositoryId, name);
            return designRepositoryAclService.isGranted(project, List.of(BasePermission.READ));
        } catch (ProjectException e) {
            return false;
        }
    }

    @Override
    public List<RulesProject> getProjects(String repositoryId) {
        return userWorkspace.getProjects(repositoryId)
                .stream()
                .filter(this::hasReadAccess)
                .toList();
    }

    /**
     * Checks the read access to the project and evicts the inaccessible copy from the workspace.
     *
     * <p>A revoked permission hides the project from all listings, and the copied data must not stay
     * on the server either. The opened copy is closed: the local files and the registry record are
     * removed. A genuinely local project has no source repository to control the access, so it is
     * kept as is.
     */
    private boolean hasReadAccess(RulesProject project) {
        if (designRepositoryAclService.isGranted(project, List.of(BasePermission.READ))) {
            return true;
        }
        if (project.isOpened() && !project.isLocalOnly()) {
            log.info("Access to the project '{}' was revoked. The copy leaves the user workspace.",
                    project.getName());
            try {
                project.close();
            } catch (ProjectException e) {
                log.warn("Cannot close the revoked project '{}'.", project.getName(), e);
            }
        }
        return false;
    }

    @Override
    public void activate() {
        userWorkspace.activate();
    }

    @Override
    public void addWorkspaceListener(UserWorkspaceListener listener) {
        userWorkspace.addWorkspaceListener(listener);
    }

    @Override
    public DesignTimeRepository getDesignTimeRepository() {
        return userWorkspace.getDesignTimeRepository();
    }

    @Override
    public LocalWorkspace getLocalWorkspace() {
        return userWorkspace.getLocalWorkspace();
    }

    @Override
    public void passivate() {
        userWorkspace.passivate();
    }

    @Override
    public void refresh() {
        userWorkspace.refresh();
    }

    @Override
    public void syncProjects() {
        userWorkspace.syncProjects();
    }

    @Override
    public String getActualName(AProject project) throws ProjectException, IOException {
        if (designRepositoryAclService.isGranted(project, List.of(BasePermission.READ))) {
            return userWorkspace.getActualName(project);
        } else {
            throw new ProjectException("There is no permission for reading the project.");
        }
    }

    @Override
    public void release() {
        userWorkspace.release();
    }

    @Override
    public void removeWorkspaceListener(UserWorkspaceListener listener) {
        userWorkspace.removeWorkspaceListener(listener);
    }

    @Override
    public RulesProject uploadLocalProject(String repositoryId,
                                           String name,
                                           String projectFolder,
                                           String comment) throws ProjectException {
        if (userWorkspace.hasProject(repositoryId, name)) {
            var path = userWorkspace.getDesignTimeRepository().getRulesLocation() + name;
            if (designRepositoryAclService.isGranted(repositoryId, path, List.of(BasePermission.WRITE))) {
                return userWorkspace.uploadLocalProject(repositoryId, name, projectFolder, comment);
            } else {
                throw new ProjectException("There is no permission for modifying '%s'.".formatted(path));
            }
        } else {
            if (allowProjectCreateDelete && designRepositoryAclService.isGranted(repositoryId, null, List.of(BasePermission.CREATE))) {
                return userWorkspace.uploadLocalProject(repositoryId, name, projectFolder, comment);
            } else {
                throw new ProjectException("There is no permission for creating a new project.");
            }
        }
    }

    @Override
    public Optional<RulesProject> getProjectByPath(String repositoryId, String realPath) {
        var rulesProjectOptional = userWorkspace.getProjectByPath(repositoryId, realPath);
        if (rulesProjectOptional
                .isPresent() && !designRepositoryAclService.isGranted(rulesProjectOptional.get(), List.of(BasePermission.READ))) {
            return Optional.empty();
        }
        return rulesProjectOptional;
    }

    @Override
    public WorkspaceUser getUser() {
        return userWorkspace.getUser();
    }

    @Override
    public RulesProject getProject(String repositoryId, String name) throws ProjectException {
        var rulesProject = userWorkspace.getProject(repositoryId, name);
        if (rulesProject != null && !designRepositoryAclService.isGranted(rulesProject, List.of(BasePermission.READ))) {
            throw new ProjectException("There is no permission for reading the project.");
        }
        return rulesProject;
    }

    @Override
    public RulesProject getProject(String repositoryId, String name, boolean refreshBefore) throws ProjectException {
        var rulesProject = userWorkspace.getProject(repositoryId, name, refreshBefore);
        if (rulesProject != null && !designRepositoryAclService.isGranted(rulesProject, List.of(BasePermission.READ))) {
            throw new ProjectException("There is no permission for reading the project.");
        }
        return rulesProject;
    }

    @Override
    public void setProjectBranch(RulesProject project, String branch) throws ProjectException {
        userWorkspace.setProjectBranch(project, branch);
    }

    @Override
    public Collection<RulesProject> getProjects() {
        return userWorkspace.getProjects()
                .stream()
                .filter(this::hasReadAccess)
                .toList();
    }

    @Override
    public Collection<RulesProject> getProjects(boolean refreshBefore) {
        return userWorkspace.getProjects(refreshBefore)
                .stream()
                .filter(this::hasReadAccess)
                .toList();
    }

    @Override
    public Collection<RulesProject> getProjectsByName(String name) {
        return userWorkspace.getProjectsByName(name)
                .stream()
                .filter(this::hasReadAccess)
                .toList();
    }

    @Override
    public Collection<RulesProject> getProjectsByName(String name, boolean refreshBefore) {
        return userWorkspace.getProjectsByName(name, refreshBefore)
                .stream()
                .filter(this::hasReadAccess)
                .toList();
    }

    @Override
    public LockEngine getProjectsLockEngine() {
        return userWorkspace.getProjectsLockEngine();
    }

    @Override
    public boolean isOpenedOtherProject(AProject project) {
        return userWorkspace.isOpenedOtherProject(project);
    }
}
