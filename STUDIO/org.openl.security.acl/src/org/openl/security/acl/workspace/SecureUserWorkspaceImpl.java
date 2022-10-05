package org.openl.security.acl.workspace;

import static org.openl.security.acl.permission.AclPermission.EDIT;
import static org.openl.security.acl.permission.AclPermission.VIEW;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.LockEngine;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.RepositoryException;
import org.openl.rules.workspace.lw.LocalWorkspace;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.rules.workspace.uw.UserWorkspaceListener;
import org.openl.security.acl.repository.DesignRepositoryAclService;

public class SecureUserWorkspaceImpl implements UserWorkspace {

    private final UserWorkspace userWorkspace;
    private final DesignRepositoryAclService designRepositoryAclService;

    public SecureUserWorkspaceImpl(UserWorkspace userWorkspace, DesignRepositoryAclService designRepositoryAclService) {
        this.userWorkspace = userWorkspace;
        this.designRepositoryAclService = designRepositoryAclService;
    }

    @Override
    public boolean hasProject(String repositoryId, String name) {
        try {
            RulesProject project = userWorkspace.getProject(repositoryId, name);
            return designRepositoryAclService.isGranted(project, List.of(VIEW));
        } catch (ProjectException e) {
            return false;
        }
    }

    @Override
    public List<? extends AProject> getProjects(String repositoryId) {

        return userWorkspace.getProjects(repositoryId)
            .stream()
            .filter(e -> designRepositoryAclService.isGranted(e, List.of(VIEW)))
            .collect(Collectors.toList());
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
    public void copyDDProject(ADeploymentProject project, String name, String comment) throws ProjectException {
        userWorkspace.copyDDProject(project, name, comment);
    }

    @Override
    public ADeploymentProject createDDProject(String name) throws RepositoryException {
        return userWorkspace.createDDProject(name);
    }

    @Override
    public ADeploymentProject getDDProject(String name) throws ProjectException {
        return userWorkspace.getDDProject(name);
    }

    @Override
    public ADeploymentProject getLatestDeploymentConfiguration(String name) {
        return userWorkspace.getLatestDeploymentConfiguration(name);
    }

    @Override
    public List<ADeploymentProject> getDDProjects() throws ProjectException {
        return userWorkspace.getDDProjects();
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
    public boolean hasDDProject(String name) {
        return userWorkspace.hasDDProject(name);
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
        if (designRepositoryAclService.isGranted(project, List.of(VIEW))) {
            return userWorkspace.getActualName(project);
        } else {
            throw new ProjectException("Access denied");
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
        if (designRepositoryAclService.isGranted(repositoryId, projectFolder, List.of(EDIT))) {
            return userWorkspace.uploadLocalProject(repositoryId, name, projectFolder, comment);
        } else {
            throw new ProjectException("Access denied");
        }
    }

    @Override
    public Optional<RulesProject> getProjectByPath(String repositoryId, String realPath) {
        Optional<RulesProject> rulesProjectOptional = userWorkspace.getProjectByPath(repositoryId, realPath);
        if (rulesProjectOptional
            .isPresent() && !designRepositoryAclService.isGranted(rulesProjectOptional.get(), List.of(VIEW))) {
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
        RulesProject rulesProject = userWorkspace.getProject(repositoryId, name);
        if (rulesProject != null && !designRepositoryAclService.isGranted(rulesProject, List.of(VIEW))) {
            throw new ProjectException("Access denied");
        }
        return rulesProject;
    }

    @Override
    public RulesProject getProject(String repositoryId, String name, boolean refreshBefore) throws ProjectException {
        RulesProject rulesProject = userWorkspace.getProject(repositoryId, name, refreshBefore);
        if (rulesProject != null && !designRepositoryAclService.isGranted(rulesProject, List.of(VIEW))) {
            throw new ProjectException("Access denied");
        }
        return rulesProject;
    }

    @Override
    public Collection<RulesProject> getProjects() {
        return userWorkspace.getProjects()
            .stream()
            .filter(e -> designRepositoryAclService.isGranted(e, List.of(VIEW)))
            .collect(Collectors.toList());
    }

    @Override
    public Collection<RulesProject> getProjects(boolean refreshBefore) {

        return userWorkspace.getProjects(refreshBefore)
            .stream()
            .filter(e -> designRepositoryAclService.isGranted(e, List.of(VIEW)))
            .collect(Collectors.toList());
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
